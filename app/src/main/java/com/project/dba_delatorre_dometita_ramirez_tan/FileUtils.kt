package com.project.dba_delatorre_dometita_ramirez_tan


import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun copyImageToInternalStorage(context: Context, uri: Uri): String {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val fileName = "product_image_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)
    val outputStream = FileOutputStream(file)
    inputStream?.copyTo(outputStream)
    inputStream?.close()
    outputStream.close()
    return file.absolutePath
}
