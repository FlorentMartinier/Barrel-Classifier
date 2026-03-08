package com.fmartinier.barrelclassifier.service

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class AnalyticsService {

    companion object {
        fun logBarrelAdded(woodType: String, volume: String) {
            val parameters = Bundle().apply {
                this.putString("wood_type", woodType)
                this.putString("volume", volume)
            }
            Firebase.analytics.logEvent("barrel_add_success",parameters)
        }

        fun logHistoryAdded() {
            Firebase.analytics.logEvent("history_add_success", Bundle())
        }

        fun logQrShared() {
            Firebase.analytics.logEvent("qr_cloud_shared", Bundle())
        }

        fun logPdfExport() {
            Firebase.analytics.logEvent("pdf_export", Bundle())
        }
    }
}