package com.example.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

sealed interface UploadState {
    data object Idle : UploadState
    data class Uploading(val progressPercent: Int) : UploadState
    data class Success(val downloadUrl: String) : UploadState
    data class Error(val message: String) : UploadState
}

/**
 * Handles image uploads cleanly across both tiers:
 * 1. If a Firebase Cloud Storage bucket is active on the project, uploads full-quality compressed JPEG to Cloud Storage.
 * 2. If Cloud Storage is not enabled on the free Firebase tier (HTTP 404), seamlessly compresses the photo into
 *    a compact inline JPEG Data URI (<= 140 KB) stored directly in the free Cloud Firestore database so uploads
 *    always succeed without requiring Blaze billing.
 */
class StorageRepository(private val context: Context) {

    private val primaryStorage by lazy { Firebase.storage }
    private val auth by lazy { Firebase.auth }

    companion object {
        private const val TAG = "StorageRepository"
        private const val MAX_RAW_BYTES = 5 * 1024 * 1024L // 5 MB input limit
        private const val MAX_DIM_PX = 1600
        private const val MAX_INLINE_DATA_URI_CHARS = 140_000 // Safe margin under 200K rules limit and 1 MiB Firestore doc cap
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/*"
        )

        /**
         * Converts a stored `data:image/...;base64,...` URI into raw ByteArray for Coil 2.x AsyncImage,
         * or returns normal https:// / content:// URLs unchanged.
         */
        fun decodeImageModel(source: String): Any {
            if (source.startsWith("data:image/", ignoreCase = true)) {
                val commaIdx = source.indexOf(',')
                if (commaIdx != -1 && commaIdx + 1 < source.length) {
                    return try {
                        Base64.decode(source.substring(commaIdx + 1), Base64.DEFAULT)
                    } catch (_: IllegalArgumentException) {
                        source
                    }
                }
            }
            return source
        }
    }

    fun uploadImage(folder: String, imageUri: Uri): Flow<UploadState> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(UploadState.Error("Authentication required before uploading images."))
            close()
            return@callbackFlow
        }

        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        if (mimeType !in ALLOWED_MIME_TYPES && !mimeType.startsWith("image/")) {
            trySend(UploadState.Error("Unsupported file type ($mimeType). Please choose a JPG, PNG, or WebP image."))
            close()
            return@callbackFlow
        }

        trySend(UploadState.Uploading(10))

        val rawBytes = try {
            withContext(Dispatchers.IO) {
                readValidatedImageBytes(imageUri)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            trySend(UploadState.Error(e.message ?: "Invalid image file."))
            close()
            return@callbackFlow
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read image", e)
            trySend(UploadState.Error("Could not read selected image. Please choose another photo."))
            close()
            return@callbackFlow
        }

        trySend(UploadState.Uploading(25))

        val projectId = primaryStorage.app.options.projectId
        val primaryBucket = primaryStorage.reference.bucket
        val targetStorage: FirebaseStorage? = withContext(Dispatchers.IO) {
            resolveActiveBucket(primaryBucket, projectId)
        }

        if (targetStorage == null) {
            // Free Spark plan / unactivated Cloud Storage bucket: store compact JPEG cleanly inside Firestore
            Log.i(TAG, "Cloud Storage bucket not enabled; encoding compact inline JPEG for Firestore.")
            trySend(UploadState.Uploading(70))
            val inlineResult = withContext(Dispatchers.IO) {
                encodeInlineDataUri(rawBytes)
            }
            if (inlineResult != null) {
                trySend(UploadState.Success(inlineResult))
            } else {
                trySend(UploadState.Error("Could not compress image for database storage. Please try another photo."))
            }
            close()
            return@callbackFlow
        }

        val compressedBytes = withContext(Dispatchers.IO) {
            compressImage(rawBytes, MAX_DIM_PX, 82)
        }
        if (compressedBytes == null) {
            trySend(UploadState.Error("Selected file is not a valid image."))
            close()
            return@callbackFlow
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val objectPath = "$folder/$uid/$fileName"
        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }

        val task: UploadTask = targetStorage.reference.child(objectPath).putBytes(compressedBytes, metadata)

        task.addOnProgressListener { snap ->
            val total = snap.totalByteCount.coerceAtLeast(1L)
            val pct = ((snap.bytesTransferred * 95L) / total).toInt().coerceIn(25, 95)
            trySend(UploadState.Uploading(pct))
        }.addOnSuccessListener {
            targetStorage.reference.child(objectPath).downloadUrl
                .addOnSuccessListener { uri ->
                    trySend(UploadState.Success(uri.toString()))
                    close()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to obtain download URL", e)
                    trySend(UploadState.Error(formatStorageError(e)))
                    close()
                }
        }.addOnFailureListener { e ->
            val code = (e as? StorageException)?.errorCode
            if (code == StorageException.ERROR_BUCKET_NOT_FOUND ||
                code == StorageException.ERROR_OBJECT_NOT_FOUND ||
                code == StorageException.ERROR_PROJECT_NOT_FOUND
            ) {
                Log.i(TAG, "Cloud Storage unavailable ($code); falling back to compact Firestore inline image.")
                val inlineResult = encodeInlineDataUri(rawBytes)
                if (inlineResult != null) {
                    trySend(UploadState.Success(inlineResult))
                } else {
                    trySend(UploadState.Error(formatStorageError(e)))
                }
            } else {
                Log.w(TAG, "Firebase Storage upload failed", e)
                trySend(UploadState.Error(formatStorageError(e)))
            }
            close()
        }

        awaitClose {
            if (!task.isComplete) {
                task.cancel()
            }
        }
    }

    private fun readValidatedImageBytes(uri: Uri): ByteArray {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Unable to open image file.")
        if (rawBytes.size > MAX_RAW_BYTES) {
            throw IllegalArgumentException("Image exceeds 5 MB size limit. Please choose a smaller photo.")
        }
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
            throw IllegalArgumentException("Selected file is not a valid image.")
        }
        return rawBytes
    }

    private fun encodeInlineDataUri(rawBytes: ByteArray): String? {
        val steps = listOf(
            640 to 70,
            520 to 60,
            420 to 50,
            320 to 42,
            240 to 34
        )
        for ((maxDim, quality) in steps) {
            val jpeg = compressImage(rawBytes, maxDim, quality) ?: return null
            val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
            if (dataUri.length <= MAX_INLINE_DATA_URI_CHARS) {
                return dataUri
            }
        }
        return null
    }

    /**
     * Checks if the primary bucket (or legacy .appspot.com bucket) exists in Firebase Storage
     * before launching UploadTask, avoiding noisy internal UploadTask 404 exceptions when Cloud
     * Storage is not enabled on the project.
     */
    private fun resolveActiveBucket(primaryBucket: String, projectId: String?): FirebaseStorage? {
        val candidates = buildList {
            if (primaryBucket.isNotBlank()) add(primaryBucket)
            if (!projectId.isNullOrBlank()) {
                val legacy = "$projectId.appspot.com"
                if (!legacy.equals(primaryBucket, ignoreCase = true)) add(legacy)
            }
        }
        for (bucket in candidates) {
            val status = probeBucketStatus(bucket)
            if (status == null || status != 404) {
                return if (bucket == primaryBucket) {
                    primaryStorage
                } else {
                    FirebaseStorage.getInstance(primaryStorage.app, "gs://$bucket")
                }
            }
        }
        return null
    }

    private fun probeBucketStatus(bucket: String): Int? {
        return try {
            val encodedBucket = URLEncoder.encode(bucket, "UTF-8")
            val url = URL("https://firebasestorage.googleapis.com/v0/b/$encodedBucket/o?maxResults=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
            }
            try {
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBitmapForAi(imageUri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to decode bitmap for AI analysis", e)
            null
        }
    }

    private fun compressImage(rawBytes: ByteArray, maxDimPx: Int, quality: Int): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null

        val scale = minOf(
            1f,
            maxDimPx.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        return try {
            ByteArrayOutputStream().use { baos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                baos.toByteArray()
            }
        } finally {
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }
    }

    private fun formatStorageError(e: Exception): String {
        val code = (e as? StorageException)?.errorCode
        return when (code) {
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED ->
                "Upload permission denied. Verify you are signed in with Google."
            StorageException.ERROR_QUOTA_EXCEEDED ->
                "Firebase Storage quota exceeded on your current plan."
            else -> e.localizedMessage ?: "Image upload failed. Please check your connection and try again."
        }
    }
}
