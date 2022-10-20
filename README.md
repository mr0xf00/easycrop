# EasyCrop for Jetpack Compose

<p align="center">
<img src="https://img.shields.io/maven-central/v/io.github.mr0xf00/easycrop">
</p>
<p align="center">Easy to use image cropping library for Jetpack compose, with support for shapes, aspect-ratios, transformations, large images, auto zoom ...</p>
<p align="center">
<img src="images/preview.gif"/>
</p>

## Getting Started

#### 1. Download

Add a dependency on the library to your Android project (Desktop not supported for now)

```kotlin
dependencies {
    implementation("io.github.mr0xf00:easycrop:0.1.0")
}
```
#### 2. Create an `ImageCropper` an instance
#### ***Option 1 : inside the composition***
```kotlin
val imageCropper = rememberImageCropper()
```
#### ***Option 2 : outside the composition (eg. ViewModel)***
```kotlin
class MyViewModel : ViewModel {
    val imageCropper = ImageCropper()
}
```
#### 3. Crop
```kotlin
scope.launch {
    val result = imageCropper.crop(bitmap) // Suspends until user accepts or cancels cropping
    when (result) {
        CropResult.Cancelled -> { }
        is CropError -> { }
        is CropResult.Success -> { result.bitmap }
    }
}
```
#### 4. Show the crop dialog
```kotlin
val cropState = imageCropper.cropState 
if(cropState != null) ImageCropperDialog(state = cropState)
```
That's it !
### Using different sources
The ```crop``` function provides overloads for `ImageBitmap`, `Uri`, `File`, but it is also possible to use a custom `ImageSrc`.

You can use the ```rememberImagePicker``` function to easily pick an image and crop it :
```kotlin
val scope = rememberCoroutineScope()
val context = LocalContext.current
val imagePicker = rememberImagePicker(onImage = { uri ->
    scope.launch {
        val result = imageCropper.crop(uri, context)
    }
})
```

### Customization 
To customize the ui of the image cropper you can provide a different implementation of `CropperStyle` to the cropper dialog.
You can also use the `CropperStyle` factory function. example :
```kotlin
ImageCropperDialog(
    state = cropState,
    style = CropperStyle(
        overlay = Color.Red.copy(alpha = .5f),
        autoZoom = false,
        guidelines = null,
    )
)
```
