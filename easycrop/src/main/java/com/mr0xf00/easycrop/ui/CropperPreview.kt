package com.mr0xf00.easycrop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.mr0xf00.easycrop.*
import com.mr0xf00.easycrop.images.rememberLoadedImage
import com.mr0xf00.easycrop.utils.ViewMat
import com.mr0xf00.easycrop.utils.times
import kotlinx.coroutines.delay

@Composable
fun CropperPreview(
    state: CropState,
    modifier: Modifier = Modifier,
    bringToViewDelay: Long = 500,
) {
    val style = LocalCropperStyle.current
    val imgTransform by animateImgTransform(target = state.transform)
    val imgMat = remember(imgTransform, state.src.size) { imgTransform.asMatrix(state.src.size) }
    val viewMat = remember { ViewMat() }
    var view by remember { mutableStateOf(IntSize.Zero) }
    var pendingDrag by remember { mutableStateOf<DragHandle?>(null) }
    val viewPadding = LocalDensity.current.run { style.touchRad.toPx() }
    val totalMat = remember(viewMat.matrix, imgMat) { imgMat * viewMat.matrix }
    val image = rememberLoadedImage(state.src, view, totalMat)
    val cropRect = remember(state.region, viewMat.matrix) {
        viewMat.matrix.map(state.region)
    }
    val cropPath = remember(state.shape, cropRect) { state.shape.asPath(cropRect) }
    BringToView(
        enabled = style.autoZoom,
        hasOverride = pendingDrag != null,
        outer = view.toSize().toRect().deflate(viewPadding),
        mat = viewMat, local = state.region,
        delay = bringToViewDelay,
    )
    Canvas(
        modifier = modifier
            .onGloballyPositioned { view = it.size }
            .background(color = style.backgroundColor)
            .cropperTouch(
                region = state.region,
                onRegion = { state.region = it },
                touchRad = style.touchRad, handles = style.handles,
                viewMat = viewMat,
                pending = pendingDrag,
                onPending = { pendingDrag = it },
            )
    ) {
        withTransform({ transform(totalMat) }) {
            image?.let { (params, bitmap) ->
                drawImage(
                    bitmap, dstOffset = params.subset.topLeft,
                    dstSize = params.subset.size
                )
            }
        }
        with(style) {
            clipPath(cropPath, ClipOp.Difference) {
                drawRect(color = overlayColor)
            }
            drawCropRect(cropRect)
        }
    }
}

@Composable
private fun BringToView(
    enabled: Boolean,
    hasOverride: Boolean,
    outer: Rect,
    mat: ViewMat,
    local: Rect,
    delay: Long = 500,
) {
    if (outer.isEmpty) return
    DisposableEffect(Unit) {
        mat.snapFit(mat.matrix.map(local), outer)
        onDispose { }
    }
    if (!enabled) return
    var overrideBlock by remember { mutableStateOf(false) }
    LaunchedEffect(hasOverride, outer, local) {
        if (hasOverride) overrideBlock = true
        else {
            if (overrideBlock) {
                delay(delay)
                overrideBlock = false
            }
            mat.fit(mat.matrix.map(local), outer)
        }
    }
}
