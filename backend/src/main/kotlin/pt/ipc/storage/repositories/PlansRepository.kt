package pt.ipc.storage.repositories

import pt.ipc.domain.plan.PlanInfoOutput
import pt.ipc.domain.plan.PlanInput
import pt.ipc.domain.plan.PlanOutput
import java.time.LocalDate
import java.util.*

interface PlansRepository {

    fun createPlan(physiotherapistID: UUID, plan: PlanInput): Int

    fun associatePlanToPatient(planID: Int, patientID: UUID, startDate: LocalDate, endDate: LocalDate)

    fun getPlan(planID: Int, patientID: UUID? = null): PlanOutput?

    fun getPlanOfPhysiotherapist(planID: Int): PlanOutput?

    fun getPlans(physiotherapistID: UUID): List<PlanInfoOutput>

    fun getPlanOfPatientContainingDate(patientID: UUID, date: LocalDate): PlanOutput?

    fun checkIfPlanIsOfPhysiotherapist(physiotherapistID: UUID, planID: Int): Boolean

    fun checkIfExistsPlanOfPatientInThisPeriod(patientID: UUID, startDate: LocalDate, endDate: LocalDate): Boolean

    fun checkIfPatientAlreadyUploadedVideo(patientID: UUID, planID: Int, dailyListID: Int, exerciseID: Int, set: Int): Boolean

    fun checkIfPhysiotherapistHasPrescribedExercise(planID: Int, exerciseID: Int, physiotherapistID: UUID): Boolean

    fun giveFeedBackOfVideo(patientID: UUID, exerciseID: Int, set: Int, feedBack: String, feedbackScore: String? = null)
}
