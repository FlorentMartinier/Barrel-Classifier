package com.fmartinier.barrelclassifier.utils

import android.app.DatePickerDialog
import android.content.Context
import com.fmartinier.barrelclassifier.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateUtils {
    companion object {
        fun calculateDuration(context: Context, dateDebut: Long, dateFin: Long?): String {
            val end = dateFin ?: System.currentTimeMillis()

            val days = (end - dateDebut) / (1000 * 60 * 60 * 24)
            val months = days / 30
            val weeks = (days % 30) / 7

            return context.resources.getString(
                R.string.month_and_week,
                months.toString(),
                weeks.toString()
            )
        }


        fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                .format(Date(timestamp))
        }

        fun parseDate(dateString: String?): Long? {
            return try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                sdf.isLenient = false
                sdf.parse(dateString ?: "")?.time
            } catch (e: Exception) {
                print(e)
                null
            }
        }

        fun openDatePicker(context: Context, onSelected: (Long) -> Unit) {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0)
                    onSelected(cal.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}