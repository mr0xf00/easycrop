package com.mr0xf00.easycrop.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

private const val Eps: Float = 2.4414062E-4f

internal fun Float.eq0(): Boolean = abs(this) <= Eps
internal infix fun Float.eq(v: Float): Boolean = abs(v - this) <= Eps
internal fun Offset.eq(other: Offset) = x.eq(other.x) && y.eq(other.y)

internal fun lerp(a: Float, b: Float, p: Float): Float = a + p * (b - a)
internal fun lerp(a: Int, b: Int, p: Float) = lerp(a.toFloat(), b.toFloat(), p).roundToInt()

internal fun lerpAngle(a: Int, b: Int, p: Float): Int {
    val angleDist = (2 * ((b - a) % 360) % 360 - (b - a) % 360)
    return (a + angleDist * p).roundToInt()
}

internal fun Int.next90() = (this + 90).angleRange()
internal fun Int.prev90() = (this - 90).angleRange()
internal fun Int.angleRange(): Int {
    val angle = (this % 360 + 360) % 360
    return if (angle <= 180) angle else angle - 360
}

fun Float.alignDown(alignment: Int): Float = floor(this / alignment) * alignment
fun Float.alignUp(alignment: Int): Float = ceil(this / alignment) * alignment
fun Float.align(alignment: Int): Float = round(this / alignment) * alignment
