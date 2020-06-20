package com.whocooler.app.Common.Models

import com.google.gson.annotations.SerializedName

data class DebatesResponse(
    @SerializedName("categories") val categories: ArrayList<Category>,
    @SerializedName("debates") var debates: ArrayList<Debate>,
    @SerializedName("has_next_page") var hasNextPage: Boolean
)
