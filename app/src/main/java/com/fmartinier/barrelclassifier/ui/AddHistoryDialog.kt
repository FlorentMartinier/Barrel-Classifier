package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.History
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddHistoryDialog(
    private val barrelId: Long,
    private val onHistoryAdded: () -> Unit
) : DialogFragment() {

    private var dateDebut: Long? = null
    private var dateFin: Long? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_history, null)

        val edtNom = view.findViewById<TextInputEditText>(R.id.edtNom)
        val edtDateDebut = view.findViewById<TextInputEditText>(R.id.edtDateDebut)
        val edtDateFin = view.findViewById<TextInputEditText>(R.id.edtDateFin)

        edtDateDebut.setOnClickListener {
            openDatePicker {
                dateDebut = it
                edtDateDebut.setText(formatDate(it))
            }
        }

        edtDateFin.setOnClickListener {
            openDatePicker {
                dateFin = it
                edtDateFin.setText(formatDate(it))
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(requireContext().resources.getString(R.string.add_history))
            .setView(view)
            .setPositiveButton(requireContext().resources.getString(R.string.add)) { _, _ ->
                if (dateDebut == null) return@setPositiveButton

                val historique = History(
                    barrelId = barrelId,
                    name = edtNom.text.toString(),
                    beginDate = dateDebut!!,
                    endDate = dateFin // peut être NULL
                )

                HistoryDao(DatabaseHelper(requireContext()))
                    .insert(historique)

                onHistoryAdded()
            }
            .setNegativeButton(requireContext().resources.getString(R.string.cancel), null)
            .create()
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
}


