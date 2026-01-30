package com.fmartinier.barrelclassifier.service

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fmartinier.barrelclassifier.R

class AlertNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {

        val alertType = inputData.getString("type") ?: return Result.failure()
        val alertId = inputData.getLong("alertId", 0L)
        val barrelName = inputData.getString("barrelName")

        val notification = NotificationCompat.Builder(
            applicationContext,
            "alerts_channel"
        )
            .setContentTitle(
                applicationContext.resources.getString(
                    R.string.alertTitle,
                    barrelName
                )
            )
            .setContentText(applicationContext.resources.getString(
                R.string.alertContent,
                alertType
            ))
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_calendar)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(alertId.toInt(), notification)

        return Result.success()
    }
}
