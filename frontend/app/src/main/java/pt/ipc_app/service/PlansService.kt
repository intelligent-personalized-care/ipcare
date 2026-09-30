package pt.ipc_app.service

import com.google.gson.Gson
import okhttp3.OkHttpClient
import pt.ipc_app.domain.Plan
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.models.EmptyResponseBody
import pt.ipc_app.service.models.plans.CreatePlanOutput
import pt.ipc_app.service.models.plans.ListOfPlans
import pt.ipc_app.service.models.plans.PlanInput
import pt.ipc_app.service.models.plans.PlanToPatient
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

/**
 * The service that handles the plan functionalities.
 *
 * @property apiEndpoint the API endpoint
 * @property httpClient the HTTP client
 * @property jsonEncoder the JSON encoder used to serialize/deserialize objects
 */
class PlansService(
    apiEndpoint: String,
    httpClient: OkHttpClient,
    jsonEncoder: Gson
) : HTTPService(apiEndpoint, httpClient, jsonEncoder) {

    /**
     * Creates a plan.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun createPlan(
        plan: PlanInput,
        physiotherapistId: UUID,
        token: String
    ): APIResult<CreatePlanOutput> =
        post(
            uri = "/users/physiotherapists/$physiotherapistId/plans",
            token = token,
            body = plan
        )

    /**
     * Gets a plan of patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPlanOfPatient(
        patientId: UUID,
        date: String,
        token: String
    ): APIResult<Plan> =
        get(
            uri = "/users/patients/$patientId/plans?date=$date",
            token = token
        )

    /**
     * Associates plan to a patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun associatePlanToPatient(
        physiotherapistId: UUID,
        patientId: UUID,
        token: String,
        planId: Int,
        startDate: String
    ): APIResult<EmptyResponseBody> =
        post(
            uri = "/users/physiotherapists/$physiotherapistId/patients/$patientId/plans",
            token = token,
            body = PlanToPatient(planId, startDate)
        )

    /**
     * Gets plans of physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getLibraryPlan(physiotherapistId: UUID, planId: Int, token: String): APIResult<pt.ipc_app.service.models.plans.LibraryPlan> =
        get(uri = "/users/physiotherapists/$physiotherapistId/plans/$planId", token = token)

    suspend fun getPhysiotherapistPlans(
        physiotherapistId: UUID,
        token: String
    ): APIResult<ListOfPlans> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/plans",
            token = token
        )

}