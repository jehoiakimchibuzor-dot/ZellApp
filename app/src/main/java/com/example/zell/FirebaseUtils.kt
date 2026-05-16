package com.example.zell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

object FirebaseUtils {

    // Max file size we allow — 10MB. Anything bigger gets rejected before upload.
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024

    /**
     * Uploads an image to Firebase Storage with automatic compression.
     *
     * Handles both URI types Android can give you:
     *  - content:// → standard photo picker, camera, Google Photos
     *  - file://     → file managers, some cloud apps, older Android versions
     *
     * Throws the original exception if upload fails so RetryHelper and ErrorHandler
     * can process it properly.
     */
    suspend fun uploadImage(context: Context, uri: Uri, folder: String): String =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "${UUID.randomUUID()}.jpg"
                val storage = FirebaseStorage.getInstance()
                val ref = storage.reference.child(folder).child(fileName)

                // Step 1: Open the file — handle both content:// and file:// URIs
                val inputStream = openInputStream(context, uri)
                    ?: throw Exception("Could not open the selected file. Please try a different image.")

                // Step 2: Decode the bitmap
                val originalBitmap = inputStream.use { BitmapFactory.decodeStream(it) }
                    ?: throw Exception("Could not read the image. The file might be corrupted or in an unsupported format.")

                // Step 3: Compress to JPEG at 70% quality
                val outputStream = ByteArrayOutputStream()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val data = outputStream.toByteArray()

                // Step 4: Reject files that are still too large after compression
                if (data.size > MAX_FILE_SIZE_BYTES) {
                    throw Exception("Image is too large (max 10MB). Please choose a smaller image.")
                }

                CrashlyticsLogger.i("FirebaseUtils", "Uploading to ${ref.path} — ${data.size / 1024}KB")

                // Step 5: Upload and get the public download URL
                ref.putBytes(data).await()
                val url = ref.downloadUrl.await().toString()

                CrashlyticsLogger.i("FirebaseUtils", "Upload successful: $url")
                url

            } catch (e: Exception) {
                CrashlyticsLogger.e("FirebaseUtils", "Image upload failed in folder: $folder", e)
                throw e // let RetryHelper / ErrorHandler deal with it
            }
        }

    /**
     * Uploads any raw file (audio, video, documents) directly to Firebase Storage
     * without any re-encoding. Used for voice notes, video clips, etc.
     *
     * @param file     The local file to upload
     * @param folder   Destination folder in Storage (e.g. "chat_media/convoId")
     * @param mimeType MIME type for Content-Type metadata (e.g. "audio/mp4")
     * @return The public download URL
     */
    suspend fun uploadFile(file: File, folder: String, mimeType: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) throw Exception("Recording file not found. Please try again.")
                if (file.length() > 50 * 1024 * 1024) throw Exception("File is too large (max 50MB).")

                val extension = file.extension.ifBlank { "bin" }
                val fileName  = "${UUID.randomUUID()}.$extension"
                val storage   = FirebaseStorage.getInstance()
                val ref       = storage.reference.child(folder).child(fileName)
                val metadata  = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType(mimeType)
                    .build()

                CrashlyticsLogger.i("FirebaseUtils", "Uploading file to ${ref.path} — ${file.length() / 1024}KB")

                ref.putFile(android.net.Uri.fromFile(file), metadata).await()
                val url = ref.downloadUrl.await().toString()

                CrashlyticsLogger.i("FirebaseUtils", "File upload successful: $url")
                url
            } catch (e: Exception) {
                CrashlyticsLogger.e("FirebaseUtils", "File upload failed in folder: $folder", e)
                throw e
            }
        }

    /**
     * Opens an InputStream from a URI regardless of its scheme.
     *
     * Why this matters:
     * - content:// URIs come from the Android photo picker, camera, Google Photos
     * - file:// URIs come from file managers, some cloud apps, older Android versions
     * - Without this, file:// URIs would return null and silently fail
     */
    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return when (uri.scheme?.lowercase()) {
            "content" -> {
                // Standard Android URI — use ContentResolver
                context.contentResolver.openInputStream(uri)
            }
            "file" -> {
                // Direct file path URI — open directly from the filesystem
                uri.path?.let { path -> File(path).inputStream() }
            }
            else -> {
                // Fallback: try ContentResolver for anything else (http, etc.)
                CrashlyticsLogger.w("FirebaseUtils", "Unknown URI scheme: ${uri.scheme}, trying ContentResolver")
                runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            }
        }
    }
}
