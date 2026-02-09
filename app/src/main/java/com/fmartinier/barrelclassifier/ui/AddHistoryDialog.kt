package com.fmartinier.barrelclassifier.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.AlertDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.enums.EAlertType
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.AlertService
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.formatDate
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.openDatePicker
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.parseDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class AddHistoryDialog : DialogFragment() {

    private var beginDate: Long? = null
    private var endDate: Long? = null
    private var historyId: Long? = null

    private lateinit var edtName: TextInputEditText
    private lateinit var edtBeginDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText
    private lateinit var alertsContainer: LinearLayout
    private lateinit var btnAddAlert: MaterialButton
    private lateinit var advancedButton: MaterialButton
    private lateinit var advancedLayout: LinearLayout
    private lateinit var edtDescription: TextInputEditText
    private lateinit var edtAngelsShare: TextInputEditText
    private lateinit var edtAlcohol: TextInputEditText
    private lateinit var edtHistoryType: MaterialAutoCompleteTextView

    private val alertService = AlertService()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_history, null)
        historyId = arguments
            ?.takeIf { it.containsKey(ARG_HISTORY_ID) }
            ?.getLong(ARG_HISTORY_ID)
        val alertDao = AlertDao(DatabaseHelper(requireContext()))
        val historyDao = HistoryDao(DatabaseHelper(requireContext()))

        edtName = view.findViewById(R.id.edtName)
        edtBeginDate = view.findViewById(R.id.edtBeginDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        beginDate = parseDate(edtBeginDate.text.toString())
        endDate = parseDate(edtEndDate.text.toString())
        alertsContainer = view.findViewById<LinearLayout>(R.id.alertsContainer)
        btnAddAlert = view.findViewById<MaterialButton>(R.id.btnAddAlert)
        advancedButton = view.findViewById<MaterialButton>(R.id.txtToggleAdvanced)
        advancedLayout = view.findViewById<LinearLayout>(R.id.layoutAdvanced)
        edtDescription = view.findViewById(R.id.edtDescription)
        edtAngelsShare = view.findViewById(R.id.edtAngelsShare)
        edtAlcohol = view.findViewById(R.id.edtAlcohol)
        edtHistoryType = view.findViewById(R.id.autoCompleteHistoryType)

        val items = resources.getStringArray(R.array.type_fut_array)
        val historyTypeAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        edtHistoryType.setAdapter(historyTypeAdapter)

        advancedButton.setOnClickListener {
            if (advancedLayout.isGone) {
                advancedLayout.visibility = View.VISIBLE
                advancedButton.text = getString(R.string.advanced_option_up)
            } else {
                advancedLayout.visibility = View.GONE
                advancedButton.text = getString(R.string.advanced_option_down)
            }
        }

        edtBeginDate.setOnClickListener {
            openDatePicker(requireContext()) {
                beginDate = it
                edtBeginDate.setText(formatDate(it))
            }
        }

        edtEndDate.setOnClickListener {
            openDatePicker(requireContext()) {
                endDate = it
                edtEndDate.setText(formatDate(it))
            }
        }

        // Cas d'un update, pré remplissage des champs
        historyId?.let {
            val alerts = alertDao.getByHistoryId(historyId!!)
            val history = historyDao.getById(historyId!!)
            edtName.setText(history.name)
            beginDate = history.beginDate
            endDate = history.endDate
            edtBeginDate.setText(formatDate(history.beginDate))
            history.endDate?.let {
                edtEndDate.setText(formatDate(history.endDate))
            }
            alerts.forEach { alert ->
                addAlertRow(alert)
            }
            edtHistoryType.setText(history.type, false)
            history.description?.let { edtDescription.setText(it) }
            history.angelsShare?.let { edtAngelsShare.setText(it) }
            history.alcoholicStrength?.let { edtAlcohol.setText(it) }
        }

        val positiveButtonText = if (historyId == null) R.string.add else R.string.modify

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_history))
            .setView(view)
            .setPositiveButton(getString(positiveButtonText), null) // IMPORTANT
            .setNegativeButton(getString(R.string.cancel), null)
            .setBackground(requireContext(), R.color.dialog_bg)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        btnAddAlert.setOnClickListener {
            addAlertRow()
        }

        positiveButton.setOnClickListener {

            val name = edtName.text?.toString()?.trim().orEmpty()
            val alerts = mutableListOf<Alert>()
            val barrelId = requireArguments().getLong(ARG_BARREL_ID)
            val barrelName = requireArguments().getString(ARG_BARREL_NAME) ?: ""
            val description = edtDescription.text?.toString()?.trim().orEmpty()
            val angelsShare = edtAngelsShare.text?.toString()?.trim().orEmpty()
            val alcohol = edtAlcohol.text?.toString()?.trim().orEmpty()
            val historyType = edtHistoryType.text?.toString()?.trim().orEmpty()


            for (i in 0 until alertsContainer.childCount) {
                val row = alertsContainer.getChildAt(i) as? LinearLayout ?: continue

                val spinner = row.findViewById<Spinner>(row.getChildAt(0).id)
                val dateLayout = row.getChildAt(1) as TextInputLayout
                val edtDate = dateLayout.editText

                val type = spinner.selectedItem?.toString() ?: continue
                val dateText = edtDate?.text?.toString().orEmpty()

                if (dateText.isBlank()) continue

                val date = parseDate(dateText) ?: continue

                alerts.add(
                    Alert(
                        historyId = 0,
                        type = type,
                        date = date
                    )
                )
            }

            when {
                name.isEmpty() -> {
                    showToast(getString(R.string.history_name) + " " + getString(R.string.required))
                }

                beginDate == null -> {
                    showToast(getString(R.string.begin_date) + " " + getString(R.string.required))
                }

                endDate != null && endDate!! < beginDate!! -> {
                    showToast(getString(R.string.incoherent_date))
                }

                (beginDate ?: 0) > System.currentTimeMillis() -> {
                    showToast(getString(R.string.date_future))
                }

                else -> {
                    val history = History(
                        barrelId = barrelId,
                        name = name,
                        beginDate = beginDate!!,
                        endDate = endDate,
                        description = description,
                        angelsShare = angelsShare,
                        alcoholicStrength = alcohol,
                        type = historyType,
                    )

                    val historyDao = HistoryDao(DatabaseHelper(requireContext()))
                    val alertDao = AlertDao(DatabaseHelper(requireContext()))
                    if (this.historyId != null) {
                        historyDao.update(historyId!!, history)
                        alertService.cancelByHistoryId(requireContext(), historyId!!)
                        alertDao.deleteByHistoryId(historyId!!)
                    } else {
                        historyId = historyDao
                            .insert(history)
                    }

                    alertDao
                        .insert(alerts, historyId!!)
                        .forEach {
                            alertService.schedule(requireContext(), it, barrelName, historyId)
                        }

                    parentFragmentManager.setFragmentResult(
                        "add_barrel_result",
                        Bundle.EMPTY
                    )
                    dismiss()
                }
            }
        }
    }

    private fun addAlertRow(alert: Alert? = null) {
        val context = requireContext()

        // Ligne horizontale
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply { topMargin = 12 }
        }

        // Spinner type d’alerte
        val alertTypes = EAlertType.entries.map {
            getString(it.alertDescription)
        }
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                alertTypes
            )
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            id = View.generateViewId()
        }

        // Date avec icône calendrier
        val dateLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.5f)
            hint = "Date"
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_calendar)
            id = View.generateViewId()
        }

        val edtDate = TextInputEditText(context).apply {
            isFocusable = false
            isClickable = true
            id = View.generateViewId()
        }
        dateLayout.addView(edtDate)

        // Bouton supprimer
        val btnDelete = ImageButton(context).apply {
            setImageResource(R.drawable.ic_delete)
            setBackgroundResource(android.R.color.transparent)
        }

        // DatePicker
        edtDate.setOnClickListener {
            openDatePicker(requireContext()) {
                edtDate.setText(formatDate(it))
            }
        }

        // Suppression de l’alerte
        btnDelete.setOnClickListener {
            alertsContainer.removeView(row)
        }

        // Ajout des vues
        row.addView(spinner)
        row.addView(dateLayout)
        row.addView(btnDelete)

        alert?.let {
            val indexOfSelectedAlert = alertTypes.indexOf(it.type)
            if (indexOfSelectedAlert >= 0) {
                spinner.setSelection(indexOfSelectedAlert)
            }
            edtDate.setText(formatDate(it.date))
        }

        alertsContainer.addView(row)
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "AddHistoryDialog"
        private const val ARG_HISTORY_ID = "history_id"
        private const val ARG_BARREL_ID = "barrel_id"
        private const val ARG_BARREL_NAME = "barrel_name"

        fun newInstance(barrel: Barrel, historyId: Long? = null): AddHistoryDialog {
            return AddHistoryDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BARREL_ID, barrel.id)
                    putString(ARG_BARREL_NAME, barrel.name)
                    historyId?.let {
                        putLong(ARG_HISTORY_ID, historyId)
                    }
                }
            }
        }
    }
}

