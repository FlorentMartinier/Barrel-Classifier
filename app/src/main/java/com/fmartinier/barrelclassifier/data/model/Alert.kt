package com.fmartinier.barrelclassifier.data.model

data class Alert(
    val id: Long = 0,
    val historyId: Long, // clé étrangère
    var type: String,
    var date: Long
)