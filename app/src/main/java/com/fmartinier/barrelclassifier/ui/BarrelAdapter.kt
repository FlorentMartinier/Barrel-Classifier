package com.fmartinier.barrelclassifier.ui

import android.content.Context
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
    private val onAddHistory: (Long) -> Unit
) : RecyclerView.Adapter<BarrelAdapter.BarrelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarrelViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_barrel, parent, false)
        return BarrelViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarrelViewHolder, position: Int) {
        val barrel = barrels[position]

        holder.txtNomBarrel.text = barrel.name

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

        holder.btnAddHistorique.setOnClickListener {
            onAddHistory(barrel.id)
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

        /* ========================
           Image & infos du fût
           ======================== */

        val imgBarrel: ImageView =
            itemView.findViewById(R.id.imgFut)

        val txtNomBarrel: TextView =
            itemView.findViewById(R.id.txtNomFut)

        val txtInfosBarrel: TextView =
            itemView.findViewById(R.id.txtInfos)

        /* ========================
           Actions fût
           ======================== */

        // Bouton suppression fût (poubelle rouge)
        val btnDeleteBarrel: ImageButton =
            itemView.findViewById(R.id.btnDeleteFut)

        // Bouton ajout historique
        val btnAddHistorique: Button =
            itemView.findViewById(R.id.btnAddHistorique)

        /* ========================
           Historique déroulant
           ======================== */

        // Zone cliquable "Historique + chevron"
        val layoutToggleHistorique: LinearLayout =
            itemView.findViewById(R.id.layoutToggle)

        // Chevron animé
        val imgChevron: ImageView =
            itemView.findViewById(R.id.imgChevron)

        // Conteneur des historiques
        val layoutHistorique: LinearLayout =
            itemView.findViewById(R.id.layoutHistorique)

        /* ========================
           État interne
           ======================== */

        // Permet de savoir si l’historique est ouvert ou non
        var isExpanded: Boolean = false
    }

    private fun afficherHistoriques(
        holder: BarrelViewHolder,
        barrel: Barrel
    ) {
        holder.layoutHistorique.removeAllViews()

        if (barrel.histories.isEmpty()) {
            val empty = TextView(context)
            empty.text = "Aucun historique"
            empty.setPadding(8, 8, 8, 8)
            holder.layoutHistorique.addView(empty)
            return
        }

        for (historique in barrel.histories) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_historique, holder.layoutHistorique, false)

            val txtNom = view.findViewById<TextView>(R.id.txtNom)
            val txtDetails = view.findViewById<TextView>(R.id.txtDetails)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteHistorique)

            txtNom.text = historique.name

            val finTexte = historique.endDate?.let {
                formatDate(it)
            } ?: "en cours de vieillissement"

            txtDetails.text =
                "Début : ${formatDate(historique.beginDate)}\n" +
                        "Fin : $finTexte\n" +
                        "Durée : ${calculateDuration(historique.beginDate, historique.endDate)}"

            btnDelete.setOnClickListener {
                confirmDeleteHistorique(historique)
            }

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
