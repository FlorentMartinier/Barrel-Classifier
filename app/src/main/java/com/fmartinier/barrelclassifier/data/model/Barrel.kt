package com.fmartinier.barrelclassifier.data.model

data class Barrel(
    val id: Long = 0,
    val name: String,
    val volume: Int, // en litres
    val brand: String,
    val woodType: String,
    val imagePath: String?, // chemin local de la photo
    val heatType: String?,
    val storageHygrometer: String?,
    val storageTemperature: String?,
    val histories: List<History>
)