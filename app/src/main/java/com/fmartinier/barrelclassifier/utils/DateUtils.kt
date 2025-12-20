package com.fmartinier.barrelclassifier.utils

import android.content.Context
import com.fmartinier.barrelclassifier.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateUtils {
    companion object {
        fun calculateDuration(context : Context, dateDebut: Long, dateFin: Long?): String {
            val end = dateFin ?: System.currentTimeMillis()

            val days = (end - dateDebut) / (1000 * 60 * 60 * 24)
            val months = days / 30
            val weeks = (days % 30) / 7

            return context.resources.getString(R.string.month_and_week, months.toString(), weeks.toString())
        }


        fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            return sdf.format(Date(timestamp))
        }
    }
}