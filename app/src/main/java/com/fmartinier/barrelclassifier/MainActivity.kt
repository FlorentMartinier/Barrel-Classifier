package com.fmartinier.barrelclassifier

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddBarrel: FloatingActionButton
    private lateinit var emptyStateLayout: RelativeLayout

    private lateinit var adapter: BarrelAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var barrelDao: BarrelDao

    private lateinit var imgArrow: ImageView

    private var currentPhotoPath: String? = null
    private var barrelForPhoto: Barrel? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel(context = this)

        supportFragmentManager.setFragmentResultListener(
            "add_barrel_result",
            this
        ) { _, _ ->
            loadBarrels()
        }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    currentPhotoPath?.let { path ->
                        barrelForPhoto?.let { barrel ->
                            val db = DatabaseHelper(this)
                            BarrelDao(db).updateImage(barrel.id, path)
                            loadBarrels()
                        }
                    }
                }
            }

        // Initialisation BDD
        dbHelper = DatabaseHelper(this)
        barrelDao = BarrelDao(dbHelper)

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
            onEditPhoto = { barrel ->
                takePhoto(barrel)
            }
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

    private fun takePhoto(barrel: Barrel) {
        barrelForPhoto = barrel

        val photoFile = createImageFile()
        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val storageDir = filesDir
        val file = File.createTempFile(
            "barrel_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
        currentPhotoPath = file.absolutePath
        return file
    }

    private fun createNotificationChannel(context: Context) {
        // Gérer la permission de lancer des notifications
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(POST_NOTIFICATIONS),
                1001
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alerts_channel",
                "Alertes de vieillissement",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes liées aux historiques en fût"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }



}
