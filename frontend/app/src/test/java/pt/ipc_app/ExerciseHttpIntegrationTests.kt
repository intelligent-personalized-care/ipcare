package pt.ipc_app

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pt.ipc_app.ble.SensorExerciseCommand
import pt.ipc_app.service.ExercisesService
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.connection.UnexpectedResponseException
import pt.ipc_app.service.models.exercises.*
import pt.ipc_app.service.utils.ProblemJson
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real OkHttp sockets, coroutines and Gson against an isolated loopback HTTP fixture. */
class ExerciseHttpIntegrationTests {
    private data class Reply(val status: Int, val body: String, val type: String = "application/json")
    private data class Recorded(val method: String, val uri: String, val authorization: String?, val body: String)
    private fun reply(value: Reply) {
        server.enqueue(MockResponse().setResponseCode(value.status)
            .setHeader("Content-Type", value.type).setBody(value.body))
    }
    private val gson = Gson()
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var service: ExercisesService
    private val patient = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    private val base get() = "/users/patients/$patient/plans/2/daily_lists/3/exercises/4"
    private val profile = SensorProfile(60f, 10f, 1000, 2000, 800, 0f, -10f, 110f, -45f, 45f, false, -1)

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        service = ExercisesService(server.url("/").toString().removeSuffix("/"), client, gson)
    }

    @After fun teardown() {
        server.shutdown()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdownNow()
    }

    private fun request(): Recorded {
        val value = server.takeRequest(3, TimeUnit.SECONDS) ?: error("Request not received")
        return Recorded(value.method!!, value.path!!, value.getHeader("Authorization"), value.body.readUtf8())
    }
    private fun <T> success(result: APIResult<T>): T {
        assertTrue("Expected successful HTTP result", result is APIResult.Success)
        return (result as APIResult.Success).data
    }

    @Test fun rejectedVideoCanBeRetriedWithTheSameSetAndLoadWithoutDeletingTheOriginal() = runBlocking {
        val file = java.io.File.createTempFile("ipc-video-test", ".mp4")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3, 4))
            reply(Reply(413, "Request entity too large", "text/html"))
            try {
                service.submitExerciseVideo(file, patient, 2, 3, 4, 2, "token", true, 2.5f)
                fail("Expected 413")
            } catch (error: UnexpectedResponseException) { assertEquals(413, error.statusCode) }
            val rejected = request()
            assertTrue(file.isFile)
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), file.readBytes())
            reply(Reply(204, ""))
            success(service.submitExerciseVideo(file, patient, 2, 3, 4, 2, "token", true, 2.5f))
            val retried = request()
            assertEquals(base, retried.uri)
            assertEquals(rejected.uri, retried.uri)
            assertEquals("Bearer token", retried.authorization)
            for (body in listOf(rejected.body, retried.body)) {
                fun field(name: String) = body.split("\r\n--").first { it.contains("name=\"$name\"") }
                    .substringAfter("\r\n\r\n").substringBefore("\r\n")
                assertEquals("2", field("set"))
                assertEquals("true", field("withLoad"))
                assertEquals("2.5", field("loadValue"))
                assertEquals("kg", field("loadUnit"))
            }
            assertTrue(file.isFile)
        } finally { file.delete() }
    }

    @Test fun effectivePrescriptionTravelsFromHttpToWearableCommand() = runBlocking {
        reply(Reply(200, gson.toJson(profile)))
        val loaded = success(service.getExerciseProfile(patient.toString(), 2, 3, 4, "test-access"))
        assertEquals(profile, loaded)
        assertEquals("CALIBRATE2:Elbow extension,10,60.0,10.0,1000,2000,800,0.0,-10.0,110.0,-45.0,45.0,0,-1",
            SensorExerciseCommand.calibration("Elbow extension", loaded, 10))
        val req = request()
        assertEquals("GET", req.method)
        assertEquals("$base/profile", req.uri)
        assertEquals("Bearer test-access", req.authorization)
    }

    @Test fun sessionSubmissionKeepsIdentitySamplesProfileAndLoadAcrossRetry() = runBlocking {
        val session = SensorSession(UUID.randomUUID(), 1, 10, 2000,
            listOf(SensorSample(500, -62f, 4f, 8f)), profile, true, 2.5f, "kg")
        repeat(2) {
            reply(Reply(200, gson.toJson(session)))
            assertEquals(session, success(service.submitSensorSession(patient, 2, 3, 4, session, "token")))
            val req = request()
            assertEquals("POST", req.method)
            assertEquals("$base/sensor", req.uri)
            assertEquals("Bearer token", req.authorization)
            assertEquals(session, gson.fromJson(req.body, SensorSession::class.java))
        }
    }

    @Test fun savedSetQueryAndProgressAreDecoded() = runBlocking {
        val session = SensorSession(UUID.randomUUID(), 2, 10, 1000, listOf(SensorSample(0, 0f, 0f, 0f)))
        reply(Reply(200, gson.toJson(session)))
        assertEquals(session, success(service.getSensorSession(patient.toString(), 2, 3, 4, 2, "token")))
        assertEquals("$base/sensor?set=2", request().uri)
        reply(Reply(200, """{"completedSets":[1,2]}"""))
        assertEquals(listOf(1, 2), success(service.getSensorProgress(patient.toString(), 2, 3, 4, "token")).completedSets)
        assertEquals("$base/sensor/progress", request().uri)
    }

    @Test fun prescriptionOverrideAndResetUseSeparateEndpoints() = runBlocking {
        val custom = profile.copy(raiseThreshold = 45f, holdTimeMs = 5000)
        reply(Reply(200, gson.toJson(custom)))
        assertEquals(custom, success(service.saveSensorProfile(patient.toString(), 2, 3, 4, custom, "token")))
        val req = request()
        assertEquals("$base/sensor/profile", req.uri)
        assertEquals(custom, gson.fromJson(req.body, SensorProfile::class.java))
        reply(Reply(200, gson.toJson(profile)))
        assertEquals(profile, success(service.resetSensorProfile(patient.toString(), 2, 3, 4, "token")))
        assertEquals("$base/sensor/profile/defaults", request().uri)
    }

    @Test fun jointFilterAndPaginationReachServerAndEmptyCatalogueIsAccepted() = runBlocking {
        reply(Reply(200, """{"exercises":[]}"""))
        assertTrue(success(service.getExercises(10, "KNEE", "token")).exercises.isEmpty())
        assertEquals("/exercises?skip=10&joint=KNEE", request().uri)
    }

    @Test fun unauthorizedProblemResponsePreservesAuthenticationMeaning() = runBlocking {
        reply(Reply(401, """{"title":"Unauthenticated","status":401}""", "application/problem+json"))
        val result = service.getSensorProgress(patient.toString(), 2, 3, 4, "expired")
        assertTrue(result is APIResult.Failure)
        assertTrue(((result as APIResult.Failure).error as ProblemJson).unauthenticatedResponse())
    }

    @Test fun serverErrorsDoNotExposeInternalDetailsToPatient() = runBlocking {
        reply(Reply(500, "database internal details", "text/plain"))
        val result = service.getSensorProgress(patient.toString(), 2, 3, 4, "token") as APIResult.Failure
        assertEquals("Try Again Later", result.error.title)
        assertFalse(result.error.message.orEmpty().contains("database"))
    }

    @Test fun physiotherapistFeedbackAcceptsNoContentResponse() = runBlocking {
        reply(Reply(204, ""))
        success(service.sendFeedbackToExerciseDone(patient.toString(), 2, 3, 4, 1, "Good progress", "token", 4))
        val req = request()
        assertEquals("$base/feedback", req.uri)
        val json = JsonParser.parseString(req.body).asJsonObject
        assertEquals(1, json["set"].asInt)
        assertEquals("Good progress", json["feedback"].asString)
        assertEquals("4", json["feedbackScore"].asString)
    }

    @Test fun malformedSuccessfulPayloadRaisesExplicitTransportError() = runBlocking {
        reply(Reply(200, "{not-json"))
        try {
            service.getSensorProgress(patient.toString(), 2, 3, 4, "token")
            fail("Malformed JSON must not be interpreted as successful progress")
        } catch (expected: UnexpectedResponseException) { /* expected contract */ }
    }
}
