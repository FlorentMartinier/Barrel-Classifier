package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
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
import androidx.fragment.app.DialogFragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.AlertDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.enums.EAlertType
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.service.AlertNotificationWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class AddHistoryDialog : DialogFragment() {

    private var beginDate: Long? = null
    private var endDate: Long? = null

    private lateinit var edtName: TextInputEditText
    private lateinit var edtBeginDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText
    private lateinit var alertsContainer: LinearLayout
    private lateinit var btnAddAlert: MaterialButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_history, null)

        edtName = view.findViewById(R.id.edtName)
        edtBeginDate = view.findViewById(R.id.edtBeginDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        beginDate = parseDate(edtBeginDate.text.toString())
        endDate = parseDate(edtEndDate.text.toString())
        alertsContainer = view.findViewById<LinearLayout>(R.id.alertsContainer)
        btnAddAlert = view.findViewById<MaterialButton>(R.id.btnAddAlert)

        edtBeginDate.setOnClickListener {
            openDatePicker {
                beginDate = it
                edtBeginDate.setText(formatDate(it))
            }
        }

        edtEndDate.setOnClickListener {
            openDatePicker {
                endDate = it
                edtEndDate.setText(formatDate(it))
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_history))
            .setView(view)
            .setPositiveButton(getString(R.string.add), null) // IMPORTANT
            .setNegativeButton(getString(R.string.cancel), null)
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

                alerts.any { it.date < System.currentTimeMillis() } -> {
                    showToast(getString(R.string.incoherent_date))
                }

                else -> {
                    val history = History(
                        barrelId = barrelId,
                        name = name,
                        beginDate = beginDate!!,
                        endDate = endDate
                    )

                    val historyId = HistoryDao(DatabaseHelper(requireContext()))
                        .insert(history)

                    AlertDao(DatabaseHelper(requireContext()))
                        .insert(alerts, historyId)
                        .forEach {
                            scheduleAlert(requireContext(), it, barrelName)
                        }

                    parentFragmentManager.setFragmentResult(
                        "add_barrel_result",
                        Bundle.EMPTY
                    )
                    dismiss() // 👈 fermeture contrôlée
                }
            }
        }
    }

    private fun addAlertRow() {
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
        val alertTypes = EAlertType.entries
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                alertTypes.map {
                    getString(it.alertDescription)
                }
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
            contentDescription = "Supprimer l’alerte"
        }

        // DatePicker
        edtDate.setOnClickListener {
            openDatePicker {
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

        alertsContainer.addView(row)
    }


    private fun openDatePicker(onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                cal.set(y, m, d, 0, 0)
                onSelected(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            .format(Date(timestamp))
    }

    private fun parseDate(dateString: String?): Long? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            sdf.isLenient = false
            sdf.parse(dateString ?: "")?.time
        } catch (e: Exception) {
            print(e)
            null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun scheduleAlert(context: Context, alert: Alert, barrelName: String) {
        //var tenHoursToMillis = 360000000
        //val delay = alert.date + tenHoursToMillis - System.currentTimeMillis()
        val delay = 6000L
        if (delay <= 0) return

        val data = workDataOf(
            "alertId" to alert.id,
            "type" to alert.type,
            "barrelName" to barrelName,
        )

        val workRequest = OneTimeWorkRequestBuilder<AlertNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("alert_${alert.id}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    companion object {
        const val TAG = "AddHistoryDialog"
        private const val ARG_BARREL_ID = "barrel_id"
        private const val ARG_BARREL_NAME = "barrel_name"

        fun newInstance(barrel: Barrel): AddHistoryDialog {
            return AddHistoryDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BARREL_ID, barrel.id)
                    putString(ARG_BARREL_NAME, barrel.name)
                }
            }
        }
    }
}

