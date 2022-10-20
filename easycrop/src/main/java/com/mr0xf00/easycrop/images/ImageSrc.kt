package com.mr0xf00.easycrop.images

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.compose.runtime.Stable

@Stable
public interface ImageSrc {
    public val size: IntSize
    public suspend fun open(params: DecodeParams): DecodeResult?
}

internal data class ImageBitmapSrc(private val data: ImageBitmap) : ImageSrc {
    override val size: IntSize = IntSize(data.width, data.height)
    private val resultParams = DecodeParams(1, size.toIntRect())
    override suspend fun open(params: DecodeParams) = DecodeResult(resultParams, data)
}