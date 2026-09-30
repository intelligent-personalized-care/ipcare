package pt.ipc_app.service

import com.google.gson.Gson
import okhttp3.OkHttpClient
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.models.EmptyResponseBody
import pt.ipc_app.service.models.exercises.ExerciseVideoFeedback
import pt.ipc_app.service.models.exercises.FeedbackInput
import pt.ipc_app.service.models.exercises.ListOfExercisesInfo
import pt.ipc_app.service.models.exercises.SensorSession
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.service.models.exercises.SensorProgress
import pt.ipc_app.service.utils.ContentType
import pt.ipc_app.service.utils.MultipartEntry
import java.io.File
import java.io.IOException
import java.util.*

/**
 * The service that handles the exercises functionalities.
 *
 * @property apiEndpoint the API endpoint
 * @property httpClient the HTTP client
 * @property jsonEncoder the JSON encoder used to serialize/deserialize objects
 */
class ExercisesService(
    apiEndpoint: String,
    httpClient: OkHttpClient,
    jsonEncoder: Gson
) : HTTPService(apiEndpoint, httpClient, jsonEncoder) {

    suspend fun getExerciseInfo(id: UUID, token: String): APIResult<pt.ipc_app.domain.exercise.ExerciseInfo> =
        get(uri = "/exercises/$id", token = token)

    /** Effective prescription shared by camera and wearable execution. */
    suspend fun getExerciseProfile(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int, token: String): APIResult<SensorProfile> =
        get(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/profile", token = token)

    suspend fun getSensorProfile(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int, token: String): APIResult<SensorProfile> =
        get(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor/profile", token = token)

    suspend fun resetSensorProfile(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int, token: String): APIResult<SensorProfile> =
        post(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor/profile/defaults", token = token, body = emptyMap<String, String>())

    suspend fun saveSensorProfile(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int,
                                 profile: SensorProfile, token: String): APIResult<SensorProfile> =
        post(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor/profile", token = token, body = profile)

    suspend fun getSensorProgress(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int, token: String): APIResult<SensorProgress> =
        get(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor/progress", token = token)

    suspend fun submitSensorSession(patientId: UUID, planId: Int, dailyListId: Int, exerciseId: Int,
                                    session: SensorSession, token: String): APIResult<SensorSession> =
        post(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor",
            token = token, body = session)

    suspend fun getSensorSession(patientId: String, planId: Int, dailyListId: Int, exerciseId: Int,
                                set: Int, token: String): APIResult<SensorSession> =
        get(uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/sensor?set=$set",
            token = token)

    /**
     * Gets all the exercises.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getExercises(
        skip: Int = 0,
        joint: String? = null,
        token: String
    ): APIResult<ListOfExercisesInfo> =
        get(
            uri = "/exercises?skip=$skip" + (joint?.let { "&joint=$it" } ?: ""),
            token = token
        )

    fun getExercisePreviewUrl(
        exerciseInfoId: UUID
    ): String =
        "$apiEndpoint/exercises/$exerciseInfoId/video"

    fun getExerciseVideoOfPatientUrl(
        patientId: String,
        planId: Int,
        dailyListId: Int,
        exerciseId: Int,
        set: Int
    ): String =
        "$apiEndpoint/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId?set=$set"

    /**
     * Gets physiotherapist feedback of a patient exercise.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun getFeedbackOfPhysiotherapist(
        patientId: String,
        planId: Int,
        dailyListId: Int,
        exerciseId: Int,
        set: Int,
        token: String
    ): APIResult<ExerciseVideoFeedback> =
        get(
            uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/feedback?set=$set",
            token = token
        )

    /**
     * Sends physiotherapist feedback of a patient exercise.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun sendFeedbackToExerciseDone(
        patientId: String,
        planId: Int,
        dailyListId: Int,
        exerciseId: Int,
        set: Int,
        feedback: String,
        token: String,
        rating: Int? = null
    ): APIResult<EmptyResponseBody> =
        post(
            uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId/feedback",
            token = token,
            body = FeedbackInput(
                set = set,
                feedback = feedback,
                feedbackScore = rating?.also { require(it in 1..5) }?.toString()
            )
        )

    /**
     * Submit an exercise video of Patient.
     *
     * @return the API result of the request
     *
     * @throws IOException if there is an error while sending the request
     */
    suspend fun submitExerciseVideo(
        video: File,
        patientId: UUID,
        planId: Int,
        dailyListId: Int,
        exerciseId: Int,
        set: Int,
        token: String,
        withLoad: Boolean? = null,
        loadValue: Float? = null
    ): APIResult<EmptyResponseBody> =
        postWithMultipartBody(
            uri = "/users/patients/$patientId/plans/$planId/daily_lists/$dailyListId/exercises/$exerciseId",
            token = token,
            multipartEntries = listOf(
                MultipartEntry(name = "video", value = video, contentType = ContentType.VIDEO),
                MultipartEntry(name = "set", value = set)
            ) + (withLoad?.let { listOf(MultipartEntry(name = "withLoad", value = it.toString())) } ?: emptyList()) +
                (loadValue?.let { listOf(MultipartEntry(name = "loadValue", value = it.toString()), MultipartEntry(name = "loadUnit", value = "kg")) } ?: emptyList())
        )

}
