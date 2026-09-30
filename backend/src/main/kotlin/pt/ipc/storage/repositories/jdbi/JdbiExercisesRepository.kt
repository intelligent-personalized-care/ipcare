package pt.ipc.storage.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.ipc.domain.exercises.Exercise
import pt.ipc.domain.exercises.ExerciseInfo
import pt.ipc.domain.exercises.ExerciseType
import pt.ipc.domain.plan.VideoFeedBack
import pt.ipc.storage.repositories.ExerciseRepository
import java.time.Duration
import java.time.LocalDate
import java.util.*

class JdbiExercisesRepository(
    private val handle: Handle
) : ExerciseRepository {

    override fun getExercise(exerciseID: UUID): ExerciseInfo? {
        return handle.createQuery("select *, exercise_type as type from dbo.exercise_info where id = :exerciseID")
            .bind("exerciseID", exerciseID)
            .mapTo<ExerciseInfo>()
            .singleOrNull()
    }

    override fun getExercises(skip: Int, limit: Int, joint: String?): List<ExerciseInfo> {
        return handle.createQuery("select *, exercise_type as type from dbo.exercise_info where (cast(:joint as varchar) is null or camera_joint = :joint) order by title, id offset :skip limit :limit")
            .bind("joint", joint)
            .bind("skip", skip)
            .bind("limit", limit)
            .mapTo<ExerciseInfo>()
            .toList()
    }

    override fun getExerciseByType(type: ExerciseType, skip: Int, limit: Int, joint: String?): List<ExerciseInfo> {
        return handle.createQuery("select *, exercise_type as type from dbo.exercise_info where exercise_type = :type and (cast(:joint as varchar) is null or camera_joint = :joint) order by title, id offset :skip limit :limit")
            .bind("joint", joint)
            .bind("type", type)
            .bind("skip", skip)
            .bind("limit", limit)
            .mapTo<ExerciseInfo>()
            .toList()
    }

    override fun getAllExercisesOfPatient(patientID: UUID, skip: Int, limit: Int): List<Exercise> {
        val sql = """
        SELECT de.exercise_id as ex_id, de.sets, de.reps
        FROM dbo.daily_exercise de
        JOIN dbo.daily_list dl ON de.daily_list_id = dl.id
        JOIN dbo.plan p ON dl.plan_id = p.id
        JOIN dbo.patient_plan pp ON p.id = pp.plan_id
        WHERE pp.patient_id = :patientID
        offset :skip
        limit :limit
    """
        return handle.createQuery(sql)
            .bind("patientID", patientID)
            .bind("skip", skip)
            .bind("limit", limit)
            .mapTo<Exercise>()
            .list()
    }

    override fun getExercisesOfDay(patientID: UUID, date: LocalDate): List<Exercise> {
        val sql = """
        SELECT de.exercise_id as ex_id, de.sets, de.reps
        FROM dbo.daily_exercise de
        JOIN dbo.daily_list dl ON de.daily_list_id = dl.id
        JOIN dbo.plan p ON dl.plan_id = p.id
        JOIN dbo.patient_plan pp ON p.id = pp.plan_id
        WHERE pp.patient_id = :patientID
        AND dl.day_index = :dayIndex
        AND :date BETWEEN pp.dt_start AND pp.dt_end
    """

        val dtStart = handle.createQuery(
            "SELECT dt_start FROM " +
                "dbo.patient_plan cp WHERE cp.patient_id = :patientID and :date between cp.dt_start and cp.dt_end"
        )
            .bind("patientID", patientID)
            .bind("date", date)
            .mapTo<LocalDate>()
            .singleOrNull() ?: return emptyList()

        val dayIndex = Duration.between(dtStart.atStartOfDay(), date.atStartOfDay()).toDays().toInt()

        return handle.createQuery(sql)
            .bind("patientID", patientID)
            .bind("dayIndex", dayIndex)
            .bind("date", date)
            .mapTo<Exercise>()
            .list()
    }

    override fun addExerciseInfoPreview(exerciseID: UUID, title: String, description: String, type: ExerciseType) {
        handle.createUpdate("insert into dbo.exercise_info(id, title, description, exercise_type) values(:id,:title,:description,:type)")
            .bind("id", exerciseID)
            .bind("title", title)
            .bind("description", description)
            .bind("type", type)
            .execute()
    }

    override fun getPatientVideoID(patientID: UUID, planID: Int, dailyListID: Int, dailyExerciseID: Int, set: Int): UUID? =
        handle.createQuery(
            "select es.id from dbo.exercise_session es " +
                "inner join dbo.daily_exercise de on de.id = es.daily_exercise_id " +
                "inner join dbo.daily_list dl on de.daily_list_id = dl.id " +
                "inner join dbo.plan p on p.id = dl.plan_id " +
                "inner join dbo.patient_plan pp on pp.plan_id = p.id " +
                "where de.id = :dailyExerciseID and dl.id = :dailyListID and dl.plan_id = :planID and pp.patient_id = :patientID and es.patient_id = :patientID and es.nr_set = :set"
        )
            .bind("dailyExerciseID", dailyExerciseID)
            .bind("dailyListID", dailyListID)
            .bind("planID", planID)
            .bind("patientID", patientID)
            .bind("set", set)
            .mapTo<UUID>()
            .singleOrNull()

    override fun getVideoFeedback(videoID: UUID): VideoFeedBack =
        handle.createQuery(
            "select patient_feedback, physiotherapist_feedback, with_load as withLoad, load_value as loadValue, load_unit as loadUnit, execution_mode as executionMode, execution_score as executionScore, physiotherapist_feedback_score as physiotherapistFeedbackScore from dbo.exercise_session where id = :videoID"
        )
            .bind("videoID", videoID)
            .mapTo<VideoFeedBack>()
            .single()

    override fun deletePreview(videoID: UUID) {
        handle.createUpdate("delete from dbo.exercise_info where id = :videoID")
            .bind("videoID", videoID)
            .execute()
    }

    override fun getPreviewsIDs(): List<UUID> =
        handle.createQuery("select id from dbo.exercise_info").mapTo<UUID>().toList()
}
