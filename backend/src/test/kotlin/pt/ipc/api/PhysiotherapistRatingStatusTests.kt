package pt.ipc.api

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import pt.ipc.domain.encryption.*
import pt.ipc.domain.jwt.*
import pt.ipc.domain.physiotherapist.*
import pt.ipc.services.ServiceUtils
import pt.ipc.services.patientService.PatientsServiceImpl
import pt.ipc.storage.repositories.*
import pt.ipc.storage.transaction.*
import java.util.UUID

class PhysiotherapistRatingStatusTests {
    @Test fun `rating eligibility is specific to the patient and persists when details are reloaded`() {
        val patient = UUID.randomUUID()
        val otherPatient = UUID.randomUUID()
        val professional = UUID.randomUUID()
        val patients = mock<PatientsRepository>()
        val professionals = mock<PhysiotherapistRepository>()
        val transaction = mock<Transaction>()
        whenever(transaction.patientsRepository).thenReturn(patients)
        whenever(transaction.physiotherapistRepository).thenReturn(professionals)
        val manager = object : TransactionManager {
            override fun <R> run(fileName: UUID?, block: (Transaction) -> R): R = block(transaction)
        }
        whenever(professionals.getPhysiotherapistOfPatient(any())).thenReturn(PhysiotherapistDetails(professional, "Test", "test@example.test"))
        whenever(professionals.getPhysiotherapistRating(professional)).thenReturn(Rating(4f, 2))
        whenever(professionals.isPhysiotherapistOfPatient(professional, patient)).thenReturn(true)
        var rated = false
        whenever(patients.hasPatientRatedPhysiotherapist(patient, professional)).thenAnswer { rated }
        doAnswer { rated = true; null }.whenever(patients).ratePhysiotherapist(patient, professional, 5)
        val service = PatientsServiceImpl(manager,
            EncryptionUtils(EncryptionUtilsConfiguration("0123456789abcdef")),
            ServiceUtils(manager, JwtUtils(JwtConfiguration("a".repeat(64)))))
        assertFalse(service.getPhysiotherapistOfPatient(patient).hasRated)
        service.ratePhysiotherapist(professional, patient, 5)
        assertTrue(service.getPhysiotherapistOfPatient(patient).hasRated)
        assertFalse(service.getPhysiotherapistOfPatient(otherPatient).hasRated)
        assertThrows(pt.ipc.domain.exceptions.AlreadyRatedThisPhysiotherapist::class.java) {
            service.ratePhysiotherapist(professional, patient, 5)
        }
        verify(patients, times(1)).ratePhysiotherapist(patient, professional, 5)
    }
}
