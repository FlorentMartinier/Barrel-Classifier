package com.fmartinier.barrelclassifier

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
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
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.service.NotificationService
import com.fmartinier.barrelclassifier.service.PdfService
import com.fmartinier.barrelclassifier.service.QrCloudService
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddBarrel: FloatingActionButton
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

    // DAO
    private lateinit var db: DatabaseHelper
    private lateinit var barrelDao: BarrelDao
    private lateinit var historyDao: HistoryDao

    // Services
    private val notificationService = NotificationService()
    private val imageService = ImageService()
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        db = DatabaseHelper.getInstance(this)
        barrelDao = BarrelDao.getInstance(db)
        historyDao = HistoryDao.getInstance(db)

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

        // Views
        imgArrow = findViewById(R.id.imgArrow)
        recyclerView = findViewById(R.id.recyclerView)
        fabAddBarrel = findViewById(R.id.fabAddBarrel)
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
        fabAddBarrel.setOnClickListener {
            openAddBarrelDialog()
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

        adapter.updateData(barrels)

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
}
