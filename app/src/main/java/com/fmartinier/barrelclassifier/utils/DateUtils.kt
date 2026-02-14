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
            val endMillis = dateFin ?: System.currentTimeMillis()

            val startCal = Calendar.getInstance().apply { timeInMillis = dateDebut }
            val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

            var months = (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                    (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))

            // Calcul du reliquat de jours pour trouver les semaines
            val dayOfMonthStart = startCal.get(Calendar.DAY_OF_MONTH)
            val dayOfMonthEnd = endCal.get(Calendar.DAY_OF_MONTH)

            var daysRemaining = dayOfMonthEnd - dayOfMonthStart

            // Si le jour de fin est inférieur au jour de début, on retire un mois
            // et on calcule les jours sur le mois précédent
            if (daysRemaining < 0) {
                months -= 1
                val lastMonthMaxDays = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                daysRemaining += lastMonthMaxDays
            }

            var weeks = daysRemaining / 7

            // Logique d'arrondi : 4 semaines = 1 mois de plus
            if (weeks >= 4) {
                months += 1
                weeks = 0
            }

            return if (weeks == 0) {
                context.resources.getString(
                    R.string.month,
                    months.toString(),
                )
            } else if (months == 0) {
                context.resources.getString(
                    R.string.month_and_week,
                    months.toString(),
                    weeks.toString()
                )
            } else {
                context.resources.getString(
                    R.string.month_and_week,
                    months.toString(),
                    weeks.toString()
                )
            }
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

        fun getCurrentDate(): Long {
            return System.currentTimeMillis()
        }
    }
}