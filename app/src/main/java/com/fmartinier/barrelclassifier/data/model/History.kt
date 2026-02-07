package com.fmartinier.barrelclassifier.data.model

data class History(
    val id: Long = 0,
    val barrelId: Long, // clé étrangère
    val name: String,
    val beginDate: Long, // timestamp
    val endDate: Long? = null, // timestamp
    val type: String,
    val description: String? = null,
    val angelsShare: String? = null,
    val alcoholicStrength: String? = null,
)