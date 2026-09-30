package pt.ipc.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pt.ipc.domain.exceptions.*
import pt.ipc.domain.exercises.*
import pt.ipc.services.exercisesService.SensorSessionsService
import pt.ipc.storage.repositories.jdbi.*
import java.io.File
import java.util.UUID

class SensorIntegrationTests {
    private lateinit var jdbi: Jdbi
    private lateinit var service: SensorSessionsService
    private val patient = UUID.randomUUID()
    private val otherPatient = UUID.randomUUID()
    private val physiotherapist = UUID.randomUUID()
    private val exerciseInfo = UUID.randomUUID()
    private var plan = 0
    private var daily = 0
    private var exercise = 0

    @BeforeEach fun prepare() {
        val url = System.getenv("IPC_TEST_DB_URL")
        Assumptions.assumeTrue(url != null, "Set IPC_TEST_DB_URL to the isolated ipc_integration_test database")
        require(url!!.contains("/ipc_integration_test"))
        jdbi = Jdbi.create(url).configure()
        jdbi.useHandle<Exception> { h ->
            require(h.createQuery("select current_database()").mapTo(String::class.java).one() == "ipc_integration_test")
            h.execute("drop schema if exists dbo cascade")
            h.execute(File("postgresql/createTable.sql").readText())
            h.execute("truncate dbo.\"user\" cascade")
            listOf(patient, otherPatient, physiotherapist).forEach {
                h.createUpdate("insert into dbo.\"user\"(id,name,email,password_hash) values (:id,'Test user',:email,'test')")
                    .bind("id", it).bind("email", "$it@example.test").execute()
            }
            h.execute("insert into dbo.patient(id) values ('$patient'), ('$otherPatient')")
            h.execute("insert into dbo.physiotherapist(id) values ('$physiotherapist')")
            h.execute("insert into dbo.patient_to_physiotherapist values ('$physiotherapist','$patient'), ('$physiotherapist','$otherPatient')")
            plan = h.createQuery("insert into dbo.plan(physiotherapist_id,title) values ('$physiotherapist','Sensor test') returning id").mapTo(Int::class.java).one()
            h.execute("insert into dbo.patient_plan(plan_id,patient_id,dt_start,dt_end) values ($plan,'$patient',CURRENT_DATE,CURRENT_DATE),($plan,'$otherPatient',CURRENT_DATE,CURRENT_DATE)")
            daily = h.createQuery("insert into dbo.daily_list(day_index,plan_id) values (0,$plan) returning id").mapTo(Int::class.java).one()
            h.execute("insert into dbo.exercise_info(id,title,description,exercise_type) values ('$exerciseInfo','Elbow curl','Test exercise description','Biceps')")
            exercise = h.createQuery("insert into dbo.daily_exercise(exercise_id,daily_list_id,sets,reps) values ('$exerciseInfo',$daily,1,10) returning id").mapTo(Int::class.java).one()
        }
        service = SensorSessionsService(jdbi, jacksonObjectMapper())
    }

    private fun result() = SensorSessionInput(UUID.randomUUID(), 1, 10, 1000, listOf(SensorSample(500, 2f, 60f, 8f)))

    @Test fun `physiotherapist star rating persists with feedback without affecting another patient`() {
        val input = result()
        service.save(patient, patient, plan, daily, exercise, input)
        val other = result()
        service.save(otherPatient, otherPatient, plan, daily, exercise, other)
        val manager = pt.ipc.storage.transaction.TransactionManagerImpl(jdbi)
        val feedback = pt.ipc.services.physiotherapistService.PhysiotherapistsServiceImpl(
            pt.ipc.domain.encryption.EncryptionUtils(pt.ipc.domain.encryption.EncryptionUtilsConfiguration("0123456789abcdef")),
            manager, pt.ipc.services.ServiceUtils(manager, pt.ipc.domain.jwt.JwtUtils(pt.ipc.domain.jwt.JwtConfiguration("a".repeat(64)))))
        feedback.giveFeedbackOfExercise(physiotherapist, plan, daily, exercise, 1, "Good control", "4", patient)
        jdbi.useHandle<Exception> { h ->
            assertEquals("4", JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).physiotherapistFeedbackScore)
            assertEquals("Good control", JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).physiotherapistFeedBack)
            assertNull(JdbiExercisesRepository(h).getVideoFeedback(other.sessionId).physiotherapistFeedbackScore)
        }
        assertThrows(InvalidFeedbackScore::class.java) {
            feedback.giveFeedbackOfExercise(physiotherapist, plan, daily, exercise, 1, "Invalid", "6", patient)
        }
        jdbi.useHandle<Exception> { h ->
            assertEquals("4", JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).physiotherapistFeedbackScore)
        }
    }

    @Test fun `HTTP submission persists load and exposes result and progress to physiotherapist`() {
        val mapper = jacksonObjectMapper()
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.exercises.SensorSessionsController(service))
            .setControllerAdvice(pt.ipc.http.pipeline.exceptionHandler.ExceptionHandler())
            .setCustomArgumentResolvers(pt.ipc.http.pipeline.authentication.UserArgumentResolver()).build()
        val path = "/users/patients/$patient/plans/$plan/daily_lists/$daily/exercises/$exercise/sensor"
        val input = result().copy(withLoad = true, loadValue = 2.5f, loadUnit = "kg")
        fun asUser(id: UUID) = org.springframework.test.web.servlet.request.RequestPostProcessor { request ->
            pt.ipc.http.pipeline.authentication.UserArgumentResolver.addUserTo(
                pt.ipc.domain.User(id, "Test", "test@example.test", "unused"), request)
            request
        }
        repeat(2) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .with(asUser(patient)).contentType("application/json").content(mapper.writeValueAsString(input)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
            .with(asUser(physiotherapist)).param("set", "1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sessionId").value(input.sessionId.toString()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.loadValue").value(2.5))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.loadUnit").value("kg"))
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("$path/progress")
            .with(asUser(physiotherapist)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.completedSets[0]").value(1))
        jdbi.useHandle<Exception> { h ->
            assertEquals(1, h.createQuery("select count(*) from dbo.sensor_result").mapTo(Int::class.java).one())
        }
    }

    @Test fun `invalid HTTP session returns problem response and leaves no partial database writes`() {
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.exercises.SensorSessionsController(service))
            .setControllerAdvice(pt.ipc.http.pipeline.exceptionHandler.ExceptionHandler())
            .setCustomArgumentResolvers(pt.ipc.http.pipeline.authentication.UserArgumentResolver()).build()
        val invalid = result().copy(withLoad = true, loadValue = -2f, loadUnit = "kg")
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/users/patients/$patient/plans/$plan/daily_lists/$daily/exercises/$exercise/sensor")
            .with { request ->
                pt.ipc.http.pipeline.authentication.UserArgumentResolver.addUserTo(
                    pt.ipc.domain.User(patient, "Test", "test@example.test", "unused"), request)
                request
            }.contentType("application/json").content(jacksonObjectMapper().writeValueAsString(invalid)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
        jdbi.useHandle<Exception> { h ->
            assertEquals(0, h.createQuery("select count(*) from dbo.exercise_session").mapTo(Int::class.java).one())
            assertEquals(0, h.createQuery("select count(*) from dbo.sensor_result").mapTo(Int::class.java).one())
        }
    }

    @Test fun `camera and sensor routes expose the same effective patient prescription`() {
        val profile = SensorProfile(45f, 8f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 100f)
        service.saveProfile(physiotherapist, patient, plan, daily, exercise, profile)
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.exercises.SensorSessionsController(service))
            .setCustomArgumentResolvers(pt.ipc.http.pipeline.authentication.UserArgumentResolver()).build()
        val base = "/users/patients/$patient/plans/$plan/daily_lists/$daily/exercises/$exercise"
        listOf("$base/profile", "$base/sensor/profile").forEach { route ->
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(route).with { request ->
                pt.ipc.http.pipeline.authentication.UserArgumentResolver.addUserTo(pt.ipc.domain.User(patient, "Patient", "patient@example.test", "test"), request)
                request
            }).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.raiseThreshold").value(45.0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.holdTimeMs").value(5000))
        }
    }

    @Test fun `save is idempotent and physiotherapist can read the same result`() {
        val input = result()
        assertEquals(input, service.save(patient, patient, plan, daily, exercise, input))
        assertEquals(input, service.save(patient, patient, plan, daily, exercise, input))
        assertEquals(input, service.get(physiotherapist, patient, plan, daily, exercise, 1))
        assertThrows(ExerciseAlreadyUploaded::class.java) { service.save(patient, patient, plan, daily, exercise, result()) }
    }

    @Test fun `shared plan keeps results completion and feedback separate`() {
        val input = result()
        service.save(patient, patient, plan, daily, exercise, input)
        assertTrue(service.progress(otherPatient, otherPatient, plan, daily, exercise)["completedSets"]!!.isEmpty())
        assertThrows(Forbidden::class.java) { service.get(otherPatient, patient, plan, daily, exercise, 1) }
        assertThrows(Forbidden::class.java) { service.save(physiotherapist, patient, plan, daily, exercise, result()) }
        service.save(otherPatient, otherPatient, plan, daily, exercise, result())
        jdbi.useHandle<Exception> { h ->
            val plans = JdbiPlansRepository(h)
            assertTrue(plans.checkIfPatientAlreadyUploadedVideo(patient, plan, daily, exercise, 1))
            plans.giveFeedBackOfVideo(patient, exercise, 1, "Good movement", null)
            val repository = JdbiExercisesRepository(h)
            assertTrue(JdbiPatientsRepository(h).getPatientsVideosIDs().isEmpty(), "Sensor sessions must never be treated as missing cloud videos")
            assertEquals("Good movement", repository.getVideoFeedback(input.sessionId).physiotherapistFeedBack)
            val otherId = repository.getPatientVideoID(otherPatient, plan, daily, exercise, 1)!!
            assertNull(repository.getVideoFeedback(otherId).physiotherapistFeedBack)
        }
    }

    @Test fun `only assigned physiotherapist can configure a reachable profile`() {
        val profile = SensorProfile(60f, 25f, 300, 500, 800, 6f, -35f, 35f, -10f, 145f)
        assertThrows(Forbidden::class.java) { service.saveProfile(patient, patient, plan, daily, exercise, profile) }
        assertEquals(profile, service.saveProfile(physiotherapist, patient, plan, daily, exercise, profile))
        assertEquals(profile, service.getProfile(patient, patient, plan, daily, exercise))
        val session = result().copy(profile = profile)
        service.save(patient, patient, plan, daily, exercise, session)
        service.saveProfile(physiotherapist, patient, plan, daily, exercise, profile.copy(movementDirection = 1))
        assertEquals(session, service.get(physiotherapist, patient, plan, daily, exercise, 1), "Historical profile must not change with the prescription")
        assertThrows(InvalidSensorSession::class.java) { service.getProfile(otherPatient, otherPatient, plan, daily, exercise) }
        assertThrows(InvalidSensorSession::class.java) { service.save(patient, patient, plan, daily, exercise, result().copy(repetitions = 9)) }
    }

    @Test fun `reassigning a template creates independent exercise histories`() {
        val assigned = jdbi.inTransaction<List<Pair<Int, Int>>, Exception> { h ->
            h.execute("delete from dbo.patient_plan")
            val repository = JdbiPlansRepository(h)
            val today = java.time.LocalDate.now()
            repository.associatePlanToPatient(plan, patient, today, today)
            repository.associatePlanToPatient(plan, patient, today.plusDays(1), today.plusDays(1))
            assertEquals(1, repository.getPlans(physiotherapist).size, "Snapshots must not duplicate the physiotherapist template list")
            h.createQuery("select pp.plan_id, de.id from dbo.patient_plan pp join dbo.daily_list dl on dl.plan_id = pp.plan_id join dbo.daily_exercise de on de.daily_list_id = dl.id order by pp.dt_start")
                .map { rs, _ -> rs.getInt(1) to rs.getInt(2) }.list()
        }
        assertNotEquals(assigned[0], assigned[1])
        assigned.forEach { (assignedPlan, assignedExercise) ->
            val assignedDaily = jdbi.withHandle<Int, Exception> { h -> h.createQuery("select daily_list_id from dbo.daily_exercise where id = :id").bind("id", assignedExercise).mapTo(Int::class.java).one() }
            assertTrue(service.progress(patient, patient, assignedPlan, assignedDaily, assignedExercise)["completedSets"]!!.isEmpty())
            service.save(patient, patient, assignedPlan, assignedDaily, assignedExercise, result())
        }
    }

    @Test fun `joint catalogue is complete repeatable and preserves existing exercises`() {
        jdbi.useHandle<Exception> { h ->
            val seed = File("postgresql/insertTable.sql").readText()
            h.execute(seed)
            h.execute(seed)
            val repository = JdbiExercisesRepository(h)
            val catalogue = repository.getExercises(0, 100).filter { it.id.toString().startsWith("73a20000-cc31-4c29-9000-") }
            assertEquals(8, catalogue.size)
            val extension = catalogue.single { it.title == "Elbow Extension" }
            val flexion = catalogue.single { it.title == "Elbow Flexion" }
            assertEquals(false, extension.useRoll)
            assertEquals(1, extension.movementDirection)
            assertEquals(false, flexion.useRoll)
            assertEquals(-1, flexion.movementDirection)
            assertEquals(60f, extension.raiseThreshold)
            assertEquals(100f, extension.maxPitch)
            assertEquals(25f, extension.maxRoll)
            val wristFlexion = catalogue.single { it.title == "Wrist Flexion" }
            val wristExtension = catalogue.single { it.title == "Wrist Extension" }
            val kneeExtension = catalogue.single { it.title == "Knee Extension - Seated" }
            assertEquals(false, wristFlexion.useRoll); assertEquals(1, wristFlexion.movementDirection)
            assertEquals(false, wristExtension.useRoll); assertEquals(-1, wristExtension.movementDirection)
            assertEquals(false, kneeExtension.useRoll); assertEquals(-1, kneeExtension.movementDirection)
            assertEquals(60f, wristFlexion.maxPitch); assertEquals(30f, wristExtension.maxRoll)
            assertEquals(-30f, wristExtension.minRoll); assertEquals(100f, kneeExtension.maxPitch)
            assertEquals(55f, kneeExtension.maxRoll)

            assertNotNull(repository.getExercise(exerciseInfo), "Seeding must preserve historical exercise definitions")
            assertEquals(setOf(ExerciseType.Forearms, ExerciseType.Biceps, ExerciseType.Triceps, ExerciseType.Legs), catalogue.map { it.type }.toSet())
            catalogue.forEach { entry ->
                assertEquals(true, entry.supportsCamera)
                assertTrue(entry.cameraJoint in setOf("WRIST", "ELBOW", "KNEE"))
                assertTrue(entry.cameraMovement in setOf("FLEXION", "EXTENSION"))
                assertEquals(entry.title != "Supported Mini Squat", entry.supportsSensors)
                assertNotNull(entry.useRoll)
                SensorProfile(entry.raiseThreshold!!, entry.lowerThreshold!!, entry.minRaiseTimeMs!!,
                    entry.holdTimeMs!!, entry.cooldownMs!!, entry.minMovementSpeed!!,
                    entry.minPitch!!, entry.maxPitch!!, entry.minRoll!!, entry.maxRoll!!, entry.useRoll!!, entry.movementDirection!!).validate()
            }
        }
    }

    @Test fun `complete schema is repeatable and per assignment profile persists`() {
        jdbi.useHandle<Exception> { h ->
            val schema = File("postgresql/createTable.sql").readText()
            h.execute(schema); h.execute(schema)
        }
        val profile = SensorProfile(30f, 5f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 60f, true, -1)
        service.saveProfile(physiotherapist, patient, plan, daily, exercise, profile)
        assertEquals(profile, service.getProfile(patient, patient, plan, daily, exercise))
        assertThrows(InvalidSensorSession::class.java) { profile.copy(movementDirection = 0).validate() }
        assertThrows(InvalidSensorSession::class.java) { profile.copy(movementDirection = 2).validate() }
    }

    @Test fun `plan angles inherit catalogue or remain scoped to one day and assignment`() {
        val wrist = UUID.fromString("73a20000-cc31-4c29-9000-000000000001")
        val custom = SensorProfile(45f, 5f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 60f)
        val ids = jdbi.inTransaction<List<Triple<Int, Int, Int>>, Exception> { h ->
            val schema = File("postgresql/createTable.sql").readText()
            h.execute(schema); h.execute(schema)
            h.execute(File("postgresql/insertTable.sql").readText())
            val repository = JdbiPlansRepository(h)
            val template = repository.createPlan(physiotherapist, pt.ipc.domain.plan.PlanInput("Per day profile", listOf(
                pt.ipc.domain.plan.DailyListInput(listOf(Exercise(wrist, 1, 10))),
                pt.ipc.domain.plan.DailyListInput(listOf(Exercise(wrist, 1, 10, custom))),
                pt.ipc.domain.plan.DailyListInput(listOf(Exercise(wrist, 1, 10, custom.copy(raiseThreshold = 50f))))
            )))
            repository.associatePlanToPatient(template, patient, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(2))
            h.createQuery("select pp.plan_id, dl.id, de.id from dbo.patient_plan pp join dbo.plan p on p.id = pp.plan_id join dbo.daily_list dl on dl.plan_id = p.id join dbo.daily_exercise de on de.daily_list_id = dl.id where p.template_plan_id = :template order by dl.day_index")
                .bind("template", template).map { rs, _ -> Triple(rs.getInt(1), rs.getInt(2), rs.getInt(3)) }.list()
        }
        fun read(day: Int) = ids[day].let { (p, d, e) -> service.getProfile(patient, patient, p, d, e) }
        assertEquals(30f, read(0).raiseThreshold)
        assertEquals(45f, read(1).raiseThreshold)
        assertEquals(50f, read(2).raiseThreshold)
        jdbi.useHandle<Exception> { it.execute("update dbo.exercise_info set raise_threshold = 32 where id = '$wrist'") }
        assertEquals(32f, read(0).raiseThreshold, "Uncustomized entries must follow catalogue values")
        ids[1].let { (p, d, e) -> service.saveProfile(physiotherapist, patient, p, d, e, custom.copy(raiseThreshold = 40f)) }
        assertEquals(40f, read(1).raiseThreshold)
        assertEquals(50f, read(2).raiseThreshold, "The same exercise on another day must not change")
        ids[1].let { (p, d, e) ->
            assertThrows(Forbidden::class.java) { service.resetProfile(patient, patient, p, d, e) }
            service.resetProfile(physiotherapist, patient, p, d, e)
        }
        assertEquals(32f, read(1).raiseThreshold)
        assertEquals(50f, read(2).raiseThreshold)
    }

    @Test fun `reset without catalogue defaults rolls back rather than losing the profile`() {
        val custom = SensorProfile(45f, 5f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 60f)
        service.saveProfile(physiotherapist, patient, plan, daily, exercise, custom)
        assertThrows(InvalidSensorSession::class.java) { service.resetProfile(physiotherapist, patient, plan, daily, exercise) }
        assertEquals(custom, service.getProfile(patient, patient, plan, daily, exercise))
    }


    @Test fun `complete schema retains results and enforces session ownership and set limits`() {
        val input = result()
        service.save(patient, patient, plan, daily, exercise, input)
        jdbi.useHandle<Exception> { h ->
            h.execute(File("postgresql/createTable.sql").readText())
            assertEquals(patient, h.createQuery("select patient_id from dbo.exercise_session").mapTo(UUID::class.java).one())
            assertEquals(1, h.createQuery("select count(*) from dbo.sensor_result").mapTo(Int::class.java).one())
            assertThrows(Exception::class.java) {
                h.execute("insert into dbo.exercise_session(id,patient_id,daily_exercise_id,dt_submit,nr_set) values ('${UUID.randomUUID()}','$patient',$exercise,CURRENT_TIMESTAMP,2)")
            }
            assertThrows(Exception::class.java) {
                h.execute("insert into dbo.exercise_session(id,daily_exercise_id,dt_submit,nr_set) values ('${UUID.randomUUID()}',$exercise,CURRENT_TIMESTAMP,1)")
            }
        }
        assertEquals(input, service.get(patient, patient, plan, daily, exercise, 1))
    }

    @Test fun `load answers survive sensor persistence retries and physiotherapist feedback reads`() {
        listOf(true, false).forEachIndexed { index, load ->
            val owner = if (index == 0) patient else otherPatient
            val input = result().copy(withLoad = load, loadValue = if (load) 2.5f else null, loadUnit = if (load) "kg" else null)
            service.save(owner, owner, plan, daily, exercise, input)
            assertEquals(input, service.save(owner, owner, plan, daily, exercise, input))
            assertEquals(load, service.get(physiotherapist, owner, plan, daily, exercise, 1).withLoad)
            jdbi.useHandle<Exception> { h ->
                assertEquals(load, JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).withLoad)
                assertEquals(input.loadValue, JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).loadValue)
                assertEquals(input.loadUnit, JdbiExercisesRepository(h).getVideoFeedback(input.sessionId).loadUnit)
            }
            assertThrows(ExerciseAlreadyUploaded::class.java) {
                service.save(owner, owner, plan, daily, exercise, input.copy(withLoad = !load, loadValue = if (!load) 3f else null, loadUnit = if (!load) "kg" else null))
            }
        }
    }

    @Test fun `camera load and historical unknown load remain distinguishable`() {
        jdbi.useHandle<Exception> { h ->
            val id = UUID.randomUUID()
            JdbiPatientsRepository(h).uploadExerciseVideoOfPatient(patient, exercise, id, java.time.LocalDate.now(), 1, null, "camera", true, 1.5f, "kg", null)
            assertEquals(true, JdbiExercisesRepository(h).getVideoFeedback(id).withLoad)
            assertEquals(1.5f, JdbiExercisesRepository(h).getVideoFeedback(id).loadValue)
        }
        val legacy = result()
        service.save(otherPatient, otherPatient, plan, daily, exercise, legacy)
        assertNull(service.get(physiotherapist, otherPatient, plan, daily, exercise, 1).withLoad)
    }

    @Test fun `joint filtering precedes pagination and supports empty pages and type intersection`() {
        jdbi.useHandle<Exception> { h ->
            h.execute(File("postgresql/insertTable.sql").readText())
            val repository = JdbiExercisesRepository(h)
            val all = repository.getExercises(0, 100)
            listOf("WRIST", "ELBOW", "KNEE").forEach { joint ->
                val expected = all.filter { it.cameraJoint == joint }
                assertFalse(expected.isEmpty())
                assertEquals(expected, repository.getExercises(0, 100, joint))
                assertEquals(expected.drop(1).take(1), repository.getExercises(1, 1, joint))
                assertTrue(repository.getExercises(100, 10, joint).isEmpty())
            }
            assertTrue(repository.getExerciseByType(ExerciseType.Legs, 0, 10, "WRIST").isEmpty())
            assertEquals(all.filter { it.cameraJoint == "KNEE" && it.type == ExerciseType.Legs }, repository.getExerciseByType(ExerciseType.Legs, 0, 100, "KNEE"))
        }
    }

    @Test fun `physiotherapist library lists templates without patient associations`() {
        jdbi.useHandle<Exception> { h ->
            h.execute("delete from dbo.patient_plan")
            val repository = JdbiPlansRepository(h)
            assertEquals(listOf(plan), repository.getPlans(physiotherapist).map { it.id })
            val detail = repository.getPlanOfPhysiotherapist(plan)!!
            assertEquals("Sensor test", detail.title)
            assertEquals(10, detail.dailyLists.first()!!.exercises.first().reps)
            assertTrue(repository.getPlans(UUID.randomUUID()).isEmpty())
        }
    }

    @Test fun `catalogue HTTP route filters joints and rejects invalid filters`() {
        jdbi.useHandle<Exception> { it.execute(File("postgresql/insertTable.sql").readText()) }
        val service = pt.ipc.services.exercisesService.ExercisesServiceImpl(pt.ipc.storage.transaction.TransactionManagerImpl(jdbi))
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.exercises.ExercisesController(service))
            .setControllerAdvice(pt.ipc.http.pipeline.exceptionHandler.ExceptionHandler()).build()
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/exercises").param("joint", "WRIST").param("limit", "1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.exercises.length()").value(1))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.exercises[0].cameraJoint").value("WRIST"))
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/exercises").param("joint", "INVALID"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest)
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/exercises").param("skip", "-1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest)
    }
}
