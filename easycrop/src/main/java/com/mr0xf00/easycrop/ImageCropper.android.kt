package com.mr0xf00.easycrop

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import com.mr0xf00.easycrop.images.ImageSrc
import com.mr0xf00.easycrop.images.toImageSrc
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Initiates a new crop session, cancelling the current one, if any.
 * Suspends until a result is available (cancellation, error, success) and returns it.
 * The resulting image will be scaled down to fit [maxResultSize] if provided.
 * [file] will be used as a source.
 */
public suspend fun ImageCropper.crop(
    file: File, maxResultSize: IntSize? = DefaultMaxCropSize,
): CropResult {
    return crop(maxResultSize) { file.toImageSrc() }
}

/**
 * Initiates a new crop session, cancelling the current one, if any.
 * Suspends until a result is available (cancellation, error, success) and returns it.
 * The resulting image will be scaled down to fit [maxResultSize] if provided.
 * [uri] will be used as a source.
 * Set [cacheBeforeUse] to false if you're certain that reopening it multiple times won't be a problem,
 * true otherwise.
 */
public suspend fun ImageCropper.crop(
    uri: Uri,
    context: Context,
    maxResultSize: IntSize? = DefaultMaxCropSize,
    cacheBeforeUse: Boolean = true
): CropResult = cacheUri(enabled = cacheBeforeUse, uri, context) { cached ->
    crop(maxResultSize) { cached?.toImageSrc(context) }
}

private const val CacheDir = "easycrop_cache"

private suspend fun <R> cacheUri(
    enabled: Boolean, uri: Uri, context: Context,
    block: suspend (Uri?) -> R
): R {
    if (!enabled) return block(uri)
    val dst = context.cacheDir.resolve("$CacheDir/${UUID.randomUUID()}")
    return try {
        val cached = runCatching { copy(uri, dst, context) }.getOrNull()
        block(cached)
    } finally {
        dst.deleteInBackground()
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun File.deleteInBackground() {
    GlobalScope.launch(Dispatchers.IO) { runCatching { delete() } }
}

private suspend fun copy(src: Uri, dst: File, context: Context) = withContext(Dispatchers.IO) {
    dst.parentFile?.mkdirs()
    context.contentResolver.openInputStream(src)!!.use { srcStream ->
        dst.outputStream().use { dstStream ->
            srcStream.copyTo(dstStream)
        }
    }
    Uri.fromFile(dst)
}