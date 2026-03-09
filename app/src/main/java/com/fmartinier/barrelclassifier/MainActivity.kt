package com.fmartinier.barrelclassifier

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.AlertDao
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.AlertService
import com.fmartinier.barrelclassifier.service.AnalyticsService
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.service.NotificationService
import com.fmartinier.barrelclassifier.service.PdfService
import com.fmartinier.barrelclassifier.service.QrCloudService
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.fmartinier.barrelclassifier.utils.FileUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var speedDial: SpeedDialView
    private lateinit var emptyStateLayout: RelativeLayout

    private lateinit var adapter: BarrelAdapter

    private lateinit var imgArrow: ImageView

    private var currentBarrelPhotoPath: String? = null
    private var currentHistoryPhotoPath: String? = null
    private var barrelForIntent: Barrel? = null
    private var historyForPhoto: History? = null
    private lateinit var barrelCameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var historyCameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickHistoryImageLauncher: ActivityResultLauncher<String>
    private lateinit var pickBarrelImageLauncher: ActivityResultLauncher<String>
    private lateinit var importQrLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportZipLauncher: ActivityResultLauncher<String>
    private lateinit var importZipLauncher: ActivityResultLauncher<String>

    // DAO
    private lateinit var db: DatabaseHelper
    private lateinit var barrelDao: BarrelDao
    private lateinit var historyDao: HistoryDao
    private lateinit var alertDao: AlertDao

    // Services
    private val notificationService = NotificationService()
    private val imageService = ImageService()
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        db = DatabaseHelper.getInstance(this)
        barrelDao = BarrelDao.getInstance(db)
        historyDao = HistoryDao.getInstance(db)
        alertDao = AlertDao.getInstance(db)

        val ta = theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        managePopupRate()
        ta.recycle()
        setTheme(R.style.Theme_BarrelClassifier)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationService.createNotificationChannel(this)

        supportFragmentManager.setFragmentResultListener(
            "add_barrel_result",
            this
        ) { _, _ ->
            loadBarrels()
        }

        barrelCameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    currentBarrelPhotoPath?.let { path ->
                        barrelForIntent?.let { barrel ->
                            val oldImagePath = barrel.imagePath
                            barrelDao.updateImage(barrel.id, path)
                            imageService.deleteImageIfExist(oldImagePath)
                            loadBarrels()
                        }
                    }
                }
            }

        historyCameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    currentHistoryPhotoPath?.let { path ->
                        historyForPhoto?.let { history ->
                            val oldImagePath = history.imagePath
                            historyDao.updateImage(history.id, path)
                            imageService.deleteImageIfExist(oldImagePath)
                            loadBarrels()
                        }
                    }
                }
            }

        pickHistoryImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val path = imageService.copyImageToInternalStorage(it, this)
                    currentHistoryPhotoPath = path
                    historyForPhoto?.let { history ->
                        val oldImagePath = history.imagePath
                        historyDao.updateImage(history.id, path)
                        imageService.deleteImageIfExist(oldImagePath)
                    }
                }
                loadBarrels()
            }

        pickBarrelImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val path = imageService.copyImageToInternalStorage(it, this)
                currentBarrelPhotoPath = path
                barrelForIntent?.let { barrel ->
                    val oldImagePath = barrel.imagePath
                    barrelDao.updateImage(barrel.id, path)
                    imageService.deleteImageIfExist(oldImagePath)
                }
            }
            loadBarrels()
        }

        importQrLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {

                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.result
                if (account != null) {
                    barrelForIntent?.let { barrel ->
                        processCloudQR(account, barrel)
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error_google_connexion),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Toast.makeText(
                    this,
                    getString(R.string.error_google_authent, e.statusCode.toString()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        exportZipLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument(FileUtils.ZIP_TYPE)
        ) { uri: Uri? ->
            uri?.let { exportToZip(it) }
        }

        importZipLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { importZipArchive(it) }
            }

        // Views
        imgArrow = findViewById(R.id.imgArrow)
        recyclerView = findViewById(R.id.recyclerView)
        speedDial = findViewById(R.id.speedDial)
        emptyStateLayout = findViewById(R.id.layoutEmptyState)

        // RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BarrelAdapter(
            context = this,
            barrels = emptyList(),
            refresh = { loadBarrels() },
            onAddHistory = { barrel, historyId ->
                openAddHistoryDialog(barrel, historyId)
            },
            onEditBarrel = { barrel ->
                openEditBarrel(barrel)
            },
            onTakeBarrelPicture = { barrel ->
                takePhotoForBarrel(barrel)
            },
            onImportBarrelPicture = { barrel ->
                barrelForIntent = barrel
                pickBarrelImageLauncher.launch("image/*")
            },
            onTakeHistoryPicture = { history ->
                takePhotoForHistory(history)
            },
            onExportQrCloud = { intent, barrel ->
                barrelForIntent = barrel
                importQrLauncher.launch(intent)
            },
            onImportHistoryPicture = { history ->
                historyForPhoto = history
                pickHistoryImageLauncher.launch("image/*")
            },
        )

        recyclerView.adapter = adapter

        // FAB - ajout fût
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_add_barrel, R.drawable.ic_add)
                .setLabel(R.string.add_barrel)
                .create()
        )
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_export, R.drawable.ic_export)
                .setLabel(getString(R.string.zip_export))
                .create()
        )
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_import, R.drawable.ic_import)
                .setLabel(getString(R.string.zip_import))
                .create()
        )
        speedDial.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_export -> {
                    exportZipLauncher.launch("barrel_manager_export.json")
                    speedDial.close()
                    true
                }

                R.id.fab_import -> {
                    importZipLauncher.launch(FileUtils.ZIP_TYPE)
                    speedDial.close()
                    true
                }

                R.id.fab_add_barrel -> {
                    openAddBarrelDialog()
                    speedDial.close()
                    true
                }

                else -> false
            }
        }

        // Chargement initial
        loadBarrels()
    }

    // Exemple simplifié du processus après acceptation
    private fun processCloudQR(account: GoogleSignInAccount, barrel: Barrel) {
        showLoadingDialog(getString(R.string.google_drive_import))
        lifecycleScope.launch(Dispatchers.IO) {
            val credential = GoogleAccountCredential.usingOAuth2(
                this@MainActivity, listOf(DriveScopes.DRIVE_FILE)
            ).setSelectedAccount(account.account)

            val manager = QrCloudService(this@MainActivity)
            val pdfFile = PdfService(this@MainActivity).export(barrel)

            val publicUrl = manager.uploadPdfToDrive(credential, pdfFile)

            withContext(Dispatchers.Main) {
                if (publicUrl != null) {
                    val qrBitmap = manager.generateQRCode(publicUrl)
                    hideLoadingDialog()
                    shareQrCodeDirectly(qrBitmap)
                } else {
                    hideLoadingDialog()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_upload), Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun shareQrCodeDirectly(qrBitmap: Bitmap) {
        val manager = QrCloudService(this)
        val file = manager.saveBitmapToCache(qrBitmap)

        if (file != null) {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
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
                startActivity(viewIntent)
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.no_image_viewer_error), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun showLoadingDialog(message: String) {
        val builder = MaterialAlertDialogBuilder(this)
        val padding = 50

        val progressBar = ProgressBar(this).apply {
            setPadding(padding, padding, padding, padding)
        }

        builder.apply {
            setTitle(message)
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

    /**
     * Recharge les fûts depuis la BDD
     * et met à jour l'UI
     */
    private fun loadBarrels() {
        val barrels = barrelDao.findAllWithHistories()
        if (adapter.barrels != barrels) {
            adapter.updateData(barrels)
        }

        if (barrels.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            startArrowAnimation()
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            stopArrowAnimation()
        }
    }

    /**
     * Ouvre le dialog d'ajout de fût
     */
    private fun openAddBarrelDialog() {
        AddBarrelDialog
            .newInstance()
            .show(supportFragmentManager, AddBarrelDialog.TAG)
    }

    private fun openAddHistoryDialog(barrel: Barrel, historyId: Long? = null) {
        AddHistoryDialog.newInstance(barrel, historyId)
            .show(supportFragmentManager, AddHistoryDialog.TAG)
    }

    private fun openEditBarrel(barrel: Barrel) {
        AddBarrelDialog.newInstance(barrel.id)
            .show(supportFragmentManager, AddBarrelDialog.TAG)
    }

    private fun startArrowAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.arrow_bounce)
        imgArrow.startAnimation(animation)
    }

    private fun stopArrowAnimation() {
        imgArrow.clearAnimation()
    }

    private fun takePhotoForBarrel(barrel: Barrel) {
        barrelForIntent = barrel
        val photoFile = imageService.createImageFile(this)
        currentBarrelPhotoPath = photoFile.absolutePath
        barrelCameraLauncher.launch(imageService.takePhoto(this, photoFile))
    }

    private fun takePhotoForHistory(history: History) {
        historyForPhoto = history
        val photoFile = imageService.createImageFile(this)
        currentHistoryPhotoPath = photoFile.absolutePath
        historyCameraLauncher.launch(imageService.takePhoto(this, photoFile))
    }

    private fun managePopupRate() {
        val prefs = getSharedPreferences("app_stats", MODE_PRIVATE)
        val hasRated = prefs.getBoolean("has_rated", false)

        if (!hasRated) {
            val launchCount = prefs.getInt("launch_count", 0) + 1

            prefs.edit { putInt("launch_count", launchCount) }
            println("launchCount : $launchCount")

            if (listOf(3, 6, 9).contains(launchCount)) {
                showRatePopup()
            }
        }
    }

    private fun showRatePopup() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rate_popup_title))
            .setMessage(getString(R.string.rate_popup_description))
            .setPositiveButton(getString(R.string.note_app)) { _, _ ->
                // On marque comme noté pour ne plus redemander
                getSharedPreferences("app_stats", MODE_PRIVATE).edit {
                    putBoolean(
                        "has_rated",
                        true
                    )
                }

                val appPackage = packageName
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "market://details?id=$appPackage".toUri()
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=$appPackage".toUri()
                        )
                    )
                }
            }
            .setNegativeButton(getString(R.string.later), null)
            .show()
    }

    fun exportToZip(uri: Uri) {
        val (jsonString, imagePaths) = exportBarrelsWithImages()
        val zipFile = File(applicationContext.cacheDir, "barrel_manager_export.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { out ->
            // 1. Ajouter le JSON
            val entry = ZipEntry("data.json")
            out.putNextEntry(entry)
            out.write(jsonString.toByteArray())
            out.closeEntry()

            // 2. Ajouter les images
            imagePaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    out.putNextEntry(ZipEntry("images/${file.name}"))
                    file.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            zipFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
            zipFile.delete()

            Toast.makeText(this, getString(R.string.zip_export_success), Toast.LENGTH_SHORT).show()
        }
    }

    fun importZipArchive(zipUri: Uri) {
        val tempDir = File(applicationContext.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            FileUtils.unzip(applicationContext, zipUri, tempDir)

            // 2. Chercher le fichier JSON dans les fichiers extraits
            val jsonFile = File(tempDir, "data.json")
            if (!jsonFile.exists()) throw Exception(getString(R.string.data_json_missing))

            val jsonString = jsonFile.readText()
            val importedData = importJson(jsonString)

            // 4. Traiter et déplacer les images
            val finalImageDir = File(applicationContext.filesDir, "")
            if (!finalImageDir.exists()) finalImageDir.mkdirs()

            importedData.flatMap { barrel ->
                val fileName = File(barrel.imagePath ?: "").name
                if (fileName.isNullOrEmpty()) {
                    return@flatMap emptyList<History>()
                }
                val tempImage = File(tempDir, "images/$fileName")

                if (tempImage.exists()) {
                    val permanentImage = File(finalImageDir, fileName)
                    tempImage.copyTo(permanentImage, overwrite = true)
                    barrelDao.updateImage(barrel.id, permanentImage.absolutePath)
                }
                barrel.histories
            }.forEach { history ->
                val fileName = File(history.imagePath ?: "").name
                if (fileName.isNullOrEmpty()) {
                    return@forEach
                }
                val tempImage = File(tempDir, "images/$fileName")
                if (tempImage.exists()) {
                    val permanentImage = File(finalImageDir, fileName)
                    tempImage.copyTo(permanentImage, overwrite = true)
                    historyDao.updateImage(history.id, permanentImage.absolutePath)
                }
            }

            Toast.makeText(applicationContext,
                getString(R.string.zip_import_success), Toast.LENGTH_SHORT).show()
            AnalyticsService.logImportSuccess()
            loadBarrels()
        } catch (_: Exception) {
            Toast.makeText(applicationContext,
                getString(R.string.zip_import_error), Toast.LENGTH_SHORT).show()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun exportBarrelsWithImages(): Pair<String, List<String>> {
        barrelDao.findAllWithHistories().let {
            val barrelImages = it.mapNotNull { barrel -> barrel.imagePath }
            val historyImages = it
                .flatMap { barrel -> barrel.histories }
                .mapNotNull { history -> history.imagePath }
            val allImages = barrelImages + historyImages

            return Pair(jacksonObjectMapper().writeValueAsString(it), allImages)
        }
    }

    private fun importJson(jsonString: String): List<Barrel> {
        try {
            val mapper = jacksonObjectMapper()
            val importedData = mapper.readValue(jsonString, jacksonTypeRef<List<Barrel>>())

            // 3. Injecter dans la BDD
            importedData.forEach { barrel ->
                val barrelId = barrelDao.insert(barrel)
                barrel.histories.forEach { history ->
                    val historyToSave = history.copy(barrelId = barrelId)
                    val historyId = historyDao.insert(historyToSave)
                    val alertsToSave = history.alerts.map { alert ->
                        alert.copy(historyId = historyId)
                    }
                    alertDao.insert(alertsToSave, historyId)
                        .forEach {
                            AlertService().schedule(applicationContext, it, barrel.name, historyId)
                        }
                }
            }

            return importedData
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity,
                getString(R.string.imported_file_incompatible), Toast.LENGTH_LONG).show()
            AnalyticsService.logImportError(e.message.toString())
        }
        return listOf()
    }
}
