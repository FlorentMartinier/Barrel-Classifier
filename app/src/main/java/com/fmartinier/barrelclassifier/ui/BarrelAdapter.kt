package com.fmartinier.barrelclassifier.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.fmartinier.barrelclassifier.data.enums.EHistoryType
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.AlertService
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateDuration
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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
        holder.chipGroup.removeAllViews()

        barrel.histories
            .flatMap { alertDao.getByHistoryId(it.id) }
            .filter { it.date > System.currentTimeMillis() }
            .minByOrNull { it.date }
            ?.let {
                holder.layoutNextAlert.visibility = View.VISIBLE
                val nextAlertDate = formatDate(it.date)
                holder.txtNextAlertDate.text = context.resources.getString(
                    R.string.next_alert,
                    it.type,
                    nextAlertDate
                )
            }

        var hasAdvanced = false

        barrel.heatType?.takeIf { it.isNotBlank() }?.let {
            addChip(holder, it, "🔥")
            hasAdvanced = true
        }

        barrel.storageHygrometer?.takeIf { it.isNotBlank() }?.let {
            addChip(holder, "$it%", "💧")
            hasAdvanced = true
        }

        barrel.storageTemperature?.takeIf { it.isNotBlank() }?.let {
            addChip(holder, "${it}°C", "🌡️")
            hasAdvanced = true
        }

        holder.chipGroup.visibility = if (hasAdvanced) View.VISIBLE else View.GONE


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

        val txtBarrelName: TextView = itemView.findViewById(R.id.txtBarrelName)
        val txtBarrelDetails: TextView = itemView.findViewById(R.id.txtBarrelDetails)
        val btnEditBarrel: ImageButton = itemView.findViewById(R.id.btnEditBarrel)
        val btnDeleteBarrel: ImageButton = itemView.findViewById(R.id.btnDeleteBarrel)
        val btnAddHistory: TextView = itemView.findViewById(R.id.btnAddHistory)
        val layoutToggleHistory: LinearLayout = itemView.findViewById(R.id.layoutToggleHistory)
        val imgChevron: ImageView = itemView.findViewById(R.id.imgChevron)
        val imgBarrel: ImageView = itemView.findViewById(R.id.imgBarrel)
        val photoOverlay: LinearLayout = itemView.findViewById(R.id.photoOverlay)
        val layoutHistory: LinearLayout = itemView.findViewById(R.id.layoutHistory)
        val chipGroup: ChipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroupAdvanced)
        val txtNextAlertDate: TextView = itemView.findViewById<TextView>(R.id.txtNextAlertDate)
        val layoutNextAlert: LinearLayout = itemView.findViewById<LinearLayout>(R.id.layoutNextAlert)

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
            val alertsContainer: LinearLayout = view.findViewById(R.id.historyAlertsContainer)
            val actionsContainer: LinearLayout = view.findViewById(R.id.historyActionsContainer)
            val chipAngels: Chip = view.findViewById(R.id.chipAngels)
            val chipAlcohol: Chip = view.findViewById(R.id.chipAlcohol)
            val txtDescription: TextView = view.findViewById(R.id.txtDescription)
            val txtExpandDescription: TextView = view.findViewById(R.id.txtExpandDescription)

            val historyType = getHistoryType(history)
            txtName.text = "${history.name} · $historyType"

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

            history.angelsShare
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    chipAngels.visibility = View.VISIBLE
                    chipAngels.text = "👼 ${history.angelsShare}%"
                }

            history.alcoholicStrength
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    chipAlcohol.visibility = View.VISIBLE
                    chipAlcohol.text = "🥃 ${history.alcoholicStrength}%"
                }

            val description = history.description

            if (description.isNullOrBlank()) {
                txtDescription.visibility = View.GONE
                txtExpandDescription.visibility = View.GONE
            } else {
                txtDescription.text = description
                txtDescription.visibility = View.INVISIBLE
                txtExpandDescription.visibility = View.GONE

                txtDescription.maxLines = Integer.MAX_VALUE
                txtDescription.ellipsize = null
            }

            txtDescription.afterMeasured {

                val lineCount = txtDescription.lineCount

                if (lineCount <= 1) {
                    txtExpandDescription.visibility = View.GONE
                } else {
                    txtExpandDescription.visibility = View.VISIBLE
                    txtExpandDescription.text = txtDescription.context.getString(R.string.see_more)
                    txtDescription.maxLines = 1
                    txtDescription.ellipsize = TextUtils.TruncateAt.END
                }

                txtDescription.visibility = View.VISIBLE
            }

            txtExpandDescription.setOnClickListener {

                val expanded = txtDescription.maxLines > 1

                if (expanded) {
                    // collapse
                    val startHeight = txtDescription.height

                    txtDescription.maxLines = 1
                    txtDescription.ellipsize = TextUtils.TruncateAt.END
                    txtDescription.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            txtDescription.width,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.UNSPECIFIED
                    )
                    val endHeight = txtDescription.measuredHeight

                    animateTextViewHeight(txtDescription, startHeight, endHeight)

                    txtExpandDescription.text = context.getString(R.string.see_more)

                } else {
                    // expand
                    val startHeight = txtDescription.height

                    txtDescription.maxLines = Int.MAX_VALUE
                    txtDescription.ellipsize = null
                    txtDescription.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            txtDescription.width,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.UNSPECIFIED
                    )
                    val endHeight = txtDescription.measuredHeight

                    animateTextViewHeight(txtDescription, startHeight, endHeight)

                    txtExpandDescription.text = context.getString(R.string.see_less)
                }
            }

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

    fun addChip(holder: BarrelViewHolder, text: String, icon: String) {
        val chip = Chip(holder.itemView.context).apply {
            this.text = "$icon $text"
            isChipIconVisible = true
            isClickable = false
            isCheckable = false
            setEnsureMinTouchTargetSize(false)
        }
        holder.chipGroup.addView(chip)
    }

    fun TextView.afterMeasured(block: () -> Unit) {
        if (width > 0) {
            block()
            return
        }

        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }

    private fun animateTextViewHeight(view: TextView, start: Int, end: Int, duration: Long = 220) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = duration
        animator.interpolator = android.view.animation.DecelerateInterpolator()

        animator.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }

        animator.start()
    }

    private fun getHistoryType(history: History): String {
        return when (history.type) {
            context.resources.getString(EHistoryType.AGING.historyTypeDescription) -> "${history.type} ⌛"
            context.resources.getString(EHistoryType.SEASONNING.historyTypeDescription) -> "${history.type} 🪵"
            context.resources.getString(EHistoryType.MIX.historyTypeDescription) -> "${history.type} 🔄"
            else -> history.type
        }
    }
}
