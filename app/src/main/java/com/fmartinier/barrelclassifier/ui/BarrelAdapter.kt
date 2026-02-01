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
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.AlertDao
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.AlertService
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateDuration
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate

class BarrelAdapter(
    private val context: Context,
    private var barrels: List<Barrel>,
    private val refresh: () -> Unit,
    private val onAddHistory: (Barrel, Long?) -> Unit,
    private val onEditBarrel: (Barrel) -> Unit,
    private val onEditPhoto: (Barrel) -> Unit
) : RecyclerView.Adapter<BarrelAdapter.BarrelViewHolder>() {

    val dbHelper = DatabaseHelper(context)
    val historyDao = HistoryDao(dbHelper)
    val alertDao = AlertDao(dbHelper)
    val alertService = AlertService()

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
            onAddHistory(barrel, null)
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
            val btnMenu = view.findViewById<ImageButton>(R.id.btnMenu)
            val actionsSection = view.findViewById<LinearLayout>(R.id.actionsSection)
            val alertsSection = view.findViewById<LinearLayout>(R.id.alertsSection)
            val alertsContainer: LinearLayout =
                view.findViewById(R.id.historyAlertsContainer)
            val actionsContainer: LinearLayout =
                view.findViewById(R.id.historyActionsContainer)

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

            btnMenu.setOnClickListener {
                val popup = PopupMenu(it.context, it)
                popup.inflate(R.menu.history_item_menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onAddHistory(barrel, history.id)
                            true
                        }
                        R.id.action_delete -> {
                            confirmDeleteHistory(history)
                            true
                        }
                        else -> false
                    }
                }

                popup.show()
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
            val alerts = AlertDao(DatabaseHelper(context)).getByHistoryId(history.id)

            // Affichage des actions effectuées
            val doneActions = alerts.filter { it.date < System.currentTimeMillis() }
            displayAlerts(
                actionsSection,
                actionsContainer,
                doneActions,
                "✅"
            )

            // Affichage des alertes à venir
            val futureAlerts = alerts.filter { it.date >= System.currentTimeMillis() }
            displayAlerts(
                alertsSection,
                alertsContainer,
                futureAlerts,
                "📆"
            )
        }
    }

    private fun displayAlerts(
        section: LinearLayout,
        container: LinearLayout,
        alerts: List<Alert>,
        icon: String
    ) {
        container.removeAllViews()

        if (alerts.isEmpty()) {
            section.visibility = View.GONE
        } else {
            section.visibility = View.VISIBLE

            alerts
                .forEach { alert ->
                    val alertView = LayoutInflater.from(context).inflate(
                        R.layout.item_alert,
                        container,
                        false
                    )

                    alertView.findViewById<TextView>(R.id.txtAlertType).text = "$icon ${alert.type}"
                    alertView.findViewById<TextView>(R.id.txtAlertDate).text =
                        formatDate(alert.date)

                    container.addView(alertView)
                }
        }
    }

    private fun confirmDeleteHistory(history: History) {
        AlertDialog.Builder(context)
            .setTitle(context.resources.getString(R.string.remove_history))
            .setMessage(context.resources.getString(R.string.remove_history_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                val historyId = history.id
                alertDao.getByHistoryId(historyId).forEach {
                    alertService.cancelByAlertId(context, it.id)
                }
                historyDao.delete(historyId)

                // Recharge les données après suppression
                refresh()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .show()
    }
}
