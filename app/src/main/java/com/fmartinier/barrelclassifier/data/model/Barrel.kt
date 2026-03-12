package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Barrel(
    @field:JsonProperty("id")
    val id: Long = 0,
    @field:JsonProperty("name")
    val name: String = "",
    @field:JsonProperty("volume")
    val volume: Int = 1, // en litres
    @field:JsonProperty("brand")
    val brand: String = "",
    @field:JsonProperty("woodType")
    val woodType: String = "",
    @field:JsonProperty("imagePath")
    val imagePath: String? = null, // chemin local de la photo
    @field:JsonProperty("heatType")
    val heatType: String? = null,
    @field:JsonProperty("storageHygrometer")
    val storageHygrometer: String? = null,
    @field:JsonProperty("storageTemperature")
    val storageTemperature: String? = null,
    @field:JsonProperty("description")
    val description: String? = null,
    @field:JsonProperty("histories")
    val histories: List<History> = emptyList()
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