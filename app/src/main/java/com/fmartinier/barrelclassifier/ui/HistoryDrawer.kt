package com.fmartinier.barrelclassifier.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.enums.EHistoryType
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.HistoryService
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.ui.BarrelAdapter.BarrelViewHolder
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateDuration
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class HistoryDrawer(
    val context: Context,
    private val refresh: () -> Unit,
    private val onAddHistory: (Barrel, Long?) -> Unit,
    private val onTakeHistoryPicture: (History) -> Unit,
    private val onImportHistoryPicture: (History) -> Unit,
) {

    val dbHelper = DatabaseHelper.getInstance(context)
    val historyService = HistoryService(context, dbHelper)
    val imageService = ImageService()

    fun displayAllForBarrel(
        holder: BarrelViewHolder,
        barrel: Barrel
    ) {
        holder.layoutHistory.removeAllViews()

        if (barrel.histories.isEmpty()) {
            val emptyText = TextView(context)
            emptyText.text = context.resources.getString(R.string.no_history)
            emptyText.setTextColor(Color.GRAY)
            emptyText.setPadding(16, 8, 16, 8)
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f)
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
            val img = view.findViewById<ImageView>(R.id.imgHistory)

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

            holder.layoutHistory.addView(view)

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
            .setBackground(context, R.color.dialog_bg)
            .setTitle(context.resources.getString(R.string.remove_history))
            .setMessage(context.resources.getString(R.string.remove_history_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                historyService.delete(history)
                refresh()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .show()
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