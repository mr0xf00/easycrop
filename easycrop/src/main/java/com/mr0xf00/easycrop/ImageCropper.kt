package com.mr0xf00.easycrop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize
import com.mr0xf00.easycrop.images.ImageBitmapSrc
import com.mr0xf00.easycrop.images.ImageSrc
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
/** Union type denoting the possible results after a crop operation is done */
public sealed interface CropResult {
    /** The final result as an ImageBitmap.
     * use [asAndroidBitmap] if you need an [android.graphics.Bitmap].
     */
    data class Success(val bitmap: ImageBitmap) : CropResult

    /** The user has cancelled the operation or another session was started. */
    object Cancelled : CropResult
}

public enum class CropError : CropResult {
    /** The supplied image is invalid, not supported by the codec
     * or you don't have the required permissions to read it */
    LoadingError,
    /** The result could not be saved. Try reducing the maxSize supplied to [ImageCropper.crop] */
    SavingError
}

public enum class CropperLoading {
    /** The image is being prepared. */
    PreparingImage,

    /** The user has accepted the cropped image and the result is being saved. */
    SavingResult,
}

internal val DefaultMaxCropSize = IntSize(3000, 3000)

/**
 * State holder for the image cropper.
 * Allows starting new crop sessions as well as getting the state of the pending crop.
 */
@Stable
public interface ImageCropper {
    /** The pending crop state, if any */
    val cropState: CropState?

    val loadingStatus: CropperLoading?

    /**
     * Initiates a new crop session, cancelling the current one, if any.
     * Suspends until a result is available (cancellation, error, success) and returns it.
     * The resulting image will be scaled down to fit [maxResultSize] (if provided).
     * [createSrc] will be used to construct an [ImageSrc] instance.
     */
    suspend fun crop(
        maxResultSize: IntSize? = DefaultMaxCropSize,
        createSrc: suspend () -> ImageSrc?
    ): CropResult
}

/**
 * Initiates a new crop session, cancelling the current one, if any.
 * Suspends until a result is available (cancellation, error, success) and returns it.
 * The resulting image will be scaled down to fit [maxResultSize] if provided.
 * [bmp] will be used as a source.
 */
public suspend fun ImageCropper.crop(
    maxResultSize: IntSize? = DefaultMaxCropSize,
    bmp: ImageBitmap
): CropResult = crop(maxResultSize = maxResultSize) {
    ImageBitmapSrc(bmp)
}

@Composable
public fun rememberImageCropper() : ImageCropper {
    return remember { ImageCropper() }
}

/**
 * Creates an [ImageCropper] instance.
 */
public fun ImageCropper(): ImageCropper = object : ImageCropper {
    override var cropState: CropState? by mutableStateOf(null)
    private val cropStateFlow = snapshotFlow { cropState }
    override var loadingStatus: CropperLoading? by mutableStateOf(null)
    override suspend fun crop(
        maxResultSize: IntSize?,
        createSrc: suspend () -> ImageSrc?
    ): CropResult {
        cropState = null
        val src = withLoading(CropperLoading.PreparingImage) { createSrc() }
            ?: return CropError.LoadingError
        val newCrop = CropState(src) { cropState = null }
        cropState = newCrop
        cropStateFlow.takeWhile { it === newCrop }.collect()
        if (!newCrop.accepted) return CropResult.Cancelled
        return withLoading(CropperLoading.SavingResult) {
            val result = newCrop.createResult(maxResultSize)
            if (result == null) CropError.SavingError
            else CropResult.Success(result)
        }
    }

    inline fun <R> withLoading(status: CropperLoading, op: () -> R): R {
        return try {
            loadingStatus = status
            op()
        } finally {
            loadingStatus = null
        }
    }
}