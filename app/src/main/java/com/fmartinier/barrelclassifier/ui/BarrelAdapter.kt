package com.fmartinier.barrelclassifier.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.BarrelService
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.service.PdfService
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate
import com.fmartinier.barrelclassifier.utils.FileUtils
import com.fmartinier.barrelclassifier.utils.TextViewUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.drive.DriveScopes

class BarrelAdapter(
    private val context: Context,
    var barrels: List<Barrel>,
    private val refresh: () -> Unit,
    private val onAddHistory: (Barrel, Long?) -> Unit,
    private val onEditBarrel: (Barrel) -> Unit,
    private val onTakeBarrelPicture: (Barrel) -> Unit,
    private val onImportBarrelPicture: (Barrel) -> Unit,
    private val onExportQrCloud: (Intent, Barrel) -> Unit,
    onTakeHistoryPicture: (History) -> Unit,
    onImportHistoryPicture: (History) -> Unit,
) : RecyclerView.Adapter<BarrelAdapter.BarrelViewHolder>() {

    val dbHelper = DatabaseHelper.getInstance(context)
    val barrelService = BarrelService(context, dbHelper)
    val imageService = ImageService()
    val historyDrawer =
        HistoryDrawer(context, refresh, onAddHistory, onTakeHistoryPicture, onImportHistoryPicture)
    val statisticsDrawer = StatisticsDrawer(context)

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarrelViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_barrel, parent, false)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Permission d'écrire des fichiers
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, options)

        return BarrelViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarrelViewHolder, position: Int) {
        val barrel = barrels[position]

        holder.txtBarrelName.text = barrel.name
        holder.txtBarrelDetails.text = "${barrel.brand} • ${barrel.woodType} • ${barrel.volume}L"

        holder.chipGroup.removeAllViews()

        barrel.histories
            .flatMap { it.alerts }
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


        holder.layoutToggleHistory.setOnClickListener {

            val view = holder.layoutHistory
            holder.isHistoryExpanded = !holder.isHistoryExpanded

            if (holder.isHistoryExpanded) {
                expand(view)
            } else {
                collapse(view)
            }

            // rotation chevron
            holder.imgChevronHistory.animate()
                .rotation(if (holder.isHistoryExpanded) 180f else 0f)
                .setDuration(250)
                .start()
        }

        holder.layoutToggleStats.setOnClickListener {

            val view = holder.layoutStats
            holder.isStatsExpanded = !holder.isStatsExpanded

            if (holder.isStatsExpanded) {
                expand(view)
            } else {
                collapse(view)
            }

            // rotation chevron
            holder.imgChevronStats.animate()
                .rotation(if (holder.isStatsExpanded) 180f else 0f)
                .setDuration(250)
                .start()
        }

        holder.btnMenu.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.inflate(R.menu.barrel_menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_barrel -> {
                        onEditBarrel(barrel)
                        true
                    }

                    R.id.action_delete_barrel -> {
                        confirmDeleteBarrel(barrel)
                        true
                    }

                    R.id.barrel_take_picture -> {
                        onTakeBarrelPicture(barrel)
                        true
                    }

                    R.id.barrel_import_image -> {
                        onImportBarrelPicture(barrel)
                        true
                    }

                    R.id.action_pdf_export -> {
                        val file = PdfService(context).export(barrel)
                        FileUtils.viewFile(context, file, FileUtils.PDF_TYPE)
                        true
                    }

                    R.id.action_qr_export -> {
                        onGenerateCloudQRClicked(barrel)
                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }

        holder.btnAddHistory.setOnClickListener {
            onAddHistory(barrel, null)
        }

        holder.photoOverlay.setOnClickListener {
            takePictureOrOpenImageOrBarrel(barrel)
        }

        holder.imgBarrel.setOnClickListener {
            takePictureOrOpenImageOrBarrel(barrel)
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
        TextViewUtils.convertToDetailedDescription(context, holder.txtDescription, holder.txtExpandDescription, barrel.description)

        historyDrawer.displayAllForBarrel(holder, barrel)
        statisticsDrawer.displayAllForBarrel(holder, barrel)
    }

    override fun getItemCount() = barrels.size

    private fun takePictureOrOpenImageOrBarrel(barrel: Barrel) {
        val barrelImagePath = barrel.imagePath
        if (barrelImagePath == null) {
            onTakeBarrelPicture(barrel)
        } else {
            imageService.showImageFullscreen(context, barrelImagePath)
        }
    }

    private fun confirmDeleteBarrel(barrel: Barrel) {
        MaterialAlertDialogBuilder(context)
            .setBackground(context, R.color.dialog_bg)
            .setTitle(context.resources.getString(R.string.remove_barrel))
            .setMessage(context.resources.getString(R.string.remove_barrel_validation))
            .setPositiveButton(context.resources.getString(R.string.remove)) { _, _ ->
                barrelService.delete(barrel)

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
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnBarrelMenu)
        val btnAddHistory: TextView = itemView.findViewById(R.id.btnAddHistory)
        val layoutToggleStats: LinearLayout = itemView.findViewById(R.id.layoutToggleStats)
        val layoutToggleHistory: LinearLayout = itemView.findViewById(R.id.layoutToggleHistory)
        val imgChevronStats: ImageView = itemView.findViewById(R.id.imgChevronStats)
        val imgChevronHistory: ImageView = itemView.findViewById(R.id.imgChevronHistory)
        val imgBarrel: ImageView = itemView.findViewById(R.id.imgBarrel)
        val photoOverlay: LinearLayout = itemView.findViewById(R.id.photoOverlay)
        val layoutStats: LinearLayout = itemView.findViewById(R.id.layoutStats)
        val layoutHistory: LinearLayout = itemView.findViewById(R.id.layoutHistory)
        val chipGroup: ChipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroupAdvanced)
        val txtNextAlertDate: TextView = itemView.findViewById<TextView>(R.id.txtNextAlertDate)
        val layoutNextAlert: LinearLayout =
            itemView.findViewById<LinearLayout>(R.id.layoutNextAlert)

        var isHistoryExpanded: Boolean = false
        var isStatsExpanded: Boolean = false
        val txtDescription: TextView = itemView.findViewById(R.id.txtBarrelDescription)
        val txtExpandDescription: TextView = itemView.findViewById(R.id.txtBarrelExpandDescription)
    }

    fun addChip(holder: BarrelViewHolder, text: String, icon: String) {
        val chip = Chip(holder.itemView.context).apply {
            this.text = "$icon $text"
            isChipIconVisible = true
            isClickable = false
            isCheckable = false
            setEnsureMinTouchTargetSize(false)
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.chip_bg)
            )
            setTextColor(ContextCompat.getColor(context, R.color.chip_text))
            chipStrokeWidth = 0f
        }
        holder.chipGroup.addView(chip)
    }

    private fun expand(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight

        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.duration = 280
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.doOnEnd {
            view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            view.requestLayout()
        }
        animator.start()
    }

    private fun collapse(view: View) {
        val initialHeight = view.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.duration = 220
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }

        animator.doOnEnd {
            view.visibility = View.GONE
        }

        animator.start()
    }

    private fun onGenerateCloudQRClicked(barrel: Barrel) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.generate_qr_cloud))
            .setMessage(context.getString(R.string.generate_qr_cloud_confirm))
            .setNegativeButton(context.getString(R.string.refuse)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(context.getString(R.string.accept)) { _, _ ->
                startGoogleAuthProcess(barrel) // Lance la connexion Google puis l'upload
            }
            .show()
    }

    private fun startGoogleAuthProcess(barrel: Barrel) {
        val signInIntent = googleSignInClient.signInIntent
        onExportQrCloud(signInIntent, barrel)
    }
}
