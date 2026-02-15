package com.fmartinier.barrelclassifier.service

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fmartinier.barrelclassifier.data.model.Alert
import java.util.concurrent.TimeUnit

class AlertService {

    fun schedule(context: Context, alert: Alert, barrelName: String, historyId: Long?) {
        var tenHoursToMillis = 36000000
        val delay = alert.date + tenHoursToMillis - System.currentTimeMillis()
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
            .addTag("history_$historyId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun cancelByHistoryId(context: Context, historyId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("history_$historyId")
    }

    fun cancelById(context: Context, alertId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("alert_$alertId")
    }
}