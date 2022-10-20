package com.mr0xf00.easycrop.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

internal val IdentityMat = Matrix()

internal operator fun Matrix.times(other: Matrix): Matrix = copy().apply {
    this *= other
}

internal fun Matrix.setScaleTranslate(sx: Float, sy: Float, tx: Float, ty: Float) {
    reset()
    values[Matrix.ScaleX] = sx
    values[Matrix.TranslateX] = tx
    values[Matrix.ScaleY] = sy
    values[Matrix.TranslateY] = ty
}

internal fun Matrix.setRectToRect(src: Rect, dst: Rect) {
    val sx: Float = dst.width / src.width
    val tx = dst.left - src.left * sx
    val sy: Float = dst.height / src.height
    val ty = dst.top - src.top * sy
    setScaleTranslate(sx, sy, tx, ty)
}

internal fun Matrix.copy(): Matrix = Matrix(values.clone())

internal fun Matrix.inverted() = copy().apply { invert() }