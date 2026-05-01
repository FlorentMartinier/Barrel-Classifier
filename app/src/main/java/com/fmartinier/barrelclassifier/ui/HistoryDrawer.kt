package com.fmartinier.barrelclassifier.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.enums.EHistoryType
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.BarrelService
import com.fmartinier.barrelclassifier.service.HistoryService
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.ui.model.BarrelViewHolder
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculate228lEquivalentAge
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateNbDaysBetweenDates
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDaysToDurationString
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.getEquivalenceRatio
import com.fmartinier.barrelclassifier.utils.TextViewUtils
import com.fmartinier.barrelclassifier.utils.TooltipUtils.Companion.createTooltip
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class HistoryDrawer(
    val context: Context,
    val refresh: () -> Unit,
    val onTakeHistoryPicture: (History) -> Unit,
    val onImportHistoryPicture: (History) -> Unit,
    val fragmentManager: FragmentManager
) {

    val dbHelper = DatabaseHelper.getInstance(context)
    val historyService = HistoryService(context, dbHelper)
    val imageService = ImageService()
    val barrelService = BarrelService(context, dbHelper)

    fun displayAllForBarrel(
        holder: BarrelViewHolder,
        barrel: Barrel
    ) {
        holder.layoutHistory?.removeAllViews()

        if (barrel.histories.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = context.resources.getString(R.string.no_history)
            emptyText.setTextColor(Color.GRAY)
            emptyText.setPadding(16, 8, 16, 8)
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f)
            holder.layoutHistory?.addView(emptyText)
            return
        }

        for (history in barrel.histories) {

            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_history, holder.layoutHistory, false)

            val txtName = view.findViewById<TextView>(R.id.txtName)
            val txtDuration = view.findViewById<TextView>(R.id.txtDuration)
            val txtDurationEquivalent = view.findViewById<TextView>(R.id.txtDurationEquivalent)
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
            val img = view.findViewById<ImageView>(R.id.imgHistory)

            val historyType = getHistoryType(history)
            txtName.text = "${history.name} · $historyType"

            val nbDays = calculateNbDaysBetweenDates(history.beginDate, history.endDate)
            txtDuration.text = formatDaysToDurationString(context, nbDays)
            manageDurationEquivalence(nbDays, barrel.volume.toDouble(), txtDurationEquivalent)

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

            TextViewUtils.convertToDetailedDescription(context, txtDescription, txtExpandDescription, description)

            btnMenu.setOnClickListener {
                val popup = PopupMenu(it.context, it)
                popup.inflate(R.menu.history_item_menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            barrelService.openAddHistoryDialog(barrel, history.id, fragmentManager)
                            true
                        }

                        R.id.action_delete -> {
                            confirmDeleteHistory(history)
                            true
                        }

                        R.id.action_take_picture -> {
                            onTakeHistoryPicture(history)
                            true
                        }

                        R.id.action_import_image -> {
                            onImportHistoryPicture(history)
                            true
                        }

                        else -> false
                    }
                }

                popup.show()
            }

            if (!history.imagePath.isNullOrEmpty()) {
                val file = File(history.imagePath)

                if (file.exists()) {
                    img.visibility = View.VISIBLE
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    img.setImageBitmap(bmp)

                    img.setOnClickListener {
                        imageService.showImageFullscreen(it.context, history.imagePath)
                    }
                } else {
                    img.visibility = View.GONE
                }
            } else {
                img.visibility = View.GONE
            }

            holder.layoutHistory?.addView(view)

            // Affichage des actions effectuées
            val alerts = history.alerts
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

    private fun manageDurationEquivalence(nbDays: Int, barrelVolume: Double, txtDurationEquivalent: TextView) {
        if (barrelVolume >= 180) {
            txtDurationEquivalent.visibility = View.GONE
            return
        }
        val daysEquivalent = calculate228lEquivalentAge(nbDays, barrelVolume)
        val durationString = formatDaysToDurationString(context, daysEquivalent)
        val message = context.resources.getString(R.string.equivalence_228, durationString) + "  "
        val spannable = SpannableStringBuilder(message)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        val infoIcon = ContextCompat.getDrawable(context, R.drawable.ic_info_outline)?.apply {
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                14f,
                context.resources.displayMetrics
            ).toInt()
            setBounds(0, 0, size, size)
            setTint(typedValue.data)
        }
        if (infoIcon != null) {
            val imageSpan = ImageSpan(infoIcon, ImageSpan.ALIGN_BASELINE)
            // On attache l'image sur le dernier caractère (l'espace)
            spannable.setSpan(
                imageSpan,
                message.length - 1,
                message.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        txtDurationEquivalent.text = spannable
        val ratio = getEquivalenceRatio(barrelVolume)

        val tooltipMessage = context.getString(
            R.string.duration_equivalence_tooltip,
            barrelVolume.toString(),
            String.format("%.1f", ratio)
        )

        val tooltip = createTooltip(context, tooltipMessage)

        txtDurationEquivalent.setOnClickListener { view ->
            tooltip.showAlignTop(view)
        }
    }

    private fun getHistoryType(history: History): String {
        return when (history.type) {
            context.resources.getString(EHistoryType.AGING.historyTypeDescription) -> "${history.type} ⌛"
            context.resources.getString(EHistoryType.SEASONNING.historyTypeDescription) -> "${history.type} 🪵"
            context.resources.getString(EHistoryType.MIX.historyTypeDescription) -> "${history.type} 🔄"
            else -> history.type
        }
    }

    private fun confirmDeleteHistory(history: History) {
        MaterialAlertDialogBuilder(context)
            .setBackground(AppCompatResources.getDrawable(context, R.color.dialog_bg))
            .setTitle(context.resources.getString(R.string.remove_history))
            .setMessage(context.resources.getString(R.string.remove_history_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                historyService.delete(history)
                refresh()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .show()
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
}