package pt.ipc.storage.cloudStorageUtils

import java.util.*

interface CloudStorageUtils {

    fun uploadPatientVideo(fileName: UUID, video: ByteArray)

    fun downloadPatientVideo(fileName: UUID): ByteArray

    fun uploadPhysiotherapistCredentials(fileName: UUID, file: ByteArray)

    fun downloadPhysiotherapistCredentials(fileName: UUID): ByteArray

    fun downloadExampleVideo(exerciseID: UUID): ByteArray

    fun deleteWithID(fileName: UUID)

    fun uploadProfilePicture(fileName: UUID, file: ByteArray)

    fun downloadProfilePicture(fileName: UUID): ByteArray

    fun uploadVideoPreview(fileName: UUID, file: ByteArray)

    fun getAllCredentialsIDs(): List<UUID>

    fun deleteCredential(fileName: UUID)

    fun getPatientsVideosIDs(): List<UUID>

    fun deletePatientVideo(fileName: UUID)

    fun getUserPhotosIDs(): List<UUID>

    fun deleteUserPicture(fileName: UUID)

    fun getVideoPreviewsIDs(): List<UUID>

    fun deleteVideoPreview(fileName: UUID)
}
