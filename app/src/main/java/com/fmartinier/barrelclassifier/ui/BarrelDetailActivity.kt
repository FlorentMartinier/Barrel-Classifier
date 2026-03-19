package com.fmartinier.barrelclassifier.ui

import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.ui.model.BarrelViewHolder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

class BarrelDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var barrelDao: BarrelDao

    private lateinit var barrelFullViewBinder: BarrelFullViewBinder
    private lateinit var cardView:View
    private lateinit var holder: BarrelViewHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = DatabaseHelper.getInstance(this)
        barrelDao = BarrelDao.getInstance(db)

        // 1. Activer la transition Material (pour l'effet de zoom)
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        findViewById<View>(android.R.id.content).transitionName = "barrel_detail_transition"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        setContentView(R.layout.activity_barrel_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        toolbar.setNavigationOnClickListener {
            closeActivity()
        }

        // 2. Récupérer l'ID passé par l'intent
        val barrelId = intent.getLongExtra("BARREL_ID", -1)

        supportFragmentManager.setFragmentResultListener(
            "add_barrel_result",
            this
        ) { _, _ ->
            loadBarrelData(barrelId)
        }

        cardView = findViewById<View>(R.id.cardDetail)
        holder = BarrelViewHolder(cardView)
        barrelFullViewBinder = BarrelFullViewBinder(
            refresh = { loadBarrelData(barrelId) },
            fragmentManager = supportFragmentManager,
            activity = this
        )
        loadBarrelData(barrelId)
    }

    private fun loadBarrelData(barrelId: Long) {
        try {
            val freshBarrel = barrelDao.findById(barrelId)
            barrelFullViewBinder.bind(
                holder = holder,
                barrel = freshBarrel
            )
        } catch (_: IllegalStateException) {
            // Dans le cas d'une suppression, le fût n'existe pas. On ferme l'activité
            closeActivity()
        }
    }

    private fun closeActivity() {
        onBackPressedDispatcher.onBackPressed()
    }
}