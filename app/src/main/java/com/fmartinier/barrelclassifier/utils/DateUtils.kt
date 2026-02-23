package com.fmartinier.barrelclassifier.utils

import android.app.DatePickerDialog
import android.content.Context
import com.fmartinier.barrelclassifier.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

class DateUtils {
    companion object {
        fun formatDaysToDurationString(context: Context, totalDays: Int): String {
            if (totalDays <= 0) return "0"

            val startCal = Calendar.getInstance()
            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, totalDays)
            }

            // Calcul du nombre total de mois
            var totalMonths = (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                    (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))

            val dayOfMonthStart = startCal.get(Calendar.DAY_OF_MONTH)
            val dayOfMonthEnd = endCal.get(Calendar.DAY_OF_MONTH)

            var daysRemainingInMonth = dayOfMonthEnd - dayOfMonthStart

            // Ajustement si le jour de fin est inférieur au jour de début
            if (daysRemainingInMonth < 0) {
                totalMonths -= 1
                val lastMonthMaxDays = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                daysRemainingInMonth += lastMonthMaxDays
            }

            // Calcul des années et mois restants
            val years = totalMonths / 12
            val remainingMonths = totalMonths % 12

            return when {
                // Règle 1 : Si > 13 mois (ou exactement 13 mois) => "x ans et x mois"
                totalMonths >= 13 -> {
                    if (remainingMonths > 0) {
                        val formattedYears = context.resources.getQuantityString(R.plurals.year, years, years)
                        val formattedMonths = context.resources.getQuantityString(R.plurals.month, remainingMonths, remainingMonths)
                        context.resources.getString(R.string.element_and_element, formattedYears, formattedMonths)
                    } else {
                        context.resources.getQuantityString(R.plurals.year, years, years)
                    }
                }

                // Règle 2 : Si entre 12 et 13 mois (exclu) => "x ans"
                totalMonths == 12 -> {
                    context.resources.getQuantityString(R.plurals.year, years, years)
                }

                // Règle 3 : Si < 12 mois => Ancienne logique (Mois / Semaines / Jours)
                else -> {
                    var weeks = daysRemainingInMonth / 7
                    var finalMonths = totalMonths

                    if (weeks >= 4) {
                        finalMonths += 1
                        weeks = 0
                    }

                    when {
                        finalMonths > 0 && weeks > 0 -> {
                            val formattedMonths = context.resources.getQuantityString(R.plurals.month, finalMonths, finalMonths)
                            val formattedWeeks = context.resources.getQuantityString(R.plurals.week, weeks, weeks)
                            context.resources.getString(R.string.element_and_element, formattedMonths, formattedWeeks)
                        }
                        finalMonths > 0 -> context.resources.getQuantityString(R.plurals.month, finalMonths, finalMonths)
                        weeks > 0 -> context.resources.getQuantityString(R.plurals.week, weeks, weeks)
                        else -> context.resources.getQuantityString(R.plurals.day, totalDays, totalDays)
                    }
                }
            }
        }

        fun calculateNbDaysBetweenDates(dateDebut: Long, dateFin: Long?): Int {
            val end = dateFin ?: getCurrentDate()
            val millisDifference = end - dateDebut
            return if (millisDifference <= 0) {
                0
            } else {
                (millisDifference / (1000 * 60 * 60 * 24)).toInt()
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

        fun calculate228lEquivalentAge(days: Int, barrelVolume: Double): Int {
            if (barrelVolume <= 0) return days
            val accelerationRatio = getEquivalenceRatio(barrelVolume)
            return (days * accelerationRatio).roundToInt()
        }

        fun getEquivalenceRatio(barrelVolume: Double): Double {
            val referenceVolume = 228.0
            return (referenceVolume / barrelVolume).pow(0.327)
        }
    }
}