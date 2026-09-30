package pt.ipc_app.service

import android.content.Context
import coil.request.ImageRequest
import com.google.gson.Gson
import okhttp3.OkHttpClient
import pt.ipc_app.domain.Plan
import pt.ipc_app.domain.user.Role
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.connection.AUTHORIZATION
import pt.ipc_app.service.models.EmptyResponseBody
import pt.ipc_app.service.models.requests.ConnectionRequestInput
import pt.ipc_app.service.models.exercises.ExercisesOfPatients
import pt.ipc_app.service.models.login.LoginInput
import pt.ipc_app.service.models.login.LoginOutput
import pt.ipc_app.service.models.refresh.RefreshTokenInput
import pt.ipc_app.service.models.refresh.RefreshTokenOutput
import pt.ipc_app.service.models.register.RegisterPatientInput
import pt.ipc_app.service.models.register.RegisterPhysiotherapistInput
import pt.ipc_app.service.models.register.RegisterOutput
import pt.ipc_app.service.models.requests.ConnectionRequestDecisionInput
import pt.ipc_app.service.models.requests.RequestsOfPhysiotherapist
import pt.ipc_app.service.models.users.*
import pt.ipc_app.service.utils.ContentType
import pt.ipc_app.service.utils.MultipartEntry
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

/**
 * The service that handles the user functionalities.
 *
 * @property apiEndpoint the API endpoint
 * @property httpClient the HTTP client
 * @property jsonEncoder the JSON encoder used to serialize/deserialize objects
 */
class UsersService(
    apiEndpoint: String,
    httpClient: OkHttpClient,
    jsonEncoder: Gson
) : HTTPService(apiEndpoint, httpClient, jsonEncoder) {

    /**
     * Registers the patient with the given [name], [email] and [password].
     *
     * @return the API result of the register request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun registerPatient(
        name: String,
        email: String,
        password: String,
        weight: Int?,
        height: Int?,
        birthDate: String?,
        physicalCondition: String?
    ): APIResult<RegisterOutput> =
        post(
            uri = "/users/patients",
            body = RegisterPatientInput(
                name = name,
                email = email,
                password = password,
                weight = weight,
                height = height,
                birthDate = birthDate,
                physicalCondition = physicalCondition
            )
        )

    /**
     * Registers the physiotherapist with the given [name], [email] and [password].
     *
     * @return the API result of the register request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun registerPhysiotherapist(
        name: String,
        email: String,
        password: String
    ): APIResult<RegisterOutput> =
        post(
            uri = "/users/physiotherapists",
            body = RegisterPhysiotherapistInput(
                name = name,
                email = email,
                password = password
            )
        )

    /**
     * Logs in the user with the given [email] and [password].
     *
     * @return the API result of the register request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun login(
        email: String,
        password: String
    ): APIResult<LoginOutput> =
        post(
            uri = "/users/login",
            body = LoginInput(
                email = email,
                password = password
            )
        )

    /**
     * Updates the tokens of user.
     *
     * @return the API result of the register request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun refreshTokens(
        refreshToken: String
    ): APIResult<RefreshTokenOutput> =
        post(
            uri = "/users/refresh",
            body = RefreshTokenInput(refreshToken)
        )

    /**
     * Gets the patient profile.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPatientProfile(
        patientId: UUID,
        token: String
    ): APIResult<PatientOutput> =
        get(
            uri = "/users/patients/$patientId/profile",
            token = token
        )

    /**
     * Gets the physiotherapist profile.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPhysiotherapistProfile(
        physiotherapistId: UUID,
        token: String
    ): APIResult<PhysiotherapistProfile> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/profile",
            token = token
        )

    /**
     * Gets the physiotherapist of patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPhysiotherapistOfPatient(
        patientId: UUID,
        token: String
    ): APIResult<PhysiotherapistOutput> =
        get(
            uri = "/users/patients/$patientId/physiotherapist",
            token = token
        )

    /**
     * Gets patient's details of physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPatientOfPhysiotherapist(
        physiotherapistId: UUID,
        patientId: UUID,
        token: String
    ): APIResult<PatientOfPhysiotherapist> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/patients/$patientId",
            token = token
        )

    /**
     * Gets patients of physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPatientsOfPhysiotherapist(
        physiotherapistId: UUID,
        token: String
    ): APIResult<PatientsOfPhysiotherapist> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/patients",
            token = token
        )

    /**
     * Search physiotherapists available.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun searchPhysiotherapistsAvailable(
        name: String?,
        token: String
    ): APIResult<ListPhysiotherapistsOutput> =
        get(
            uri = "/users/physiotherapists" + if (name != null) "?name=${java.net.URLEncoder.encode(name, "UTF-8")}" else "",
            token = token
        )

    /**
     * Gets the current plan of patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getCurrentPlanOfPatient(
        patientId: UUID,
        date: LocalDate = LocalDate.now(),
        token: String
    ): APIResult<Plan> =
        get(
            uri = "/users/patients/$patientId/plans?date=$date",
            token = token
        )

    /**
     * Connects the physiotherapist with the patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun connectPhysiotherapist(
        physiotherapistId: UUID,
        patientId: UUID,
        comment: String?,
        token: String
    ): APIResult<EmptyResponseBody> =
        post(
            uri = "/users/physiotherapists/$physiotherapistId",
            token = token,
            body = ConnectionRequestInput(patientId, comment)
        )

    /**
     * Disconnects a patient from the physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun disconnectPhysiotherapist(
        patientId: UUID,
        physiotherapistId: UUID? = null,
        token: String
    ): APIResult<EmptyResponseBody> =
        delete(
            uri = if (physiotherapistId != null) "/users/physiotherapists/$physiotherapistId/patients/$patientId" else "/users/patients/$patientId/physiotherapist",
            token = token
        )

    /**
     * Gets all physiotherapist requests of physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getPhysiotherapistRequests(
        physiotherapistId: UUID,
        token: String
    ): APIResult<RequestsOfPhysiotherapist> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/requests",
            token = token
        )

    /**
     * Gets all exercises of patients of physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getExercisesOfPatients(
        physiotherapistId: UUID,
        date: LocalDate,
        token: String
    ): APIResult<ExercisesOfPatients> =
        get(
            uri = "/users/physiotherapists/$physiotherapistId/patients/exercises?date=$date",
            token = token
        )

    /**
     * Decide connection request.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun decideConnectionRequest(
        physiotherapistId: UUID,
        requestId: UUID,
        requestDecision: ConnectionRequestDecisionInput,
        token: String
    ): APIResult<PatientsOfPhysiotherapist> =
        post(
            uri = "/users/physiotherapists/$physiotherapistId/requests/$requestId",
            token = token,
            body = requestDecision
        )

    /**
     * Rates the physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun ratePhysiotherapist(
        physiotherapistId: UUID,
        patientId: UUID,
        stars: Int,
        token: String
    ): APIResult<EmptyResponseBody> =
        post(
            uri = "/users/physiotherapists/$physiotherapistId/rate",
            token = token,
            body = RatingInput(patientId, stars)
        )

    /**
     * Gets the profile picture of an user.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    fun getProfilePicture(
        context: Context,
        userId: UUID,
        token: String
    ): ImageRequest =
        ImageRequest.Builder(context)
            .data("$apiEndpoint/users/$userId/photo")
            .addHeader(AUTHORIZATION, "$BEARER_TOKEN $token")
            .build()

    /**
     * Updates the profile picture of an user.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun updateProfilePicture(
        image: File,
        userId: UUID,
        role: Role,
        token: String
    ): APIResult<EmptyResponseBody> =
        postWithMultipartBody(
            uri = "/users/${role.name.lowercase()}s/$userId/profile/photo",
            token = token,
            multipartEntries = listOf(MultipartEntry(name = "photo", value = image, contentType = ContentType.IMAGE))
        )

    /**
     * Submits the credential document of a physiotherapist.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun submitCredentialDocument(
        doc: File,
        physiotherapistId: UUID,
        token: String
    ): APIResult<EmptyResponseBody> =
        postWithMultipartBody(
            uri = "/users/physiotherapists/$physiotherapistId/credential",
            token = token,
            multipartEntries = listOf(MultipartEntry(name = "credential", value = doc, contentType = ContentType.PDF))
        )

}

