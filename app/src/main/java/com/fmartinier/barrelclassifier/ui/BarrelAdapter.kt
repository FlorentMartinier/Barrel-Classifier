package com.fmartinier.barrelclassifier.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateDuration
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate

class BarrelAdapter(
    private val context: Context,
    private var barrels: List<Barrel>,
    private val refresh: () -> Unit,
    private val onAddHistory: (Long) -> Unit,
    private val onEditBarrel: (Barrel) -> Unit,
    private val onEditPhoto: (Barrel) -> Unit
) : RecyclerView.Adapter<BarrelAdapter.BarrelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarrelViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_barrel, parent, false)
        return BarrelViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarrelViewHolder, position: Int) {
        val barrel = barrels[position]

        holder.txtNomBarrel.text = barrel.name
        holder.txtBarrelDetails.text = "${barrel.brand} • ${barrel.woodType} • ${barrel.volume}L"

        // Chevron + animation
        holder.layoutToggleHistorique.setOnClickListener {
            holder.isExpanded = !holder.isExpanded
            holder.layoutHistorique.visibility =
                if (holder.isExpanded) View.VISIBLE else View.GONE

            holder.imgChevron.animate()
                .rotation(if (holder.isExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }

        // Suppression fût
        holder.btnDeleteBarrel.setOnClickListener {
            confirmDeleteBarrel(barrel)
        }

        holder.btnEditBarrel.setOnClickListener {
            onEditBarrel(barrel)
        }

        holder.btnAddHistorique.setOnClickListener {
            onAddHistory(barrel.id)
        }

        holder.photoOverlay.setOnClickListener {
            onEditPhoto(barrel)
        }

        if (barrel.imagePath == null) {
            holder.photoOverlay.visibility = View.VISIBLE
            holder.imgBarrel.setImageResource(R.drawable.ic_barrel_placeholder)
        } else {
            holder.photoOverlay.visibility = View.GONE
            holder.imgBarrel.setImageBitmap(
                BitmapFactory.decodeFile(barrel.imagePath)
            )
        }

        afficherHistoriques(holder, barrel)
    }

    override fun getItemCount() = barrels.size

    private fun confirmDeleteBarrel(barrel: Barrel) {
        AlertDialog.Builder(context)
            .setTitle("Supprimer le fût")
            .setMessage(
                "Voulez-vous vraiment supprimer ce fût ?\n\n" +
                        "Tous les historiques associés seront définitivement supprimés."
            )
            .setPositiveButton("Supprimer") { _, _ ->
                val dbHelper = DatabaseHelper(context)
                val barrelDao = BarrelDao(dbHelper)

                barrelDao.deleteBarrel(barrel.id)

                // Rafraîchit la liste après suppression
                refresh()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    fun updateData(newBarrels: List<Barrel>) {
        this.barrels = newBarrels
        notifyDataSetChanged()
    }

    inner class BarrelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val txtNomBarrel: TextView =
            itemView.findViewById(R.id.txtBarrelName)

        val txtBarrelDetails: TextView =
            itemView.findViewById(R.id.txtBarrelDetails)

        val btnEditBarrel: ImageButton =
            itemView.findViewById(R.id.btnEditBarrel)

        val btnDeleteBarrel: ImageButton =
            itemView.findViewById(R.id.btnDeleteBarrel)

        // Bouton ajout historique
        val btnAddHistorique: TextView =
            itemView.findViewById(R.id.btnAddHistorique)

        // Zone cliquable "Historique + chevron"
        val layoutToggleHistorique: LinearLayout =
            itemView.findViewById(R.id.layoutToggleHistorique)

        // Chevron animé
        val imgChevron: ImageView =
            itemView.findViewById(R.id.imgChevron)

        val imgBarrel: ImageView =
            itemView.findViewById(R.id.imgBarrel)

        val photoOverlay: LinearLayout =
            itemView.findViewById(R.id.photoOverlay)

        // Conteneur des historiques
        val layoutHistorique: LinearLayout =
            itemView.findViewById(R.id.layoutHistorique)

        // Permet de savoir si l’historique est ouvert ou non
        var isExpanded: Boolean = false

    }

    private fun afficherHistoriques(
        holder: BarrelViewHolder,
        barrel: Barrel
    ) {
        holder.layoutHistorique.removeAllViews()

        if (barrel.histories.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = "Aucun historique"
            emptyText.setTextColor(Color.GRAY)
            emptyText.setPadding(16, 8, 16, 8)
            holder.layoutHistorique.addView(emptyText)
            return
        }

        for (history in barrel.histories) {

            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_historique, holder.layoutHistorique, false)

            val txtNom = view.findViewById<TextView>(R.id.txtNom)
            val txtDuration = view.findViewById<TextView>(R.id.txtDuration)
            val txtDates = view.findViewById<TextView>(R.id.txtDates)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteHistorique)

            // Nom
            txtNom.text = history.name

            // Durée
            txtDuration.text = calculateDuration(
                history.beginDate,
                history.endDate
            )

            // Dates
            val dateDebut = formatDate(history.beginDate)
            val dateFin = history.endDate?.let {
                formatDate(it)
            } ?: "en cours"

            txtDates.text = "Début : $dateDebut • Fin : $dateFin"

            // Suppression
            btnDelete.setOnClickListener {
                confirmDeleteHistorique(history)
            }

            // Animation d'apparition
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()

            holder.layoutHistorique.addView(view)
        }
    }


    private fun confirmDeleteHistorique(historique: History) {
        AlertDialog.Builder(context)
            .setTitle("Supprimer l'historique")
            .setMessage("Voulez-vous vraiment supprimer cet historique ?")
            .setPositiveButton("Supprimer") { _, _ ->
                val dbHelper = DatabaseHelper(context)
                val historiqueDao = HistoryDao(dbHelper)

                historiqueDao.deleteHistory(historique.id)

                // Recharge les données après suppression
                refresh()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
