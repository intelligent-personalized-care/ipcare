package pt.ipc.storage.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.reflect.ColumnName
import pt.ipc.domain.User
import pt.ipc.domain.patient.PatientDailyExercises
import pt.ipc.domain.patient.PatientInformation
import pt.ipc.domain.patient.PatientOfPhysiotherapist
import pt.ipc.domain.exercises.DailyExercise
import pt.ipc.domain.physiotherapist.PhysiotherapistAvailable
import pt.ipc.domain.physiotherapist.PhysiotherapistDetails
import pt.ipc.domain.physiotherapist.PhysiotherapistProfile
import pt.ipc.domain.physiotherapist.Rating
import pt.ipc.domain.physiotherapist.RequestInformation
import pt.ipc.domain.plan.PlanOfPatient
import pt.ipc.storage.repositories.PhysiotherapistRepository
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

class JdbiPhysiotherapistsRepository(
    private val handle: Handle
) : PhysiotherapistRepository {

    override fun registerPhysiotherapist(user: User, sessionID: String) {
        handle.createUpdate("insert into dbo.\"user\" values(:id,:u_name,:u_email,:password_hash)")
            .bind("id", user.id)
            .bind("u_name", user.name)
            .bind("u_email", user.email)
            .bind("password_hash", user.passwordHash)
            .execute()

        handle.createUpdate("insert into dbo.physiotherapist values (:id)")
            .bind("id", user.id)
            .execute()

        handle.createUpdate("insert into dbo.session(user_id, session) values(:userID, :sessionID)")
            .bind("userID", user.id)
            .bind("sessionID", sessionID)
            .execute()
    }

    override fun insertCredential(physiotherapistID: UUID, dtSubmit: LocalDate) {
        handle.createUpdate("insert into dbo.docs_authenticity(physiotherapist_id, state, dt_submit) values (:physiotherapistID,'waiting',:dtSubmit)")
            .bind("physiotherapistID", physiotherapistID)
            .bind("dtSubmit", dtSubmit)
            .execute()
    }

    override fun getUserByIDAndSession(id: UUID, sessionID: String): User? =
        handle.createQuery(
            "select u.id,u.name,u.email,u.password_hash from dbo.\"user\" u " +
                "inner join dbo.physiotherapist m on u.id = m.id " +
                "inner join dbo.session s on u.id = s.user_id " +
                " where s.user_id = :id and s.session = :sessionID "
        )
            .bind("id", id)
            .bind("sessionID", sessionID)
            .mapTo<User>()
            .singleOrNull()

    override fun getPhysiotherapistProfile(physiotherapistID: UUID): PhysiotherapistProfile? {
        val details = handle.createQuery(
            "select u.id,u.name,u.email from dbo.physiotherapist m " +
                "inner join dbo.\"user\" u on u.id = m.id " +
                "where u.id = :physiotherapistID "
        )
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PhysiotherapistDetails>()
            .singleOrNull() ?: return null

        val rating = handle.createQuery(
            "SELECT coalesce(avg(stars), 5) AS averageStarts, count(*) AS nrOfReviews FROM dbo.physiotherapist_rating WHERE physiotherapist_id = :physiotherapistID"
        )
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<Rating>()
            .single()
            .isEmpty()

        val docState = handle.createQuery("select state from dbo.docs_authenticity where physiotherapist_id = :physiotherapistID")
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<String>()
            .singleOrNull()

        return PhysiotherapistProfile(
            id = details.id,
            name = details.name,
            email = details.email,
            rating = rating,
            docState = docState
        )
    }

    override fun getPatientOfPhysiotherapist(physiotherapistID: UUID, patientID: UUID): PatientOfPhysiotherapist? {
        val patientInfo = handle.createQuery(
            "select u.name, u.email, c.weight, c.height, c.physical_condition, c.birth_date from dbo.\"user\" u " +
                "inner join dbo.patient_to_physiotherapist ctm on u.id = ctm.patient_id " +
                "inner join dbo.patient c on u.id = c.id " +
                "where ctm.patient_id = :patientID and ctm.physiotherapist_id = :physiotherapistID"
        )
            .bind("patientID", patientID)
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PatientInfo>()
            .singleOrNull() ?: return null

        val plans = handle.createQuery(
            "select p.id,p.title,cp.dt_start as startDate,cp.dt_end as endDate from dbo.patient c " +
                "inner join dbo.patient_plan cp on c.id = cp.patient_id " +
                "inner join dbo.plan p on p.id = cp.plan_id " +
                "where cp.patient_id = :patientID " +
                "order by cp.dt_start"
        )
            .bind("patientID", patientID)
            .mapTo<PlanOfPatient>()
            .toList()

        return PatientOfPhysiotherapist(
            id = patientID,
            name = patientInfo.name,
            email = patientInfo.email,
            weight = patientInfo.weight,
            height = patientInfo.height,
            physicalCondition = patientInfo.physicalCondition,
            birthDate = patientInfo.birthDate,
            plans = plans
        )
    }

    private data class PatientInfo(
        val name: String,
        val email: String,
        val weight: Int? = null,
        val height: Int? = null,
        val physicalCondition: String? = null,
        val birthDate: LocalDate? = null
    )

    override fun getPhysiotherapist(physiotherapistID: UUID): PhysiotherapistDetails? =
        handle.createQuery("select u.id, u.name, u.email from dbo.physiotherapist m inner join dbo.\"user\" u on u.id = m.id where m.id = :physiotherapistID")
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PhysiotherapistDetails>()
            .singleOrNull()

    override fun getPatientsOfPhysiotherapist(physiotherapistID: UUID): List<PatientInformation> =
        handle.createQuery("select u.id,u.name, u.email from dbo.\"user\" u inner join dbo.patient_to_physiotherapist cm on u.id = cm.patient_id where cm.physiotherapist_id = :physiotherapistID ")
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PatientInformation>()
            .toList()

    override fun getPhysiotherapistOfPatient(patientID: UUID): PhysiotherapistDetails? {
        val physiotherapistId = handle.createQuery(
            """
                select physiotherapist_id from dbo.patient_to_physiotherapist where patient_id = :patientID
            """.trimIndent()
        )
            .bind("patientID", patientID)
            .mapTo<UUID>()
            .singleOrNull() ?: return null

        return getPhysiotherapist(physiotherapistId)
    }

    override fun getPhysiotherapistRating(physiotherapistID: UUID): Rating =
        handle.createQuery(
            "SELECT coalesce(avg(stars), 5) AS averageStarts, count(*) AS nrOfReviews FROM dbo.physiotherapist_rating WHERE physiotherapist_id = :physiotherapistID"
        )
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<Rating>()
            .single()
            .isEmpty()

    override fun searchPhysiotherapistsAvailable(name: String?, skip: Int, limit: Int, patientID: UUID): List<PhysiotherapistAvailable> {
        return handle.createQuery(
            """
                select u.id, u.name, u.email,
                    exists(select 1 from dbo.physiotherapist_requests pr
                           where pr.physiotherapist_id = p.id and pr.patient_id = :patientID) as requested
                from dbo.physiotherapist p
                inner join dbo."user" u on u.id = p.id
                where exists(select 1 from dbo.docs_authenticity da
                             where da.physiotherapist_id = p.id and da.state = 'valid')
                  and (:name = '' or strpos(lower(u.name), lower(:name)) > 0)
                order by lower(u.name), u.id
                limit :limit offset :skip
            """.trimIndent()
        )
            .bind("patientID", patientID)
            .bind("name", name?.trim().orEmpty())
            .bind("skip", skip)
            .bind("limit", limit)
            .mapTo<PhysiotherapistAvailable>()
            .toList().map { it.copy(rating = getPhysiotherapistRating(it.id)) }
    }

    override fun acceptRequest(requestID: UUID, patientID: UUID, physiotherapistID: UUID) {
        handle.createUpdate("insert into dbo.patient_to_physiotherapist values (:physiotherapistID,:patientID)")
            .bind("physiotherapistID", physiotherapistID)
            .bind("patientID", patientID)
            .execute()

        handle.createUpdate("delete from dbo.physiotherapist_requests where patient_id = :patientID ")
            .bind("patientID", patientID)
            .execute()
    }

    override fun declineRequest(requestID: UUID) {
        handle.createUpdate("delete from dbo.physiotherapist_requests where request_id = :requestID ")
            .bind("requestID", requestID)
            .execute()
    }

    override fun getRequestInformation(requestID: UUID): RequestInformation? =
        handle.createQuery(
            """
            select request_id, request_text, patient_id as patient_id, u.name, u.email from dbo.physiotherapist_requests
            inner join dbo.patient c on c.id = physiotherapist_requests.patient_id
            inner join dbo."user" u on u.id = c.id
            where request_id = :requestID
            """.trimIndent()
        )
            .bind("requestID", requestID)
            .mapTo<RequestInformation>()
            .singleOrNull()

    override fun physiotherapistRequests(physiotherapistID: UUID): List<RequestInformation> =
        handle.createQuery("select mr.request_id,mr.request_text,mr.patient_id as patient_id, u.name,u.email from dbo.physiotherapist_requests mr inner join dbo.\"user\" u on u.id = mr.patient_id where physiotherapist_id = :physiotherapistID")
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<RequestInformation>()
            .toList()

    override fun checkIfPhysiotherapistIsVerified(physiotherapistID: UUID): Boolean =
        handle.createQuery("select count(*) from dbo.docs_authenticity where physiotherapist_id = :physiotherapistID and state = 'valid' ")
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<Int>()
            .single() == 1

    override fun isPhysiotherapistOfPatient(physiotherapistID: UUID, patientID: UUID): Boolean =
        handle.createQuery("select count(*) from dbo.patient_to_physiotherapist where physiotherapist_id = :physiotherapistID and patient_id = :patientID")
            .bind("physiotherapistID", physiotherapistID)
            .bind("patientID", patientID)
            .mapTo<Int>()
            .single() == 1

    override fun exercisesOfPatients(physiotherapistID: UUID, date: LocalDate): List<PatientDailyExercises> {
        val patients = handle.createQuery(
            "select u.id,u.name from dbo.patient_to_physiotherapist ctm " +
                "inner join dbo.physiotherapist m on m.id = ctm.physiotherapist_id " +
                "inner join dbo.\"user\" u on ctm.patient_id = u.id where m.id = :physiotherapistID"
        )
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PatientData>()
            .toList()
            .ifEmpty { return emptyList() }

        return patients.mapNotNull { patient ->
            getExercisesTotalInfoOfPatient(patient, date)
        }
    }

    private data class PatientData(val id: UUID, val name: String)

    private fun getExercisesTotalInfoOfPatient(patientData: PatientData, date: LocalDate): PatientDailyExercises? {
        val planInfo = handle.createQuery("select cp.plan_id,cp.dt_start from dbo.patient_plan cp where patient_id = :patientID and :date between cp.dt_start and cp.dt_end")
            .bind("patientID", patientData.id)
            .bind("date", date)
            .mapTo<PlanInfo>()
            .singleOrNull() ?: return null

        val dayIndex = Duration.between(planInfo.dtStart.atStartOfDay(), date.atStartOfDay()).toDays().toInt()

        val dailyListID =
            handle.createQuery("select id from dbo.daily_list where day_index = :dayIndex and plan_id = :planID")
                .bind("dayIndex", dayIndex)
                .bind("planID", planInfo.id)
                .mapTo<Int>().single()

        val exercises: List<DailyExercise> =
            handle.createQuery(
                """
                    select de.id, de.exercise_id as ex_id, ei.title, ei.description, ei.exercise_type as type, de.sets, de.reps,
                        case when count(es.daily_exercise_id) != de.sets then 0 else 1 end as is_done
                    from dbo.daily_exercise de
                    inner join dbo.daily_list dl on de.daily_list_id = dl.id
                    inner join dbo.exercise_info ei on ei.id = de.exercise_id
                    left join dbo.exercise_session es on de.id = es.daily_exercise_id and es.patient_id = :patientID
                    where daily_list_id = :dailyListID
                    group by de.id, dl.plan_id, dl.id, ei.title, ei.description, ei.exercise_type, de.sets, de.reps
                """.trimIndent()
            )
                .bind("dailyListID", dailyListID)
                .bind("patientID", patientData.id)
                .mapTo<DailyExercise>()
                .toList()

        return PatientDailyExercises(
            id = patientData.id,
            name = patientData.name,
            planId = planInfo.id,
            dailyListId = dailyListID,
            exercises = exercises
        )
    }

    override fun getAllCredentials(): List<UUID> =
        handle.createQuery("select physiotherapist_id from dbo.docs_authenticity")
            .mapTo<UUID>()
            .toList()

    override fun deleteCredential(physiotherapistID: UUID) {
        handle.createUpdate("delete from dbo.docs_authenticity where physiotherapist_id = :physiotherapistID")
            .bind("physiotherapistID", physiotherapistID)
            .execute()
    }

    private data class PlanInfo(@ColumnName("plan_id") val id: Int, @ColumnName("dt_start") val dtStart: LocalDate)
}
