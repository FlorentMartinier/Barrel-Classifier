package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonProperty

data class History(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("barrelId")
    val barrelId: Long, // clé étrangère
    @JsonProperty("name")
    val name: String,
    @JsonProperty("beginDate")
    val beginDate: Long, // timestamp
    @JsonProperty("endDate")
    val endDate: Long? = null, // timestamp
    @JsonProperty("type")
    val type: String,
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("angelsShare")
    val angelsShare: String? = null,
    @JsonProperty("alcoholicStrength")
    val alcoholicStrength: String? = null,
    @JsonProperty("imagePath")
    val imagePath: String? = null,
    @JsonProperty("alerts")
    val alerts: List<Alert>
)