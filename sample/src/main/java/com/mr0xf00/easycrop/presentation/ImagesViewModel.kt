package com.mr0xf00.easycrop.presentation

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mr0xf00.easycrop.CropError
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.ImageCropper
import com.mr0xf00.easycrop.crop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImagesViewModel(private val app: Application) : AndroidViewModel(app) {
    val imageCropper = ImageCropper()
    private val _selectedImage = MutableStateFlow<ImageBitmap?>(null)
    val selectedImage = _selectedImage.asStateFlow()
    private val _cropError = MutableStateFlow<CropError?>(null)
    val cropError = _cropError.asStateFlow()

    fun cropErrorShown() {
        _cropError.value = null
    }

    fun setSelectedImage(uri: Uri) {
        viewModelScope.launch {

            val bitmap = app.contentResolver.openInputStream(uri).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
            when(val result = imageCropper.crop(bmp = bitmap.asImageBitmap())) {
                CropResult.Cancelled -> {}
                is CropError -> _cropError.value = result
                is CropResult.Success -> {
                    _selectedImage.value = result.bitmap
                }
            }
        }
    }
}