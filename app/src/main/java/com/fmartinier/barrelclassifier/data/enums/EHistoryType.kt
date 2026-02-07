package com.fmartinier.barrelclassifier.data.enums

import com.fmartinier.barrelclassifier.R

enum class EHistoryType(val historyTypeDescription: Int) {
    AGING(R.string.aging), // Vieillissement
    SEASONNING(R.string.seasonning), // Marquage
    MIX(R.string.mix), // Mixte
}