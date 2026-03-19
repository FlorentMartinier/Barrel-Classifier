package com.fmartinier.barrelclassifier.ui

import android.animation.ValueAnimator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.BarrelService
import com.fmartinier.barrelclassifier.service.ImageService
import com.fmartinier.barrelclassifier.service.PdfService
import com.fmartinier.barrelclassifier.service.QrCloudService
import com.fmartinier.barrelclassifier.ui.model.BarrelViewHolder
import com.fmartinier.barrelclassifier.utils.DateUtils
import com.fmartinier.barrelclassifier.utils.FileUtils
import com.fmartinier.barrelclassifier.utils.TextViewUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BarrelFullViewBinder(
    private val refresh: () -> Unit,
    val fragmentManager: FragmentManager,
    val activity: ComponentActivity,
) {

    private val dbHelper = DatabaseHelper.getInstance(activity)
    private val barrelService = BarrelService(activity, dbHelper)
    private val imageService = ImageService()
    private val historyDrawer = HistoryDrawer(
        context = activity,
        refresh = { refresh() },
        onTakeHistoryPicture = { history ->
            onTakeHistoryPicture(history)
        },
        onImportHistoryPicture = { history ->
            onImportHistoryPicture(history)
        },
        fragmentManager = this@BarrelFullViewBinder.fragmentManager
    )
    private val statisticsDrawer = StatisticsDrawer(activity)
    private val googleSignInClient: GoogleSignInClient
    private var barrelDao = BarrelDao.getInstance(dbHelper)
    private var historyDao = HistoryDao.getInstance(dbHelper)
    private var qrCloudService: QrCloudService = QrCloudService(activity.applicationContext)

    private var currentBarrelPhotoPath: String? = null
    private var currentHistoryPhotoPath: String? = null
    private var barrelForIntent: Barrel? = null
    private var historyForPhoto: History? = null

    private var barrelCameraLauncher: ActivityResultLauncher<Intent>
    private var historyCameraLauncher: ActivityResultLauncher<Intent>
    private var pickHistoryImageLauncher: ActivityResultLauncher<String>
    private var pickBarrelImageLauncher: ActivityResultLauncher<String>
    private var importQrLauncher: ActivityResultLauncher<Intent>

    init {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, options)

        barrelCameraLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    currentBarrelPhotoPath?.let { path ->
                        barrelForIntent?.let { barrel ->
                            val oldImagePath = barrel.imagePath
                            barrelDao.updateImage(barrel.id, path)
                            imageService.deleteImageIfExist(oldImagePath)
                            refresh()
                        }
                    }
                }
            }

        historyCameraLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    currentHistoryPhotoPath?.let { path ->
                        historyForPhoto?.let { history ->
                            val oldImagePath = history.imagePath
                            historyDao.updateImage(history.id, path)
                            imageService.deleteImageIfExist(oldImagePath)
                            refresh()
                        }
                    }
                }
            }

        pickHistoryImageLauncher =
            activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val path = imageService.copyImageToInternalStorage(it, activity)
                    currentHistoryPhotoPath = path
                    historyForPhoto?.let { history ->
                        val oldImagePath = history.imagePath
                        historyDao.updateImage(history.id, path)
                        imageService.deleteImageIfExist(oldImagePath)
                    }
                }
                refresh()
            }

        pickBarrelImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val path = imageService.copyImageToInternalStorage(it, activity)
                currentBarrelPhotoPath = path
                barrelForIntent?.let { barrel ->
                    val oldImagePath = barrel.imagePath
                    barrelDao.updateImage(barrel.id, path)
                    imageService.deleteImageIfExist(oldImagePath)
                }
            }
            refresh()
        }

        importQrLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {

                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.result
                if (account != null) {
                    barrelForIntent?.let { barrel ->
                        qrCloudService.showLoadingDialog(activity)
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            qrCloudService.processCloudQR(account, barrel, activity)
                        }
                    }
                } else {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.error_google_connexion),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.error_google_authent, e.statusCode.toString()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun bind(holder: BarrelViewHolder, barrel: Barrel) {
        holder.txtBarrelName.text = barrel.name
        holder.imgBarrel.setImageBitmap(barrel.imagePath?.let { BitmapFactory.decodeFile(it) }
            ?: BitmapFactory.decodeResource(activity.resources, R.drawable.ic_barrel_placeholder))

        holder.txtBarrelDetails?.text = "${barrel.brand} • ${barrel.woodType} • ${barrel.volume}L"
        holder.chipGroup?.removeAllViews()
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
        holder.chipGroup?.visibility = if (hasAdvanced) View.VISIBLE else View.GONE

        barrel.histories
            .flatMap { it.alerts }
            .filter { it.date > System.currentTimeMillis() }
            .minByOrNull { it.date }
            ?.let {
                holder.layoutNextAlert?.visibility = View.VISIBLE
                val nextAlertDate = DateUtils.formatDate(it.date)
                holder.txtNextAlertDate?.text = activity.resources.getString(
                    R.string.next_alert,
                    it.type,
                    nextAlertDate
                )
            }

        holder.btnMenu?.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.barrel_menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_barrel -> {
                        barrelService.openEditBarrel(barrel,
                            this@BarrelFullViewBinder.fragmentManager
                        )
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
                    R.id.barrel_import_image -> { onImportBarrelPicture(barrel); true }
                    R.id.action_pdf_export -> { FileUtils.viewFile(activity, PdfService(activity).export(barrel), FileUtils.PDF_TYPE); true }
                    R.id.action_qr_export -> { onExportQrCloud(googleSignInClient.signInIntent, barrel); true }
                    else -> false
                }
            }
            popup.show()
        }

        // Actions
        holder.btnAddHistory?.setOnClickListener {
            barrelService.openAddHistoryDialog(barrel, null,
                this@BarrelFullViewBinder.fragmentManager
            )
        }
        holder.photoOverlay?.setOnClickListener { takePictureOrOpenImage(barrel) }
        holder.imgBarrel.setOnClickListener { takePictureOrOpenImage(barrel) }

        if (barrel.imagePath == null) {
            holder.photoOverlay?.visibility = View.VISIBLE
            holder.imgBarrel.setImageResource(R.drawable.ic_barrel_placeholder)
        } else {
            holder.photoOverlay?.visibility = View.GONE
            holder.imgBarrel.setImageBitmap(
                BitmapFactory.decodeFile(barrel.imagePath)
            )
        }

        // Stats & History
        historyDrawer.displayAllForBarrel(holder, barrel)
        statisticsDrawer.displayAllForBarrel(holder, barrel)

        holder.layoutToggleHistory?.setOnClickListener {
            toggleSection(holder.layoutHistory!!, holder.imgChevronHistory!!, holder.isHistoryExpanded) { holder.isHistoryExpanded = it }
        }
        holder.layoutToggleStats?.setOnClickListener {
            toggleSection(holder.layoutStats!!, holder.imgChevronStats!!, holder.isStatsExpanded) { holder.isStatsExpanded = it }
        }

        if (holder.txtDescription != null && holder.txtExpandDescription != null) {
            TextViewUtils.convertToDetailedDescription(activity, holder.txtDescription, holder.txtExpandDescription, barrel.description)
        }
    }

    private fun takePictureOrOpenImage(barrel: Barrel) {
        if (barrel.imagePath == null) {
            onTakeBarrelPicture(barrel)
        } else {
            imageService.showImageFullscreen(activity, barrel.imagePath)
        }
    }

    private fun confirmDeleteBarrel(barrel: Barrel) {
        MaterialAlertDialogBuilder(activity).setTitle(R.string.remove_barrel).setMessage(R.string.remove_barrel_validation)
            .setPositiveButton(R.string.remove) { _, _ ->
                barrelService.delete(barrel)
                refresh()
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private fun toggleSection(view: View, chevron: ImageView, isExpanded: Boolean, updateState: (Boolean) -> Unit) {
        if (isExpanded) collapse(view) else expand(view)
        chevron.animate().rotation(if (isExpanded) 0f else 180f).setDuration(250).start()
        updateState(!isExpanded)
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

    private fun onImportBarrelPicture(barrel: Barrel) {
        barrelForIntent = barrel
        pickBarrelImageLauncher.launch("image/*")
    }

    private fun onTakeBarrelPicture(barrel: Barrel) {
        barrelForIntent = barrel
        val photoFile = imageService.createImageFile(activity)
        currentBarrelPhotoPath = photoFile.absolutePath
        barrelCameraLauncher.launch(imageService.takePhoto(activity, photoFile))
    }

    private fun onTakeHistoryPicture(history: History) {
        takePhotoForHistory(history)
    }

    private fun takePhotoForHistory(history: History) {
        historyForPhoto = history
        val photoFile = imageService.createImageFile(activity)
        currentHistoryPhotoPath = photoFile.absolutePath
        historyCameraLauncher.launch(imageService.takePhoto(activity, photoFile))
    }

    private fun onExportQrCloud (intent: Intent, barrel: Barrel) {
        barrelForIntent = barrel
        importQrLauncher.launch(intent)
    }

    private fun onImportHistoryPicture(history: History) {
        historyForPhoto = history
        pickHistoryImageLauncher.launch("image/*")
    }

    private fun addChip(holder: BarrelViewHolder, text: String, icon: String) {
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
        holder.chipGroup?.addView(chip)
    }
}