package com.mr0xf00.easycrop

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntSize
import com.mr0xf00.easycrop.utils.IdentityMat

/**
 * Transformation applied on an image with [pivotRel] as pivot's relative position.
 */
public data class ImgTransform(val angleDeg: Int, val scale: Offset, val pivotRel: Offset) {
    internal val hasTransform get() = angleDeg != 0 || scale != Offset(1f, 1f)

    public companion object {
        @Stable
        internal val Identity = ImgTransform(0, Offset(1f, 1f), Offset(.5f, .5f))
    }
}

internal fun ImgTransform.asMatrix(imgSize: IntSize): Matrix {
    if (!hasTransform) return IdentityMat
    val matrix = Matrix()
    val pivot = Offset(imgSize.width * pivotRel.x, imgSize.height * pivotRel.y)
    matrix.translate(pivot.x, pivot.y)
    matrix.rotateZ(angleDeg.toFloat())
    matrix.scale(scale.x, scale.y)
    matrix.translate(-pivot.x, -pivot.y)
    return matrix
}