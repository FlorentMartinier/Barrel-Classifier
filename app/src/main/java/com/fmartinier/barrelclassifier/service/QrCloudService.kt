package com.fmartinier.barrelclassifier.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.fmartinier.barrelclassifier.R
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class QrCloudService(private val context: Context) {

    // 1. Upload le fichier et retourne l'URL publique
    suspend fun uploadPdfToDrive(
        credential: GoogleAccountCredential,
        pdfFile: java.io.File
    ): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = Drive.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory(),
                credential
            ).setApplicationName(context.getString(R.string.app_name)).build()

            // Configuration des métadonnées du fichier
            val fileMetadata = File().apply {
                name = pdfFile.name
                parents = listOf("root") // Ou un dossier spécifique
            }

            val mediaContent = FileContent("application/pdf", pdfFile)

            // Upload
            val googleFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            // 2. Rendre le fichier public (Lecture seule pour tous ceux qui ont le lien)
            val publicPermission = Permission().apply {
                type = "anyone"
                role = "reader"
            }
            driveService.permissions().create(googleFile.id, publicPermission).execute()

            return@withContext googleFile.webViewLink
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    // 2. Génère le Bitmap du QR Code
    fun generateQRCode(url: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }

    // Dans ta classe CloudQRManager, ajoute cette fonction :
    fun saveBitmapToCache(bitmap: Bitmap): java.io.File? {
        try {
            // On enregistre dans le dossier "Pictures" public pour que l'utilisateur le retrouve
            val cachePath = java.io.File(context.cacheDir, "temp_qr")
            cachePath.mkdirs()
            val imageFile = java.io.File(cachePath, "barrel_qr_code.png")

            val out = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            return imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}