package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Barrel(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("volume")
    val volume: Int, // en litres
    @JsonProperty("brand")
    val brand: String,
    @JsonProperty("woodType")
    val woodType: String,
    @JsonProperty("imagePath")
    val imagePath: String?, // chemin local de la photo
    @JsonProperty("heatType")
    val heatType: String?,
    @JsonProperty("storageHygrometer")
    val storageHygrometer: String?,
    @JsonProperty("storageTemperature")
    val storageTemperature: String?,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("histories")
    val histories: List<History>
) {

    @JsonIgnore
    fun getCurrentHistory(): History? {
        val notEndedHistory = histories.filter { it.endDate == null }
        return if (notEndedHistory.isNotEmpty()) {
            notEndedHistory.first()
        } else {
            null
        }
    }
}