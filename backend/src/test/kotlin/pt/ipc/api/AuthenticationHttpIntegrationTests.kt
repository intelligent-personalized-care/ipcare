package pt.ipc.api

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import pt.ipc.domain.Role
import pt.ipc.domain.User
import pt.ipc.domain.encryption.*
import pt.ipc.domain.exceptions.PhysiotherapistNotVerified
import pt.ipc.domain.jwt.*
import pt.ipc.http.controllers.physiotherapists.PhysiotherapistsController
import pt.ipc.http.pipeline.authentication.*
import pt.ipc.http.pipeline.exceptionHandler.ExceptionHandler
import pt.ipc.http.utils.SseEmitterRepository
import pt.ipc.services.ServiceUtils
import pt.ipc.services.physiotherapistService.PhysiotherapistService
import java.util.UUID

class AuthenticationHttpIntegrationTests {
    private val users = mock<ServiceUtils>()
    private val physiotherapists = mock<PhysiotherapistService>()
    private val jwt = JwtUtils(JwtConfiguration("http-integration-test-key-".repeat(4)))
    private val encryption = EncryptionUtils(EncryptionUtilsConfiguration("0123456789abcdef"))
    private val id = UUID.randomUUID()
    private val session = UUID.randomUUID()
    private lateinit var mvc: MockMvc
    private val path get() = "/users/physiotherapists/$id/patients"

    @BeforeEach fun setup() {
        mvc = MockMvcBuilders.standaloneSetup(PhysiotherapistsController(physiotherapists, SseEmitterRepository()))
            .addInterceptors(AuthenticationInterceptor(AuthorizationHeaderProcessor(users, jwt, encryption)))
            .setCustomArgumentResolvers(UserArgumentResolver())
            .setControllerAdvice(ExceptionHandler()).build()
    }

    private fun token(role: Role): String {
        whenever(users.getUser(id, role, encryption.encrypt(session.toString())))
            .thenReturn(User(id, "Test", "test@example.test", "unused"))
        return jwt.createAccessToken(id, role, session)
    }

    @Test fun `missing authentication returns problem JSON without invoking service`() {
        mvc.perform(get(path)).andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(401))
        verifyNoInteractions(physiotherapists)
    }

    @Test fun `patient cannot call physiotherapist action`() {
        mvc.perform(get(path).header("Authorization", "Bearer ${token(Role.PATIENT)}"))
            .andExpect(status().isForbidden)
        verifyNoInteractions(physiotherapists)
    }

    @Test fun `verified physiotherapist receives own patient list`() {
        whenever(physiotherapists.getPatientsOfPhysiotherapist(id)).thenReturn(emptyList())
        mvc.perform(get(path).header("Authorization", "Bearer ${token(Role.PHYSIOTHERAPIST)}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.patients").isEmpty)
        verify(users).checkIfPhysiotherapistIsVerified(id)
        verify(physiotherapists).getPatientsOfPhysiotherapist(id)
    }

    @Test fun `valid role cannot access another physiotherapist patient list`() {
        mvc.perform(get("/users/physiotherapists/${UUID.randomUUID()}/patients")
            .header("Authorization", "Bearer ${token(Role.PHYSIOTHERAPIST)}"))
            .andExpect(status().isForbidden)
        verifyNoInteractions(physiotherapists)
    }

    @Test fun `revoked session rejects otherwise valid token`() {
        val access = jwt.createAccessToken(id, Role.PATIENT, session)
        mvc.perform(get(path).header("Authorization", "Bearer $access"))
            .andExpect(status().isUnauthorized)
        verifyNoInteractions(physiotherapists)
    }

    @Test fun `refresh token cannot be used as bearer access token`() {
        mvc.perform(get(path).header("Authorization", "Bearer ${jwt.createRefreshToken(session)}"))
            .andExpect(status().isUnauthorized)
        verifyNoInteractions(physiotherapists)
    }

    @Test fun `unverified physiotherapist is rejected before accessing patients`() {
        val access = token(Role.PHYSIOTHERAPIST)
        doAnswer { throw PhysiotherapistNotVerified }.whenever(users).checkIfPhysiotherapistIsVerified(id)
        mvc.perform(get(path).header("Authorization", "Bearer $access"))
            .andExpect(status().isForbidden)
        verifyNoInteractions(physiotherapists)
    }
}
