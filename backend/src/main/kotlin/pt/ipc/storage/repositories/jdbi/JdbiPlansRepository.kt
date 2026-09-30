package pt.ipc.storage.repositories.jdbi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.ipc.domain.exercises.DailyExercise
import pt.ipc.domain.plan.DailyListOutput
import pt.ipc.domain.plan.PlanInfoOutput
import pt.ipc.domain.plan.PlanInput
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.storage.repositories.PlansRepository
import java.time.LocalDate
import java.util.*

class JdbiPlansRepository(
    private val handle: Handle
) : PlansRepository {

    override fun createPlan(physiotherapistID: UUID, plan: PlanInput): Int {
        plan.dailyLists.filterNotNull().flatMap { it.exercises }.forEach { it.sensorProfile?.validate() }
        val planID = handle.createQuery("insert into dbo.plan (physiotherapist_id, title) values(:physiotherapistID, :title) returning id")
            .bind("physiotherapistID", physiotherapistID)
            .bind("title", plan.title)
            .mapTo<Int>()
            .first()

        plan.dailyLists.forEachIndexed { index, dailyListCreation ->
            val dailyListID =
                handle.createQuery("insert into dbo.daily_list (day_index, plan_id) values (:index,:planID) returning id")
                    .bind("index", index)
                    .bind("planID", planID)
                    .mapTo<Int>()
                    .first()

            dailyListCreation?.exercises?.forEach { exerciseCreation ->
                handle.createUpdate("insert into dbo.daily_exercise (exercise_id, daily_list_id, sets, reps, sensor_profile) values (:exID,:dailyListID,:sets,:reps,cast(:profile as jsonb))")
                    .bind("exID", exerciseCreation.exerciseInfoID)
                    .bind("dailyListID", dailyListID)
                    .bind("sets", exerciseCreation.sets)
                    .bind("reps", exerciseCreation.reps)
                    .bind("profile", exerciseCreation.sensorProfile?.let { jacksonObjectMapper().writeValueAsString(it) })
                    .execute()
            }
        }
        return planID
    }

    override fun associatePlanToPatient(planID: Int, patientID: UUID, startDate: LocalDate, endDate: LocalDate) {
        val assignedPlanID = handle.createQuery(
            "insert into dbo.plan(physiotherapist_id, title, template_plan_id) " +
                "select physiotherapist_id, title, coalesce(template_plan_id, id) from dbo.plan where id = :planID returning id"
        ).bind("planID", planID).mapTo<Int>().one()
        val lists = handle.createQuery("select id, day_index from dbo.daily_list where plan_id = :planID order by day_index")
            .bind("planID", planID).map { rs, _ -> rs.getInt("id") to rs.getInt("day_index") }.list()
        lists.forEach { (sourceID, dayIndex) ->
            val targetID = handle.createQuery("insert into dbo.daily_list(plan_id, day_index) values (:plan, :day) returning id")
                .bind("plan", assignedPlanID).bind("day", dayIndex).mapTo<Int>().one()
            handle.createUpdate("insert into dbo.daily_exercise(exercise_id, daily_list_id, sets, reps, sensor_profile) select exercise_id, :target, sets, reps, sensor_profile from dbo.daily_exercise where daily_list_id = :source")
                .bind("target", targetID).bind("source", sourceID).execute()
        }
        handle.createUpdate("insert into dbo.patient_plan (plan_id,patient_id,dt_start, dt_end) values(:planID, :patientID, :startDate, :endDate)")
            .bind("planID", assignedPlanID)
            .bind("patientID", patientID)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .execute()
    }

    override fun getPlan(planID: Int, patientID: UUID?): PlanOutput? {
        val title = handle.createQuery("select title from dbo.plan where id = :planID")
            .bind("planID", planID)
            .mapTo<String>()
            .singleOrNull() ?: return null

        val dailyListsID: List<Int> =
            handle.createQuery("select id from dbo.daily_list where plan_id = :planID order by day_index")
                .bind("planID", planID)
                .mapTo<Int>()
                .toList()

        val dailyLists = mutableListOf<DailyListOutput?>()

        dailyListsID.forEachIndexed { index, dailyListID ->

            val exercises: List<DailyExercise>? =
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
                    .bind("patientID", patientID)
                    .mapTo<DailyExercise>()
                    .toList()
                    .ifEmpty { null }

            dailyLists.add(
                index,
                if (exercises != null) {
                    DailyListOutput(dailyListID, exercises)
                } else {
                    null
                }
            )
        }

        return PlanOutput(
            id = planID,
            title = title,
            dailyLists = dailyLists
        )
    }

    override fun getPlanOfPhysiotherapist(planID: Int): PlanOutput? {
        val title = handle.createQuery("select title from dbo.plan where id = :planID")
            .bind("planID", planID)
            .mapTo<String>()
            .singleOrNull() ?: return null

        val dailyListsID: List<Int> =
            handle.createQuery("select id from dbo.daily_list where plan_id = :planID order by day_index")
                .bind("planID", planID)
                .mapTo<Int>()
                .toList()

        val dailyLists = mutableListOf<DailyListOutput?>()

        dailyListsID.forEachIndexed { index, dailyListID ->

            val exercises: List<DailyExercise>? =
                handle.createQuery(
                    """
                    select de.id, de.exercise_id as ex_id, ei.title, ei.description, ei.exercise_type as type, de.sets, de.reps
                    from dbo.daily_exercise de
                    inner join dbo.daily_list dl on de.daily_list_id = dl.id
                    inner join dbo.exercise_info ei on ei.id = de.exercise_id
                    where daily_list_id = :dailyListID
                    """.trimIndent()
                )
                    .bind("dailyListID", dailyListID)
                    .mapTo<DailyExercise>()
                    .toList()
                    .ifEmpty { null }

            dailyLists.add(
                index,
                if (exercises != null) {
                    DailyListOutput(dailyListID, exercises)
                } else {
                    null
                }
            )
        }

        return PlanOutput(
            id = planID,
            title = title,
            dailyLists = dailyLists
        )
    }

    override fun getPlans(physiotherapistID: UUID): List<PlanInfoOutput> {
        return handle.createQuery(
            """
                select p.id, p.title, count(dl.id) as days from dbo.plan p
                inner join dbo.physiotherapist m on p.physiotherapist_id = m.id
                inner join dbo.daily_list dl on p.id = dl.plan_id
                where m.id = :physiotherapistID and p.template_plan_id is null
                group by p.id
            """.trimIndent()
        )
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<PlanInfoOutput>()
            .toList()
    }

    private data class PlanStartDate(val planId: Int, val dtStart: LocalDate)

    override fun getPlanOfPatientContainingDate(patientID: UUID, date: LocalDate): PlanOutput? {
        val plan = handle.createQuery(
            """
                select cp.plan_id, cp.dt_start from dbo.patient_plan cp
                where cp.patient_id = :patientID and :date between cp.dt_start and cp.dt_end
            """.trimIndent()
        )
            .bind("patientID", patientID)
            .bind("date", date)
            .mapTo<PlanStartDate>()
            .singleOrNull() ?: return null

        return getPlan(plan.planId, patientID)?.copy(startDate = plan.dtStart)
    }

    override fun checkIfPlanIsOfPhysiotherapist(physiotherapistID: UUID, planID: Int): Boolean =
        handle.createQuery("select count(*) from dbo.plan where id = :planID and physiotherapist_id = :physiotherapistID")
            .bind("planID", planID)
            .bind("physiotherapistID", physiotherapistID)
            .mapTo<Int>()
            .single() == 1

    override fun checkIfExistsPlanOfPatientInThisPeriod(patientID: UUID, startDate: LocalDate, endDate: LocalDate): Boolean {
        return handle.createQuery(
            "select count(*) from dbo.patient_plan cp where " +
                "(cp.dt_end >= :startDate and :endDate >= cp.dt_start) and cp.patient_id = :patientID"
        )
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .bind("patientID", patientID)
            .mapTo<Int>()
            .single() >= 1
    }

    override fun checkIfPhysiotherapistHasPrescribedExercise(planID: Int, exerciseID: Int, physiotherapistID: UUID): Boolean {
        return handle.createQuery(
            "select count(*) from dbo.daily_exercise de " +
                "inner join dbo.daily_list dl on de.daily_list_id = dl.id " +
                "inner join dbo.plan p on dl.plan_id = p.id " +
                "where de.id = :exerciseID and p.physiotherapist_id = :physiotherapistID and p.id = :planID"
        )
            .bind("exerciseID", exerciseID)
            .bind("physiotherapistID", physiotherapistID)
            .bind("planID", planID)
            .mapTo<Int>()
            .single() == 1
    }

    override fun checkIfPatientAlreadyUploadedVideo(
        patientID: UUID,
        planID: Int,
        dailyListID: Int,
        exerciseID: Int,
        set: Int
    ): Boolean {
        return handle.createQuery(
            """
                select count(*) from dbo.exercise_session es
                inner join dbo.daily_exercise de on es.daily_exercise_id = de.id
                inner join dbo.daily_list dl on dl.id = de.daily_list_id
                inner join dbo.plan p on p.id = dl.plan_id
                inner join dbo.patient_plan pp on pp.plan_id = p.id
                where es.nr_set = :set and es.patient_id = :patientID and es.daily_exercise_id = :exerciseID and dl.id = :dailyListID and dl.plan_id = :planID and pp.patient_id = :patientID
            """.trimIndent()
        )
            .bind("set", set)
            .bind("exerciseID", exerciseID)
            .bind("dailyListID", dailyListID)
            .bind("planID", planID)
            .bind("patientID", patientID)
            .mapTo<Int>()
            .single() == 1
    }

    override fun giveFeedBackOfVideo(patientID: UUID, exerciseID: Int, set: Int, feedBack: String, feedbackScore: String?) {
        handle.createUpdate(
            """
            update dbo.exercise_session es
            set physiotherapist_feedback = :feedBack,
                physiotherapist_feedback_score = :feedbackScore
            from dbo.daily_exercise de
            inner join dbo.daily_list dl on dl.id = de.daily_list_id
            inner join dbo.plan p on p.id = dl.plan_id
            inner join dbo.patient_plan pp on pp.plan_id = p.id
            where es.daily_exercise_id = de.id and es.patient_id = :patientID and de.id = :exerciseID and pp.patient_id = :patientID and es.nr_set = :set
            """.trimIndent()
        )
            .bind("feedBack", feedBack)
            .bind("feedbackScore", feedbackScore)
            .bind("exerciseID", exerciseID)
            .bind("patientID", patientID)
            .bind("set", set)
            .execute()
    }
}
