package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonProperty


data class History(
    @field:JsonProperty("id")
    val id: Long = 0,
    @field:JsonProperty("barrelId")
    val barrelId: Long = 0, // clé étrangère
    @field:JsonProperty("name")
    val name: String = "",
    @field:JsonProperty("beginDate")
    val beginDate: Long = 0, // timestamp
    @field:JsonProperty("endDate")
    val endDate: Long? = null, // timestamp
    @field:JsonProperty("type")
    val type: String = "",
    @field:JsonProperty("description")
    val description: String? = null,
    @field:JsonProperty("angelsShare")
    val angelsShare: String? = null,
    @field:JsonProperty("alcoholicStrength")
    val alcoholicStrength: String? = null,
    @field:JsonProperty("imagePath")
    val imagePath: String? = null,
    @field:JsonProperty("alerts")
    val alerts: List<Alert> = emptyList()
)