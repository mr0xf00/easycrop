package com.mr0xf00.easycrop.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mr0xf00.easycrop.*
import com.mr0xf00.easycrop.R

private val CropperDialogProperties = @OptIn(ExperimentalComposeUiApi::class) (DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false
))

@Composable
fun ImageCropperDialog(
    state: CropState,
    style: CropperStyle = DefaultCropperStyle,
    dialogProperties: DialogProperties = CropperDialogProperties,
    dialogPadding: PaddingValues = PaddingValues(16.dp),
    dialogShape: Shape = RoundedCornerShape(8.dp),
    topBar: @Composable (CropState) -> Unit = { DefaultTopBar(it) },
    cropControls: @Composable BoxScope.(CropState) -> Unit = { DefaultControls(it) },
    bringToViewDelay: Long = 500,
) {
    CompositionLocalProvider(LocalCropperStyle provides style) {
        Dialog(
            onDismissRequest = { state.done(accept = false) },
            properties = dialogProperties,
        ) {
            Surface(
                modifier = Modifier.padding(dialogPadding),
                shape = dialogShape,
            ) {
                Column {
                    topBar(state)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds()
                    ) {
                        CropperPreview(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            bringToViewDelay = bringToViewDelay,
                        )
                        cropControls(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DefaultControls(state: CropState) {
    val verticalControls =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    CropperControls(
        isVertical = verticalControls,
        state = state,
        modifier = Modifier
            .align(if (!verticalControls) Alignment.BottomCenter else Alignment.CenterEnd)
            .padding(12.dp),
    )
}

@Composable
private fun DefaultTopBar(state: CropState) {
    TopAppBar(title = {},
        navigationIcon = {
            IconButton(onClick = { state.done(accept = false) }) {
                Icon(Icons.Default.ArrowBack, null)
            }
        },
        actions = {
            IconButton(onClick = { state.reset() }) {
                Icon(painterResource(R.drawable.restore), null)
            }
            IconButton(onClick = { state.done(accept = true) }, enabled = !state.accepted) {
                Icon(Icons.Default.Done, null)
            }
        }
    )
}
