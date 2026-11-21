package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.PendingAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaAttachmentHelper {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    suspend fun readAttachmentFromUri(
        context: Context,
        uri: Uri,
        forcedType: String? = null
    ): Result<PendingAttachment> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri).orEmpty()
            var displayName = "attachment"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    if (sizeIdx >= 0) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

            val resolvedType = when {
                forcedType != null -> forcedType
                mime.startsWith("image/") -> "image"
                mime.startsWith("video/") -> "video"
                else -> "file"
            }

            if (resolvedType == "image") {
                val dataUrl = ImageEncoder.encodeUriToDataUrl(
                    context = context,
                    uri = uri,
                    maxDimension = 720,
                    jpegQuality = 72
                ).getOrThrow()
                PendingAttachment(
                    type = "image",
                    name = displayName,
                    sizeBytes = sizeBytes.coerceAtLeast(dataUrl.length.toLong()),
                    data = dataUrl
                )
            } else if (resolvedType == "video") {
                require(sizeBytes <= 50_000_000L) { "Video exceeds 50 MB limit" }
                PendingAttachment(
                    type = "video",
                    name = displayName,
                    sizeBytes = sizeBytes,
                    data = uri.toString()
                )
            } else {
                require(sizeBytes <= 25_000_000L) { "File exceeds 25 MB limit" }
                val rawBytes = resolver.openInputStream(uri)?.use { stream ->
                    val buf = stream.readBytes()
                    buf
                } ?: ByteArray(0)
                val encodedData = if (rawBytes.isNotEmpty() && rawBytes.size <= 240_000) {
                    val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                    "data:${mime.ifBlank { "application/octet-stream" }};base64,$b64"
                } else {
                    uri.toString()
                }
                PendingAttachment(
                    type = "file",
                    name = displayName,
                    sizeBytes = sizeBytes.coerceAtLeast(rawBytes.size.toLong()),
                    data = encodedData
                )
            }
        }
    }
}
