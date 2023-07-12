package com.hfad.kundalikob_havo.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Rain(
    @SerializedName("1h")
    val oneHour :Double,
): Serializable
