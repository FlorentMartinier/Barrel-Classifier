package com.fmartinier.barrelclassifier

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog
import com.fmartinier.barrelclassifier.ui.BarrelAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddBarrel: FloatingActionButton
    private lateinit var emptyStateLayout: RelativeLayout

    private lateinit var adapter: BarrelAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var barrelDao: BarrelDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation BDD
        dbHelper = DatabaseHelper(this)
        barrelDao = BarrelDao(dbHelper)

        // Views
        recyclerView = findViewById(R.id.recyclerView)
        fabAddBarrel = findViewById(R.id.fabAddBarrel)
        emptyStateLayout = findViewById(R.id.layoutEmptyState)

        // RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BarrelAdapter(
            context = this,
            barrels = emptyList(),
            refresh = { loadBarrels() },
            onAddHistory = { barrelId ->
                openAddHistoryDialog(barrelId)
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
        val barrels = barrelDao.getAllBarrelsWithHistorique()

        adapter.updateData(barrels)

        if (barrels.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Ouvre le dialog d'ajout de fût
     */
    private fun openAddBarrelDialog() {
        val dialog = AddBarrelDialog {
            loadBarrels()
        }
        dialog.show(supportFragmentManager, "AddBarrelDialog")
    }

    private fun openAddHistoryDialog(barrelId: Long) {
        val dialog = AddHistoryDialog(
            barrelId = barrelId,
            onHistoryAdded = {
                loadBarrels()
            }
        )
        dialog.show(supportFragmentManager, "AddHistoryDialog")
    }

}
