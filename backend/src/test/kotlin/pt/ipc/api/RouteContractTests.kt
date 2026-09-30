package pt.ipc.api

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import pt.ipc.http.controllers.physiotherapists.PhysiotherapistsController
import pt.ipc.http.utils.SseEmitterRepository
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.physiotherapistService.PhysiotherapistService
import java.util.UUID

class RouteContractTests {
    @Test fun `patient registration uses the patient route and role`() {
        val service = mock<pt.ipc.services.patientService.PatientsService>()
        whenever(service.registerPatient(any())).thenReturn(CredentialsOutput(UUID.randomUUID(), "access", "refresh"))
        val mvc = MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.patients.PatientsController(service, SseEmitterRepository())
        ).build()
        mvc.perform(post("/users/patients").contentType(MediaType.APPLICATION_JSON)
            .content("""{"name":"Patient test","email":"patient@example.test","password":"@Password1"}"""))
            .andExpect(status().isCreated)
        org.junit.jupiter.api.Assertions.assertTrue(pt.ipc.domain.Role.PATIENT.isPatient())
    }
    @Test fun `physiotherapist registration uses the canonical route`() {
        val service = mock<PhysiotherapistService>()
        whenever(service.registerPhysiotherapist(any())).thenReturn(CredentialsOutput(UUID.randomUUID(), "access", "refresh"))
        val mvc = MockMvcBuilders.standaloneSetup(PhysiotherapistsController(service, SseEmitterRepository())).build()
        listOf("/users/physiotherapists").forEach { route ->
            mvc.perform(
                post(route).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Physiotherapist test","email":"physiotherapist@example.test","password":"@Password1"}""")
            )
                .andExpect(status().isCreated)
        }
    }

    @Test fun `no physiotherapists returns HTTP 200 with an empty array`() {
        val patient = UUID.randomUUID()
        val service = mock<pt.ipc.services.patientService.PatientsService>()
        whenever(service.searchPhysiotherapistsAvailable(patient, null, 0, 10)).thenReturn(emptyList())
        val mvc = MockMvcBuilders.standaloneSetup(
            pt.ipc.http.controllers.patients.PatientsController(service, SseEmitterRepository())
        ).setCustomArgumentResolvers(pt.ipc.http.pipeline.authentication.UserArgumentResolver()).build()
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/physiotherapists").with { req ->
            pt.ipc.http.pipeline.authentication.UserArgumentResolver.addUserTo(
                pt.ipc.domain.User(patient, "Patient", "patient@example.test", "test"), req)
            req
        }).andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("""{"physiotherapists":[]}""", true))
    }
}
