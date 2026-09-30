package pt.ipc.storage.cloudStorageUtils

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.stereotype.Component
import pt.ipc.domain.exceptions.FileDoesNotExists
import java.io.ByteArrayOutputStream
import java.util.*

@Component
class CloudStorageUtilsImpl : CloudStorageUtils {

    private val storage: Storage = CloudStorageConfiguration.storage

    private val userPhotosBucket = CloudStorageConfiguration.userPhotosBucket
    private val physiotherapistCredentialsBucket = CloudStorageConfiguration.physiotherapistCredentialsBucket
    private val patientsVideosBucket = CloudStorageConfiguration.patientsVideosBucket
    private val exercisesPreviewsBucket = CloudStorageConfiguration.exercisesPreviewsBucket

    private val videoContentType = "video/mp4"
    private val pdfContentType = "application/pdf"
    private val pngContentType = "image/png"

    private fun upload(fileName: UUID, content: ByteArray, contentType: String, bucketName: String) {
        val blobId = BlobId.of(bucketName, fileName.toString())

        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .build()

        storage.create(blobInfo, content)
    }

    private fun download(fileName: UUID, bucketName: String): ByteArray {
        val blob = storage.get(bucketName, fileName.toString()) ?: throw FileDoesNotExists

        val outputStream = ByteArrayOutputStream()

        blob.downloadTo(outputStream)
        outputStream.close()

        return outputStream.toByteArray()
    }

    private fun deleteFile(bucketName: String, fileName: String) {
        BlobId.of(bucketName, fileName)?.let { storage.delete(it) }
    }

    override fun uploadPatientVideo(fileName: UUID, video: ByteArray) =
        upload(fileName = fileName, content = video, contentType = videoContentType, bucketName = patientsVideosBucket)

    override fun downloadPatientVideo(fileName: UUID): ByteArray =
        download(fileName = fileName, bucketName = patientsVideosBucket)

    override fun uploadPhysiotherapistCredentials(fileName: UUID, file: ByteArray) =
        upload(fileName = fileName, content = file, contentType = pdfContentType, bucketName = physiotherapistCredentialsBucket)

    override fun downloadPhysiotherapistCredentials(fileName: UUID): ByteArray =
        download(fileName = fileName, bucketName = physiotherapistCredentialsBucket)

    override fun downloadExampleVideo(exerciseID: UUID): ByteArray =
        download(fileName = exerciseID, bucketName = exercisesPreviewsBucket)

    override fun deleteWithID(fileName: UUID) {
        val buckets = listOf(userPhotosBucket, physiotherapistCredentialsBucket, patientsVideosBucket)

        for (bucketName in buckets) {
            val blobToDelete: BlobId? = storage.list(bucketName)
                .iterateAll()
                .find { blob -> blob.name == fileName.toString() }
                ?.let { BlobId.of(bucketName, it.name) }

            if (blobToDelete != null) storage.delete(blobToDelete)
        }
    }

    override fun uploadProfilePicture(fileName: UUID, file: ByteArray) =
        upload(fileName = fileName, content = file, contentType = pngContentType, bucketName = userPhotosBucket)

    override fun downloadProfilePicture(fileName: UUID): ByteArray =
        download(fileName = fileName, bucketName = userPhotosBucket)

    override fun uploadVideoPreview(fileName: UUID, file: ByteArray) =
        upload(fileName = fileName, file, contentType = videoContentType, exercisesPreviewsBucket)

    override fun getAllCredentialsIDs(): List<UUID> =
        storage.list(physiotherapistCredentialsBucket).iterateAll().toList().map { UUID.fromString(it.name) }

    override fun deleteCredential(fileName: UUID) {
        deleteFile(bucketName = physiotherapistCredentialsBucket, fileName = fileName.toString())
    }

    override fun getPatientsVideosIDs(): List<UUID> =
        storage.list(patientsVideosBucket).iterateAll().toList().map { UUID.fromString(it.name) }

    override fun deletePatientVideo(fileName: UUID) =
        deleteFile(bucketName = patientsVideosBucket, fileName = fileName.toString())

    override fun getUserPhotosIDs(): List<UUID> =
        storage.list(userPhotosBucket).iterateAll().toList().map { UUID.fromString(it.name) }

    override fun deleteUserPicture(fileName: UUID) {
        deleteFile(bucketName = userPhotosBucket, fileName = fileName.toString())
    }

    override fun getVideoPreviewsIDs(): List<UUID> =
        storage.list(exercisesPreviewsBucket).iterateAll().toList().map { UUID.fromString(it.name) }

    override fun deleteVideoPreview(fileName: UUID) {
        deleteFile(bucketName = exercisesPreviewsBucket, fileName = fileName.toString())
    }
}
