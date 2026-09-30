package pt.ipc.services.adminService

import pt.ipc.domain.exercises.ExerciseType
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistInfo
import pt.ipc.services.dtos.RegisterInput
import java.util.UUID

interface AdminService {

    fun createAdminAccount(registerInput: RegisterInput): CredentialsOutput

    fun getUnverifiedPhysiotherapists(): List<PhysiotherapistInfo>

    fun getCredentialOfPhysiotherapist(physiotherapistID: UUID): ByteArray

    fun decidePhysiotherapistCredential(physiotherapistID: UUID, accept: Boolean)

    fun addExerciseInfoPreview(title: String, description: String, type: ExerciseType, video: ByteArray)
}
