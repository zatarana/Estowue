package com.example.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageHelper {
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.toString()
        }
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val fileName = "product_img_${UUID.randomUUID()}.jpg"
            val outputDir = File(context.filesDir, "product_images")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val outputFile = File(outputDir, fileName)
            val outputStream = FileOutputStream(outputFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outputFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
