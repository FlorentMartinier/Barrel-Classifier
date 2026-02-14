package com.fmartinier.barrelclassifier.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class NotificationService {

    fun createNotificationChannel(activity: Activity) {
        // Gérer la permission de lancer des notifications
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                activity,
                POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(POST_NOTIFICATIONS),
                1001
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alerts_channel",
                "Alertes de vieillissement",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes liées aux historiques en fût"
            }

            val manager = activity.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}