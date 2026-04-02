package com.memodiary.data.location

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,   // 区/县 level
    val street: String? = null,     // 街道/道路
    val address: String? = null     // full address line
) {
    /** Short display label: prefer address > district > city coordinate fallback. */
    fun displayLabel(): String = address
        ?: listOfNotNull(district, city, province).joinToString("").ifBlank {
            "%.5f, %.5f".format(latitude, longitude)
        }
}
