package com.mr0xf00.easycrop.utils.compose

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

internal interface GestureState {
    val zoom: ZoomState
    val drag: DragState
    val tap: TapState
}

internal interface DragState {
    fun onBegin(x: Float, y: Float) = Unit
    fun onNext(dx: Float, dy: Float, x: Float, y: Float, pointers: Int) = Unit
    fun onDone() = Unit
}

internal inline fun DragState(
    crossinline begin: (pos: Offset) -> Unit = { },
    crossinline done: () -> Unit = {},
    crossinline next: (delta: Offset, pos: Offset, pointers: Int) -> Unit = { _, _, _ -> },
): DragState = object : DragState {
    override fun onBegin(x: Float, y: Float) = begin(Offset(x, y))
    override fun onNext(dx: Float, dy: Float, x: Float, y: Float, pointers: Int) =
        next(Offset(dx, dy), Offset(x, y), pointers)

    override fun onDone() = done()
}

internal interface TapState {
    fun onTap(x: Float, y: Float, pointers: Int) = Unit
    fun onLongPress(x: Float, y: Float, pointers: Int) = Unit
}

internal inline fun TapState(
    crossinline longPress: (pos: Offset, pointers: Int) -> Unit = { _, _ -> },
    crossinline tap: (pos: Offset, pointers: Int) -> Unit = { _, _ -> },
) = object : TapState {
    override fun onTap(x: Float, y: Float, pointers: Int) = tap(Offset(x, y), pointers)
    override fun onLongPress(x: Float, y: Float, pointers: Int) = longPress(Offset(x, y), pointers)
}

internal interface ZoomState {
    fun onBegin(cx: Float, cy: Float) = Unit
    fun onNext(scale: Float, cx: Float, cy: Float) = Unit
    fun onDone() = Unit
}

internal inline fun ZoomState(
    crossinline begin: (center: Offset) -> Unit = { },
    crossinline done: () -> Unit = {},
    crossinline next: (scale: Float, center: Offset) -> Unit = { _, _ -> },
): ZoomState = object : ZoomState {
    override fun onBegin(cx: Float, cy: Float) = begin(Offset(cx, cy))
    override fun onNext(scale: Float, cx: Float, cy: Float) = next(scale, Offset(cx, cy))
    override fun onDone() = done()
}

@Composable
internal fun rememberGestureState(
    zoom: ZoomState? = null,
    drag: DragState? = null,
    tap: TapState? = null,
): GestureState {
    val zoomState by rememberUpdatedState(newValue = zoom ?: object : ZoomState {})
    val dragState by rememberUpdatedState(newValue = drag ?: object : DragState {})
    val tapState by rememberUpdatedState(newValue = tap ?: object : TapState {})
    return object : GestureState {
        override val zoom: ZoomState get() = zoomState
        override val drag: DragState get() = dragState
        override val tap: TapState get() = tapState
    }
}

private data class GestureData(
    var dragId: PointerId = PointerId(-1),
    var firstPos: Offset = Offset.Unspecified,
    var pos: Offset = Offset.Unspecified,
    var nextPos: Offset = Offset.Unspecified,
    var pointers: Int = 0,
    var maxPointers: Int = 0,
    var isDrag: Boolean = false,
    var isZoom: Boolean = false,
    var isTap: Boolean = false,
)


internal fun Modifier.onGestures(state: GestureState): Modifier {
    return pointerInput(Unit) {
        coroutineScope {
            var info = GestureData()
            launch {
                detectTapGestures(
                    onLongPress = { state.tap.onLongPress(it.x, it.y, info.maxPointers) },
                    onTap = { state.tap.onTap(it.x, it.y, info.maxPointers) },
                )
            }
            launch {
                detectTransformGestures(panZoomLock = true) { c, _, zoom, _ ->
                    if (!(info.isDrag || info.isZoom)) {
                        if (info.pointers == 1) {
                            state.drag.onBegin(info.firstPos.x, info.firstPos.y)
                            info.pos = info.firstPos
                            info.isDrag = true
                        } else if (info.pointers > 1) {
                            state.zoom.onBegin(c.x, c.y)
                            info.isZoom = true
                        }
                    }
                    if (info.isDrag) {
                        state.drag.onNext(
                            info.nextPos.x - info.pos.x, info.nextPos.y - info.pos.y,
                            info.nextPos.x, info.nextPos.y, info.pointers
                        )
                        info.pos = info.nextPos
                    } else if (info.isZoom) {
                        if (zoom != 1f) state.zoom.onNext(zoom, c.x, c.y)
                    }
                }
            }
            launch {
                forEachGesture {
                    awaitPointerEventScope {
                        info = GestureData()
                        val first = awaitFirstDown(requireUnconsumed = false)
                        info.dragId = first.id
                        info.firstPos = first.position
                        info.pointers = 1
                        info.maxPointers = 1
                        var event: PointerEvent
                        while (info.pointers > 0) {
                            event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            var dragPointer: PointerInputChange? = null
                            for (change in event.changes) {
                                if (change.changedToDownIgnoreConsumed()) info.pointers++
                                else if (change.changedToUpIgnoreConsumed()) info.pointers--
                                info.maxPointers = max(info.maxPointers, info.pointers)
                                if (change.id == info.dragId) dragPointer = change
                            }
                            if (dragPointer == null) dragPointer =
                                event.changes.firstOrNull { it.pressed }
                            if (dragPointer != null) {
                                info.nextPos = dragPointer.position
                                if (info.dragId != dragPointer.id) {
                                    info.pos = info.nextPos
                                    info.dragId = dragPointer.id
                                }
                            }
                        }
                        if (info.isDrag) state.drag.onDone()
                        if (info.isZoom) state.zoom.onDone()
                    }
                }
            }
        }
    }
}
