package pt.ipc.api

import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersTests {
/*
    @TestConfiguration
    @Component
    class TestConfig {
        @Bean
        @Primary
        fun testJdbi(): Jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                setURL(System.getenv("postgresql_database_tests"))
            }
        ).configure()
    }

    @LocalServerPort
    var port: Int = 0

    private fun registerPatientInput(): RegisterPatientInput {
        val uuid = UUID.randomUUID()
        return RegisterPatientInput(name = uuid.toString(), email = "$uuid@gmail.com", password = "@Password12")
    }

    private fun registerInput(): RegisterInput {
        val uuid = UUID.randomUUID()
        return RegisterInput(name = uuid.toString(), email = "$uuid@gmail.com", password = "@Password12")
    }

    @Test
    fun `Create Patient`() {
        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        val registerPatientInput = RegisterPatientInput(
            name = "Test123",
            email = UUID.randomUUID().toString() + "@gmail.com",
            password = "@Password1"
        )

        httpClient
            .post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(
                registerPatientInput
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(CredentialsOutput::class.java)
    }

    @Test
    fun `Bad Email`() {
        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        val registerPatientInput = RegisterPatientInput(
            name = "Test",
            email = "bad email",
            password = "@Password1"
        )

        val result = httpClient
            .post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(
                registerPatientInput
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(Problem::class.java)
            .returnResult().responseBody

        assertEquals(result?.title, "Bad Email")
    }

    @Test
    fun `Bad Password`() {
        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        val uuid = UUID.randomUUID()

        val registerPatientInput = RegisterPatientInput(
            name = uuid.toString(),
            email = "$uuid@gmail.com",
            password = "bad password"
        )

        httpClient
            .post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(
                registerPatientInput
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(Problem::class.java) // Specify the expected response body type
    }

    @Test
    fun `create Same User`() {
        val registerPatientInput = registerPatientInput()

        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        httpClient.post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(registerPatientInput)
            .exchange()
            .expectStatus().isCreated
            .expectBody(CredentialsOutput::class.java)

        httpClient.post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(registerPatientInput)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(Problem::class.java)
    }

    @Test
    fun `create Physiotherapist`() {
        val registerInput = registerInput()

        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        httpClient.post()
            .uri(Uris.PHYSIOTHERAPISTS)
            .bodyValue(registerInput)
            .exchange()
            .expectStatus().isCreated
            .expectBody(CredentialsOutput::class.java)
    }

    @Test
    fun `create Same Physiotherapist`() {
        val registerInput = registerInput()

        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        httpClient.post()
            .uri(Uris.PHYSIOTHERAPISTS)
            .bodyValue(registerInput)
            .exchange()
            .expectStatus().isCreated
            .expectBody(CredentialsOutput::class.java)

        httpClient.post()
            .uri(Uris.PHYSIOTHERAPISTS)
            .bodyValue(registerInput)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(Problem::class.java)
    }

    @Test
    fun `Try Operation Without being verified`() {
        val registerInput = registerInput()

        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        val credentialsOutput =
            httpClient.post()
                .uri(Uris.PHYSIOTHERAPISTS)
                .bodyValue(registerInput)
                .exchange()
                .expectStatus().isCreated
                .expectBody(CredentialsOutput::class.java)
                .returnResult()
                .responseBody!!

        val uri = UriComponentsBuilder.fromPath(Uris.PATIENTS_OF_PHYSIOTHERAPIST)
            .buildAndExpand(credentialsOutput.id).toUriString()

        httpClient.post()
            .uri(uri)
            .bodyValue(registerInput)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${credentialsOutput.accessToken}")
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody(Problem::class.java)
    }

    @Test
    fun `Try Requesting physiotherapist and accepting`() {
        val registerPatientInput = registerPatientInput()

        val httpClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        httpClient.post()
            .uri(Uris.PATIENT_REGISTER)
            .bodyValue(registerPatientInput)
            .exchange()
            .expectStatus().isCreated
            .expectBody(CredentialsOutput::class.java)
            .returnResult()
            .responseBody!!
    }

 */
}
