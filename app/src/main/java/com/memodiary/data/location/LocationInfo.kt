package com.memodiary.data.location

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null
)
