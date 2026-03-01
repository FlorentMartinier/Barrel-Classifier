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
    val description: String?,
    val histories: List<History>
) {

    fun getCurrentHistory(): History? {
        val notEndedHistory = histories.filter { it.endDate == null }
        return if (notEndedHistory.isNotEmpty()) {
            notEndedHistory.first()
        } else {
            null
        }
    }
}