package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Alert(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("historyId")
    val historyId: Long, // clé étrangère
    @JsonProperty("type")
    var type: String,
    @JsonProperty("date")
    var date: Long
)