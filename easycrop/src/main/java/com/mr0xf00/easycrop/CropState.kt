package com.mr0xf00.easycrop

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toSize
import com.mr0xf00.easycrop.utils.*
import com.mr0xf00.easycrop.utils.constrainOffset
import com.mr0xf00.easycrop.utils.eq
import com.mr0xf00.easycrop.utils.next90
import com.mr0xf00.easycrop.utils.prev90
import com.mr0xf00.easycrop.images.ImageSrc

/** State for the current image being cropped */
@Stable
public interface CropState {
    public val src: ImageSrc
    public var transform: ImgTransform
    public var region: Rect
    public var aspectLock: Boolean
    public var shape: CropShape
    public val accepted: Boolean
    fun done(accept: Boolean)
    fun reset()
}

internal fun CropState(
    src: ImageSrc,
    onDone: () -> Unit = {},
): CropState = object : CropState {
    val defaultTransform: ImgTransform = ImgTransform.Identity
    val defaultShape: CropShape = RectCropShape
    val defaultAspectLock: Boolean = false
    override val src: ImageSrc get() = src
    private var _transform: ImgTransform by mutableStateOf(defaultTransform)
    override var transform: ImgTransform
        get() = _transform
        set(value) {
            onTransformUpdated(transform, value)
            _transform = value
        }

    val defaultRegion = src.size.toSize().toRect()

    private var _region by mutableStateOf(defaultRegion)
    override var region
        get() = _region
        set(value) {
//            _region = value
            _region = updateRegion(
                old = _region, new = value,
                bounds = imgRect, aspectLock = aspectLock
            )
        }

    val imgRect by derivedStateOf { getTransformedImageRect(transform, src.size) }

    override var shape: CropShape by mutableStateOf(defaultShape)
    override var aspectLock by mutableStateOf(defaultAspectLock)

    private fun onTransformUpdated(old: ImgTransform, new: ImgTransform) {
        val unTransform = old.asMatrix(src.size).apply { invert() }
        _region = new.asMatrix(src.size).map(unTransform.map(region))
    }

    override fun reset() {
        transform = defaultTransform
        shape = defaultShape
        _region = defaultRegion
        aspectLock = defaultAspectLock
    }

    override var accepted: Boolean by mutableStateOf(false)

    override fun done(accept: Boolean) {
        accepted = accept
        onDone()
    }
}

internal fun getTransformedImageRect(transform: ImgTransform, size: IntSize) : Rect {
    val dstMat = transform.asMatrix(size)
    return dstMat.map(size.toIntRect().toRect())
}

internal fun CropState.rotLeft() {
    transform = transform.copy(angleDeg = transform.angleDeg.prev90())
}

internal fun CropState.rotRight() {
    transform = transform.copy(angleDeg = transform.angleDeg.next90())
}

internal fun CropState.flipHorizontal() {
    if ((transform.angleDeg / 90) % 2 == 0) flipX() else flipY()
}

internal fun CropState.flipVertical() {
    if ((transform.angleDeg / 90) % 2 == 0) flipY() else flipX()
}

internal fun CropState.flipX() {
    transform = transform.copy(scale = transform.scale.copy(x = -1 * transform.scale.x))
}

internal fun CropState.flipY() {
    transform = transform.copy(scale = transform.scale.copy(y = -1 * transform.scale.y))
}

internal fun updateRegion(old: Rect, new: Rect, bounds: Rect, aspectLock: Boolean): Rect {
    val offsetOnly = old.width.eq(new.width) && old.height.eq(new.height)
    return if (offsetOnly) new.constrainOffset(bounds)
    else {
        val result = when {
            aspectLock -> new.keepAspect(old).scaleToFit(bounds, old)
            else -> new.constrainResize(bounds)
        }
        return when {
            result.isEmpty -> result.setSize(old, Size(1f, 1f)).constrainOffset(bounds)
            else -> result
        }
    }
}