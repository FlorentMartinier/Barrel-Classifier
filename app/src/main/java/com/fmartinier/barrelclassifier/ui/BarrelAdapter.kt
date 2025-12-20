package com.fmartinier.barrelclassifier.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        holder.txtBarrelName.text = barrel.name
        holder.txtBarrelDetails.text = "${barrel.brand} • ${barrel.woodType} • ${barrel.volume}L"

        // Chevron + animation
        holder.layoutToggleHistory.setOnClickListener {
            holder.isExpanded = !holder.isExpanded
            holder.layoutHistory.visibility =
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

        holder.btnAddHistory.setOnClickListener {
            onAddHistory(barrel.id)
        }

        holder.photoOverlay.setOnClickListener {
            onEditPhoto(barrel)
        }

        holder.imgBarrel.setOnClickListener {
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

        displayHistories(holder, barrel)
    }

    override fun getItemCount() = barrels.size

    private fun confirmDeleteBarrel(barrel: Barrel) {
        AlertDialog.Builder(context)
            .setTitle(context.resources.getString(R.string.remove_barrel))
            .setMessage(context.resources.getString(R.string.remove_barrel_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                val dbHelper = DatabaseHelper(context)
                val barrelDao = BarrelDao(dbHelper)

                barrelDao.deleteBarrel(barrel.id)

                // Rafraîchit la liste après suppression
                refresh()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .show()
    }

    fun updateData(newBarrels: List<Barrel>) {
        this.barrels = newBarrels
        notifyDataSetChanged()
    }

    inner class BarrelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val txtBarrelName: TextView =
            itemView.findViewById(R.id.txtBarrelName)

        val txtBarrelDetails: TextView =
            itemView.findViewById(R.id.txtBarrelDetails)

        val btnEditBarrel: ImageButton =
            itemView.findViewById(R.id.btnEditBarrel)

        val btnDeleteBarrel: ImageButton =
            itemView.findViewById(R.id.btnDeleteBarrel)

        // Bouton ajout historique
        val btnAddHistory: TextView =
            itemView.findViewById(R.id.btnAddHistory)

        // Zone cliquable "Historique + chevron"
        val layoutToggleHistory: LinearLayout =
            itemView.findViewById(R.id.layoutToggleHistory)

        // Chevron animé
        val imgChevron: ImageView =
            itemView.findViewById(R.id.imgChevron)

        val imgBarrel: ImageView =
            itemView.findViewById(R.id.imgBarrel)

        val photoOverlay: LinearLayout =
            itemView.findViewById(R.id.photoOverlay)

        // Conteneur des historiques
        val layoutHistory: LinearLayout =
            itemView.findViewById(R.id.layoutHistory)

        // Permet de savoir si l’historique est ouvert ou non
        var isExpanded: Boolean = false

    }

    private fun displayHistories(
        holder: BarrelViewHolder,
        barrel: Barrel
    ) {
        holder.layoutHistory.removeAllViews()

        if (barrel.histories.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = context.resources.getString(R.string.no_history)
            emptyText.setTextColor(Color.GRAY)
            emptyText.setPadding(16, 8, 16, 8)
            holder.layoutHistory.addView(emptyText)
            return
        }

        for (history in barrel.histories) {

            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_history, holder.layoutHistory, false)

            val txtName = view.findViewById<TextView>(R.id.txtName)
            val txtDuration = view.findViewById<TextView>(R.id.txtDuration)
            val txtDates = view.findViewById<TextView>(R.id.txtDates)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteHistory)

            txtName.text = history.name

            txtDuration.text = calculateDuration(
                context,
                history.beginDate,
                history.endDate
            )

            val dateDebut = formatDate(history.beginDate)
            val dateFin = history.endDate?.let {
                formatDate(it)
            } ?: context.resources.getString(R.string.in_progress)

            txtDates.text = context.resources.getString(
                R.string.begin_and_end_date,
                dateDebut,
                dateFin
            )

            btnDelete.setOnClickListener {
                confirmDeleteHistory(history)
            }

            // Animation d'apparition
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()

            holder.layoutHistory.addView(view)
        }
    }


    private fun confirmDeleteHistory(history: History) {
        AlertDialog.Builder(context)
            .setTitle(context.resources.getString(R.string.remove_history))
            .setMessage(context.resources.getString(R.string.remove_history_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                val dbHelper = DatabaseHelper(context)
                val historyDao = HistoryDao(dbHelper)

                historyDao.deleteHistory(history.id)

                // Recharge les données après suppression
                refresh()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .show()
    }
}
