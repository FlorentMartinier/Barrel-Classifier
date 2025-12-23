package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
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


class AddHistoryDialog : DialogFragment() {

    private var beginDate: Long? = null
    private var endDate: Long? = null

    private lateinit var edtName: TextInputEditText
    private lateinit var edtBeginDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_history, null)

        edtName = view.findViewById(R.id.edtName)
        edtBeginDate = view.findViewById(R.id.edtBeginDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        beginDate = parseDate(edtBeginDate.text.toString())
        endDate = parseDate(edtEndDate.text.toString())

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

        positiveButton.setOnClickListener {

            val name = edtName.text?.toString()?.trim().orEmpty()

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

                else -> {
                    val history = History(
                        barrelId = requireArguments().getLong(ARG_BARREL_ID),
                        name = name,
                        beginDate = beginDate!!,
                        endDate = endDate
                    )

                    HistoryDao(DatabaseHelper(requireContext()))
                        .insert(history)

                    parentFragmentManager.setFragmentResult(
                        "add_barrel_result",
                        Bundle.EMPTY
                    )
                    dismiss() // 👈 fermeture contrôlée
                }
            }
        }
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

    companion object {
        const val TAG = "AddHistoryDialog"
        private const val ARG_BARREL_ID = "barrel_id"

        fun newInstance(barrelId: Long): AddHistoryDialog {
            return AddHistoryDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BARREL_ID, barrelId)
                }
            }
        }
    }
}

