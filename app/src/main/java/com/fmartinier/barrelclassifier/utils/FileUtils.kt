package com.fmartinier.barrelclassifier.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipInputStream

class FileUtils {

    companion object {
        const val PDF_TYPE = "application/pdf"
        const val JSON_TYPE = "application/json"
        const val ZIP_TYPE = "application/zip"

        fun viewFile(context: Context, file: File, fileType: String) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileType)
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            context.startActivity(intent)
        }

        fun unzip(context: Context, zipUri: Uri, destinationDir: File) {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(destinationDir, entry.name)

                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            // Créer les dossiers parents si nécessaire (ex: le dossier "images/")
                            newFile.parentFile?.mkdirs()
                            newFile.outputStream().use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        }
    }
}