package com.fmartinier.barrelclassifier

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.service.NotificationService
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddBarrel: FloatingActionButton
    private lateinit var emptyStateLayout: RelativeLayout

    private lateinit var adapter: BarrelAdapter

    private lateinit var imgArrow: ImageView

    private var currentBarrelPhotoPath: String? = null
    private var currentHistoryPhotoPath: String? = null
    private var barrelForPhoto: Barrel? = null
    private var historyForPhoto: History? = null
    private lateinit var barrelCameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var historyCameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickHistoryImageLauncher: ActivityResultLauncher<String>
    private lateinit var pickBarrelImageLauncher: ActivityResultLauncher<String>

    // DAO
    private val db = DatabaseHelper(this)
    private val barrelDao = BarrelDao(db)
    private val historyDao = HistoryDao(db)

    // Services
    private val notificationService = NotificationService()
    private val imageService = ImageService()


    override fun onCreate(savedInstanceState: Bundle?) {
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
                        barrelForPhoto?.let { barrel ->
                            barrelDao.updateImage(barrel.id, path)
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
                            historyDao.updateImage(history.id, path)
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
                        historyDao.updateImage(history.id, path)
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
                barrelForPhoto?.let { barrel ->
                    barrelDao.updateImage(barrel.id, path)
                }
            }
            loadBarrels()
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
                barrelForPhoto = barrel
                pickBarrelImageLauncher.launch("image/*")
            },
            onTakeHistoryPicture = { history ->
                takePhotoForHistory(history)
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

    /**
     * Recharge les fûts depuis la BDD
     * et met à jour l'UI
     */
    private fun loadBarrels() {
        val barrels = barrelDao.getAllBarrelsWithHistories()

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
        Log.d("MainActivity, this.theme.toString()", this.theme.toString())
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
        barrelForPhoto = barrel
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
