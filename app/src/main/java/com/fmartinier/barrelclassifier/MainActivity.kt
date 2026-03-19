package com.fmartinier.barrelclassifier

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.service.ImportExportService
import com.fmartinier.barrelclassifier.service.NotificationService
import com.fmartinier.barrelclassifier.service.QrCloudService
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.fmartinier.barrelclassifier.ui.BarrelFullViewBinder
import com.fmartinier.barrelclassifier.utils.FileUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var speedDial: SpeedDialView
    private lateinit var emptyStateLayout: RelativeLayout
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    private lateinit var adapter: BarrelAdapter

    private lateinit var imgArrow: ImageView

    private lateinit var exportZipLauncher: ActivityResultLauncher<String>
    private lateinit var importZipLauncher: ActivityResultLauncher<String>

    // DAO
    private lateinit var db: DatabaseHelper
    private lateinit var barrelDao: BarrelDao
    private lateinit var historyDao: HistoryDao

    // Services
    private val notificationService = NotificationService()
    private var isGridView = false
    private lateinit var qrCloudService: QrCloudService
    private lateinit var importExportService: ImportExportService

    private lateinit var barrelFullViewBinder: BarrelFullViewBinder

    private val detailActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        loadBarrels()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        db = DatabaseHelper.getInstance(this)
        barrelDao = BarrelDao.getInstance(db)
        historyDao = HistoryDao.getInstance(db)
        qrCloudService = QrCloudService(applicationContext)
        importExportService = ImportExportService(this)

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

        exportZipLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument(FileUtils.ZIP_TYPE)
        ) { uri: Uri? ->
            uri?.let { importExportService.exportToZip(it) }
        }

        importZipLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    importExportService.importZipArchive(it)
                    loadBarrels()
                }
            }

        // Views
        imgArrow = findViewById(R.id.imgArrow)
        recyclerView = findViewById(R.id.recyclerView)
        speedDial = findViewById(R.id.speedDial)
        emptyStateLayout = findViewById(R.id.layoutEmptyState)
        toggleGroup = findViewById(R.id.toggleGroupView)

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val transition = AutoTransition()
                transition.duration = 300
                TransitionManager.beginDelayedTransition(recyclerView, transition)
                recyclerView.layoutManager = when (checkedId) {
                    R.id.btnGrid -> {
                        isGridView = true
                        GridLayoutManager(this, 2)
                    }
                    R.id.btnList -> {
                        isGridView = false
                        LinearLayoutManager(this)
                    }

                    else -> null
                }
                adapter.updateLayout(isGridView)
            }
        }

        // RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        barrelFullViewBinder = BarrelFullViewBinder(
            refresh = { loadBarrels() },
            fragmentManager = supportFragmentManager,
            activity = this
        )

        adapter = BarrelAdapter(
            context = this,
            barrels = emptyList(),
            isGrid = false,
            barrelFullViewBinder = barrelFullViewBinder,
            onBarrelClick = { intent, option ->
                detailActivityResultLauncher.launch(intent, option)
            }
        )

        recyclerView.adapter = adapter

        // FAB - ajout fût
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_add_barrel, R.drawable.ic_add)
                .setFabImageTintColor(getColor(R.color.text_primary))
                .setLabel(R.string.add_barrel)
                .create()
        )
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_export, R.drawable.ic_export)
                .setFabImageTintColor(getColor(R.color.text_primary))
                .setLabel(getString(R.string.zip_export))
                .create()
        )
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_import, R.drawable.ic_import)
                .setFabImageTintColor(getColor(R.color.text_primary))
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

    private fun startArrowAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.arrow_bounce)
        imgArrow.startAnimation(animation)
    }

    private fun stopArrowAnimation() {
        imgArrow.clearAnimation()
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
