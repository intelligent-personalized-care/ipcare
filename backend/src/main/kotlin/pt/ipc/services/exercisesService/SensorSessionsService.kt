package pt.ipc.services.exercisesService

import com.fasterxml.jackson.databind.ObjectMapper
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import pt.ipc.domain.exceptions.PatientDontHaveThisExercise
import pt.ipc.domain.exceptions.PatientNotPostedVideo
import pt.ipc.domain.exceptions.ExerciseAlreadyUploaded
import pt.ipc.domain.exceptions.ForbiddenRequest
import pt.ipc.domain.exercises.InvalidSensorSession
import pt.ipc.domain.exercises.SensorProfile
import pt.ipc.domain.exercises.SensorSessionInput
import pt.ipc.storage.repositories.jdbi.JdbiPatientsRepository
import pt.ipc.storage.repositories.jdbi.JdbiPhysiotherapistsRepository
import java.util.UUID

@Service
class SensorSessionsService(private val jdbi: Jdbi, private val json: ObjectMapper) {
    fun progress(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int): Map<String, List<Int>> =
        jdbi.inTransaction<Map<String, List<Int>>, Exception> { handle ->
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            mapOf(
                "completedSets" to handle.createQuery("select nr_set from dbo.exercise_session where patient_id = :patient and daily_exercise_id = :exercise order by nr_set")
                    .bind("patient", patientID).bind("exercise", exerciseID).mapTo<Int>().list()
            )
        }

    private val profileColumns = listOf("raise_threshold", "lower_threshold", "min_raise_time_ms", "hold_time_ms", "cooldown_ms", "min_movement_speed", "min_pitch", "max_pitch", "min_roll", "max_roll")

    fun getProfile(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int): SensorProfile =
        jdbi.inTransaction<SensorProfile, Exception> { handle ->
            readProfile(handle, userID, patientID, planID, dailyListID, exerciseID)
        }

    private fun readProfile(handle: Handle, userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int): SensorProfile {
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            fun key(column: String) = column.split('_').let { parts -> parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) } }
            fun effective(column: String, type: String = "numeric", fallback: String = "") =
                "coalesce((ds.profile->>'${key(column)}')::$type, " +
                "case when ds.profile is null then ps.$column end, " +
                "case when ds.profile is null then (de.sensor_profile->>'${key(column)}')::$type end, ei.$column$fallback) as $column"
            val fields = (profileColumns.map { effective(it) } + effective("use_roll", "boolean", ", true") +
                effective("movement_direction", "integer", ", 1")).joinToString(", ")
            val values = handle.createQuery("select $fields from dbo.daily_exercise de join dbo.exercise_info ei on ei.id = de.exercise_id join dbo.patient_plan pp on pp.plan_id = :plan and pp.patient_id = :patient left join dbo.plan_exercise_setting ps on ps.patient_plan_id = pp.id and ps.exercise_id = ei.id left join dbo.daily_exercise_setting ds on ds.patient_plan_id = pp.id and ds.daily_exercise_id = de.id where de.id = :exercise order by pp.dt_start desc limit 1")
                .bind("plan", planID).bind("patient", patientID).bind("exercise", exerciseID).mapToMap().one()
            if (profileColumns.any { values[it] == null }) throw InvalidSensorSession("The physiotherapist must configure the sensor profile for this exercise")
            fun number(key: String) = values[key] as Number
            return SensorProfile(
                number("raise_threshold").toFloat(), number("lower_threshold").toFloat(),
                number("min_raise_time_ms").toInt(), number("hold_time_ms").toInt(), number("cooldown_ms").toInt(),
                number("min_movement_speed").toFloat(), number("min_pitch").toFloat(), number("max_pitch").toFloat(),
                number("min_roll").toFloat(), number("max_roll").toFloat(), values["use_roll"] as Boolean, number("movement_direction").toInt()
            ).also { it.validate() }
        }

    fun saveProfile(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int, profile: SensorProfile): SensorProfile {
        profile.validate()
        return jdbi.inTransaction<SensorProfile, Exception> { handle ->
            if (!JdbiPhysiotherapistsRepository(handle).isPhysiotherapistOfPatient(userID, patientID)) throw ForbiddenRequest
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            val assignment = handle.createQuery("select id from dbo.patient_plan where patient_id = :patient and plan_id = :plan order by dt_start desc limit 1")
                .bind("patient", patientID).bind("plan", planID).mapTo<Int>().one()
            handle.createUpdate("insert into dbo.daily_exercise_setting(patient_plan_id, daily_exercise_id, profile) values (:assignment, :exercise, cast(:profile as jsonb)) on conflict (patient_plan_id, daily_exercise_id) do update set profile = excluded.profile")
                .bind("assignment", assignment).bind("exercise", exerciseID)
                .bind("profile", json.writeValueAsString(profile)).execute()
            profile
        }
    }

    fun resetProfile(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int): SensorProfile {
        return jdbi.inTransaction<SensorProfile, Exception> { handle ->
            if (!JdbiPhysiotherapistsRepository(handle).isPhysiotherapistOfPatient(userID, patientID)) throw ForbiddenRequest
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            handle.createUpdate("insert into dbo.daily_exercise_setting(patient_plan_id, daily_exercise_id, profile) select id, :exercise, '{}'::jsonb from dbo.patient_plan where patient_id = :patient and plan_id = :plan order by dt_start desc limit 1 on conflict (patient_plan_id, daily_exercise_id) do update set profile = excluded.profile")
                .bind("exercise", exerciseID).bind("patient", patientID).bind("plan", planID).execute()
            readProfile(handle, userID, patientID, planID, dailyListID, exerciseID)
        }
    }

    private fun authorize(handle: Handle, userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int) {
        if (userID != patientID && !JdbiPhysiotherapistsRepository(handle).isPhysiotherapistOfPatient(userID, patientID)) throw ForbiddenRequest
        if (!JdbiPatientsRepository(handle).checkIfPatientHasThisExercise(patientID, planID, dailyListID, exerciseID)) {
            throw PatientDontHaveThisExercise
        }
    }

    fun save(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int, input: SensorSessionInput): SensorSessionInput {
        if (userID != patientID) throw ForbiddenRequest
        input.validate()
        return jdbi.inTransaction<SensorSessionInput, Exception> { handle ->
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            val prescribed = handle.createQuery("select sets, reps from dbo.daily_exercise where id = :id for update")
                .bind("id", exerciseID).map { rs, _ -> rs.getInt("sets") to rs.getInt("reps") }.one()
            if (input.set > prescribed.first || input.repetitions != prescribed.second) {
                throw InvalidSensorSession("The completed set must match the prescribed repetitions")
            }
            val existing = handle.createQuery("select id from dbo.exercise_session where patient_id = :patient and daily_exercise_id = :exercise and nr_set = :set")
                .bind("patient", patientID).bind("exercise", exerciseID).bind("set", input.set).mapTo<UUID>().singleOrNull()
            if (existing != null) {
                if (existing != input.sessionId) throw ExerciseAlreadyUploaded
                val saved = read(handle, patientID, exerciseID, input.set)
                if (saved != input) throw ExerciseAlreadyUploaded
                return@inTransaction saved
            }
            handle.createUpdate("insert into dbo.exercise_session(id, patient_id, daily_exercise_id, dt_submit, execution_mode, nr_set, with_load, load_value, load_unit) values (:id, :patient, :exercise, CURRENT_TIMESTAMP, 'sensor', :set, :withLoad, :loadValue, :loadUnit)")
                .bind("id", input.sessionId).bind("patient", patientID).bind("exercise", exerciseID).bind("set", input.set).bind("withLoad", input.withLoad).bind("loadValue", input.loadValue).bind("loadUnit", input.loadUnit).execute()
            handle.createUpdate("insert into dbo.sensor_result(session_id, repetitions, duration_ms, samples, profile) values (:id, :reps, :duration, cast(:samples as jsonb), cast(:profile as jsonb))")
                .bind("id", input.sessionId).bind("reps", input.repetitions).bind("duration", input.durationMs)
                .bind("samples", json.writeValueAsString(input.samples))
                .bind("profile", input.profile?.let { json.writeValueAsString(it) }).execute()
            input
        }
    }

    fun get(userID: UUID, patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int, set: Int): SensorSessionInput =
        jdbi.inTransaction<SensorSessionInput, Exception> { handle ->
            authorize(handle, userID, patientID, planID, dailyListID, exerciseID)
            read(handle, patientID, exerciseID, set)
        }

    private fun read(handle: Handle, patientID: UUID, exerciseID: Int, set: Int): SensorSessionInput =
        handle.createQuery("select es.id, es.nr_set, es.with_load, es.load_value, es.load_unit, sr.repetitions, sr.duration_ms, sr.samples::text, sr.profile::text from dbo.exercise_session es join dbo.sensor_result sr on sr.session_id = es.id where es.patient_id = :patient and es.daily_exercise_id = :exercise and es.nr_set = :set")
            .bind("patient", patientID).bind("exercise", exerciseID).bind("set", set)
            .map { rs, _ ->
                SensorSessionInput(
                    UUID.fromString(rs.getString("id")),
                    rs.getInt("nr_set"),
                    rs.getInt("repetitions"),
                    rs.getLong("duration_ms"),
                    json.readValue(rs.getString("samples"), Array<pt.ipc.domain.exercises.SensorSample>::class.java).toList(),
                    rs.getString("profile")?.let { json.readValue(it, SensorProfile::class.java) },
                    rs.getObject("with_load") as Boolean?,
                    (rs.getObject("load_value") as? Number)?.toFloat(), rs.getString("load_unit")
                )
            }.singleOrNull() ?: throw PatientNotPostedVideo
}
