package com.ukp.axrcockpit.model

data class HwRxLensConfig(
    val vendorName: String,
    val version: Int,

    val refractiveIndexVisible: Float? = null,
    val refractiveIndexIr: Float? = null,
    val diameterMm: Float? = null,

    //Threshold for Sum of sph + cyl
    val lensThreshold: Float,

    val decimalPlacesSpherical: Int,
    val maxSpherical: Float,
    val minSpherical: Float,
    val stepSpherical: Float,

    val decimalPlacesCylindrical: Int,
    val maxCylindrical: Float,
    val minCylindrical: Float,
    val stepCylindrical: Float,

    val decimalPlacesAxis: Int,
    val maxAxis: Float,
    val minAxis: Float,
    val stepAxis: Float,
)