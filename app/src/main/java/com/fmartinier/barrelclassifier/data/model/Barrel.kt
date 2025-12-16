package com.fmartinier.barrelclassifier.data.model

data class Barrel(
    val id: Long = 0,
    val name: String,
    val volume: Int, // en litres
    val brand: String,
    val woodType: String,
    val imagePath: String?, // chemin local de la photo
    val histories: List<History>
)