package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.R
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StudioPreset(
    val id: String,
    val label: String,
    val uriValue: String,
    val subtitle: String
)

object ImageEncoder {

    val studioPresets: List<StudioPreset> = listOf(
        StudioPreset(
            id = "editorial_nights",
            label = "Neon Editorial",
            uriValue = "preset://auth_hero",
            subtitle = "Golden hour & neon frames"
        ),
        StudioPreset(
            id = "vintage_camera",
            label = "Studio Camera",
            uriValue = "preset://empty_feed",
            subtitle = "Minimalist polaroid art"
        ),
        StudioPreset(
            id = "sunset_horizon",
            label = "Sunset Horizon",
            uriValue = "preset://sunset_horizon",
            subtitle = "Warm coral & amber gradient dusk"
        ),
        StudioPreset(
            id = "cyber_tokyo",
            label = "Midnight Neon",
            uriValue = "preset://cyber_tokyo",
            subtitle = "Electric magenta & indigo glow"
        ),
        StudioPreset(
            id = "coastal_breeze",
            label = "Pacific Coast",
            uriValue = "preset://coastal_breeze",
            subtitle = "Turquoise tide & sunlit mist"
        ),
        StudioPreset(
            id = "desert_dunes",
            label = "Sahara Gold",
            uriValue = "preset://desert_dunes",
            subtitle = "Sculpted amber desert geometry"
        )
    )

    suspend fun encodeUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int = 640,
        jpegQuality: Int = 72
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            require(srcWidth > 0 && srcHeight > 0) { "Unable to decode selected image" }

            var inSampleSize = 1
            while (max(srcWidth, srcHeight) / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: error("Failed to read image stream")

            val scale = minOf(
                1f,
                maxDimension.toFloat() / max(rawBitmap.width, rawBitmap.height).toFloat()
            )
            val scaledBitmap = if (scale < 1f) {
                val targetW = (rawBitmap.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (rawBitmap.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else {
                rawBitmap
            }

            val output = ByteArrayOutputStream()
            var quality = jpegQuality
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)

            // Guarantee payload stays safely within Firestore 350KB string rule bound
            while (output.size() > 210_000 && quality > 35) {
                output.reset()
                quality -= 12
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }

            val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        }
    }

    suspend fun decodeDataUrlToBitmap(dataUrl: String): Bitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex < 0) return@runCatching null
            val base64Part = dataUrl.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun presetDrawableRes(uriValue: String): Int? = when (uriValue) {
        "preset://auth_hero" -> R.drawable.img_auth_hero
        "preset://empty_feed" -> R.drawable.img_empty_feed
        else -> null
    }
}
