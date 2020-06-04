package com.example.whocooler.Common.Models

import com.google.gson.annotations.SerializedName

data class DebatesResponse(
    @SerializedName("categories") val categories: List<Category>,
    @SerializedName("debates") val debates: List<Debate>,
    @SerializedName("has_next_page") val hasNextPage: Boolean
)
