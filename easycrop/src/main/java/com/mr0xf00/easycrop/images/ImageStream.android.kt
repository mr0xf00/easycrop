package com.mr0xf00.easycrop.images

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

internal fun interface ImageStream {
    fun openStream(): InputStream?
}

internal suspend fun Uri.toImageSrc(context: Context) = ImageStreamSrc(UriImageStream(this, context))
internal suspend fun File.toImageSrc() = ImageStreamSrc(FileImageStream(this))

internal data class FileImageStream(val file: File) : ImageStream {
    override fun openStream(): InputStream = file.inputStream()
}

internal data class UriImageStream(val uri: Uri, val context: Context) : ImageStream {
    override fun openStream(): InputStream? = context.contentResolver.openInputStream(uri)
}

