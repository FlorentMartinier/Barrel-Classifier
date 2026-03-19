package com.fmartinier.barrelclassifier.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class QrCloudService(private val context: Context) {

    private var progressDialog: AlertDialog? = null

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

    suspend fun processCloudQR(
        account: GoogleSignInAccount,
        barrel: Barrel,
        activity: Activity
    ) {
        val credential = GoogleAccountCredential.usingOAuth2(
            activity, listOf(DriveScopes.DRIVE_FILE)
        ).setSelectedAccount(account.account)

        val pdfFile = PdfService(activity).export(barrel)

        val publicUrl = uploadPdfToDrive(credential, pdfFile)

        withContext(Dispatchers.Main) {
            if (publicUrl != null) {
                val qrBitmap = generateQRCode(publicUrl)
                hideLoadingDialog()
                shareQrCodeDirectly(qrBitmap, activity)
            } else {
                hideLoadingDialog()
                Toast.makeText(
                    activity,
                    activity.getString(R.string.error_upload), Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun shareQrCodeDirectly(qrBitmap: Bitmap, activity: Activity) {
        val file = saveBitmapToCache(qrBitmap)

        if (file != null) {
            val uri = FileProvider.getUriForFile(
                activity,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Optionnel : force l'affichage du sélecteur d'applis si plusieurs visionneuses existent
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                AnalyticsService.logQrShared()
                activity.startActivity(viewIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.no_image_viewer_error),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    fun showLoadingDialog(activity: Activity) {
        val builder = MaterialAlertDialogBuilder(activity)
        val padding = 50

        val progressBar = ProgressBar(activity).apply {
            setPadding(padding, padding, padding, padding)
        }

        builder.apply {
            setTitle(activity.getString(R.string.google_drive_import))
            setView(progressBar)
            setCancelable(false) // Empêche de fermer en cliquant à côté
        }

        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun hideLoadingDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}