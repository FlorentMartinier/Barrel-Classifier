package com.fmartinier.barrelclassifier.data.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Alert(
    @field:JsonProperty("id")
    val id: Long = 0,
    @field:JsonProperty("historyId")
    val historyId: Long, // clé étrangère
    @field:JsonProperty("type")
    var type: String,
    @field:JsonProperty("date")
    var date: Long
)