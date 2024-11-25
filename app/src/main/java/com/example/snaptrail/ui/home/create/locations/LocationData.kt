package com.example.snaptrail.ui.home.create.locations

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationData(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
) : Parcelable