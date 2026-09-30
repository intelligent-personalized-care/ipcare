package pt.ipc.storage.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.ipc.domain.patient.Patient
import pt.ipc.domain.patient.PatientOutput
import pt.ipc.storage.repositories.PatientsRepository
import java.time.LocalDate
import java.util.UUID

class JdbiPatientsRepository(
    private val handle: Handle
) : PatientsRepository {

    override fun existsEmail(email: String): Boolean {
        return handle
            .createQuery("select count(*) from dbo.\"user\" where email = :email")
            .bind("email", email)
            .mapTo<Int>()
            .single() == 1
    }

    override fun getPatient(patientID: UUID): PatientOutput? =
        handle.createQuery(
            "select u.id, u.name, u.email, c.weight, c.height, c.physical_condition as physicalCondition, c.birth_date as birthDate " +
                "from dbo.patient c inner join dbo.\"user\" u on c.id = u.id where c.id = :patientID"
        )
            .bind("patientID", patientID)
            .mapTo<PatientOutput>()
            .singleOrNull()

    override fun requestPhysiotherapist(requestID: UUID, physiotherapistID: UUID, patientID: UUID, requestText: String?) {
        handle.createUpdate("insert into dbo.physiotherapist_requests (physiotherapist_id, patient_id, request_id, request_text) values (:physiotherapistID,:patientID,:requestID,:requestText)")
            .bind("physiotherapistID", physiotherapistID)
            .bind("patientID", patientID)
            .bind("requestID", requestID)
            .bind("requestText", requestText)
            .execute()
    }

    override fun deleteConnection(physiotherapistID: UUID, patientID: UUID) {
        handle.createUpdate("delete from dbo.patient_to_physiotherapist where patient_id = :patientID and physiotherapist_id = :physiotherapistID")
            .bind("patientID", patientID)
            .bind("physiotherapistID", physiotherapistID)
            .execute()
    }

    override fun registerPatient(input: Patient, sessionID: String) {
        handle.createUpdate("insert into dbo.\"user\" (id, name, email, password_hash) values (:id,:u_name,:u_email,:password_hash)")
            .bind("id", input.id)
            .bind("u_name", input.name)
            .bind("u_email", input.email)
            .bind("password_hash", input.password)
            .execute()

        handle.createUpdate(
            "insert into dbo.patient (id, physical_condition, weight, height, birth_date) values (:id,:physical_condition,:weight,:height,:birth_date)"
        )
            .bind("id", input.id)
            .bind("physical_condition", input.physicalCondition)
            .bind("weight", input.weight)
            .bind("height", input.height)
            .bind("birth_date", input.birthDate)
            .execute()

        handle.createUpdate("insert into dbo.session(user_id, session) values(:userID, :sessionID)")
            .bind("userID", input.id)
            .bind("sessionID", sessionID)
            .execute()
    }

    override fun hasPatientRatedPhysiotherapist(patientID: UUID, physiotherapistID: UUID): Boolean =
        handle.createQuery("select count(*) from dbo.physiotherapist_rating where patient_id = :patientID and physiotherapist_id = :physiotherapistID ")
            .bind("patientID", patientID)
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<Int>()
            .single() == 1

    override fun ratePhysiotherapist(patientID: UUID, physiotherapistID: UUID, rating: Int) {
        handle.createUpdate("insert into dbo.physiotherapist_rating (physiotherapist_id, patient_id, stars) values (:physiotherapistID,:patientID, :rating)")
            .bind("physiotherapistID", physiotherapistID)
            .bind("patientID", patientID)
            .bind("rating", rating)
            .execute()
    }

    override fun checkIfPatientHasThisExercise(patientID: UUID, planID: Int, dailyList: Int, exerciseID: Int): Boolean {
        return handle.createQuery(
            """
            select exists(
                select * from dbo.plan p
                inner join dbo.daily_list dl on p.id = dl.plan_id
                inner join dbo.daily_exercise de on dl.id = de.daily_list_id
                inner join dbo.patient_plan pp on pp.plan_id = p.id
                where p.id = :planID and dl.id = :dailyListID and de.id = :exerciseID and pp.patient_id = :patientID
            )
            """.trimIndent()
        )
            .bind("planID", planID)
            .bind("dailyListID", dailyList)
            .bind("exerciseID", exerciseID)
            .bind("patientID", patientID)
            .mapTo<Boolean>()
            .single()
    }

    override fun checkIfPatientAlreadyUploadedVideo(patientID: UUID, exerciseID: Int, set: Int): Boolean {
        return handle.createQuery(
            """
            select count(*) from dbo.exercise_session es
            inner join dbo.daily_exercise de on de.id = es.daily_exercise_id
            inner join dbo.daily_list dl on dl.id = de.daily_list_id
            inner join dbo.plan p on p.id = dl.plan_id
            inner join dbo.patient_plan pp on pp.plan_id = p.id
            where pp.patient_id = :patient and es.patient_id = :patient and es.daily_exercise_id = :exerciseID and es.nr_set = :set
            """.trimIndent()
        )
            .bind("patient", patientID)
            .bind("exerciseID", exerciseID)
            .bind("set", set)
            .mapTo<Int>()
            .single() == 1
    }

    override fun uploadExerciseVideoOfPatient(
        patientID: UUID,
        exerciseID: Int,
        exerciseVideoID: UUID,
        date: LocalDate,
        set: Int,
        patientFeedback: String?,
        executionMode: String?,
        withLoad: Boolean?,
        loadValue: Float?,
        loadUnit: String?,
        executionScore: String?
    ): Boolean {
        handle.createUpdate(
            "insert into dbo.exercise_session (id, patient_id, daily_exercise_id, dt_submit, nr_set, patient_feedback, execution_mode, with_load, load_value, load_unit, execution_score) " +
                "values (:exerciseVideoID,:patientID,:exerciseID,:date,:set,:patientFeedback,:executionMode,:withLoad,:loadValue,:loadUnit,:executionScore)"
        )
            .bind("exerciseVideoID", exerciseVideoID)
            .bind("exerciseID", exerciseID)
            .bind("date", date)
            .bind("patientID", patientID)
            .bind("patientFeedback", patientFeedback)
            .bind("executionMode", executionMode)
            .bind("withLoad", withLoad)
            .bind("loadValue", loadValue)
            .bind("loadUnit", loadUnit)
            .bind("executionScore", executionScore)
            .bind("set", set)
            .execute()

        handle.createUpdate(
            "insert into dbo.exercise_session_file (session_id, file_path, file_format, source) values (:exerciseVideoID, :filePath, 'mp4', 'cloud')"
        )
            .bind("exerciseVideoID", exerciseVideoID)
            .bind("filePath", exerciseVideoID.toString())
            .execute()

        return handle.createQuery(
            "select " +
                "case when count(es.daily_exercise_id) != de.sets then 0 else 1 end " +
                "from dbo.exercise_session es " +
                "inner join dbo.daily_exercise de on es.daily_exercise_id = de.id where es.daily_exercise_id = :exerciseID and es.patient_id = :patientID " +
                "GROUP BY de.sets"
        )
            .bind("exerciseID", exerciseID)
            .bind("patientID", patientID)
            .mapTo<Int>()
            .single() == 1
    }

    override fun getPatientsVideosIDs(): List<UUID> =
        handle.createQuery("select session_id from dbo.exercise_session_file where file_format = 'mp4' and source = 'cloud'").mapTo<UUID>().toList()

    override fun deletePatientVideoID(videoID: UUID) {
        handle.createUpdate("delete from dbo.exercise_session_file where session_id = :videoID")
            .bind("videoID", videoID)
            .execute()
        handle.createUpdate("delete from dbo.exercise_session where id = :videoID")
            .bind("videoID", videoID)
            .execute()
    }
}
