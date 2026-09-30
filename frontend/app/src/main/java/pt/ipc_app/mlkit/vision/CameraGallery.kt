package pt.ipc_app.mlkit.vision

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object CameraGallery {
    fun permitted(context: Context) = Build.VERSION.SDK_INT >= 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    suspend fun save(context: Context, file: File) = withContext(Dispatchers.IO) {
        check(permitted(context))
        val preferences = context.getSharedPreferences("camera_gallery", Context.MODE_PRIVATE)
        if (preferences.getBoolean(file.name, false)) return@withContext
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "IPC_${file.name}")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/IPC")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Cannot create Gallery entry")
            try {
                val output = resolver.openOutputStream(uri) ?: throw IOException("Cannot open Gallery entry")
                output.use { target -> file.inputStream().use { it.copyTo(target) } }
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "IPC")
            check(directory.isDirectory || directory.mkdirs())
            val target = File(directory, "IPC_${file.name}")
            try { file.copyTo(target, overwrite = true) } catch (e: Exception) { target.delete(); throw e }
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
        }
        check(preferences.edit().putBoolean(file.name, true).commit())
    }
}
