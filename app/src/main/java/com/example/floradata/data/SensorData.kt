package com.example.floradata.data

data class SensorData(
    val airTemperature: Double,   // Temperatura del aire (ej. 24.5 °C)
    val soilHumidity: Int,        // Humedad de la tierra en porcentaje (ej. 60%)
    val airHumidity: Int,         // Humedad del aire en porcentaje (ej. 45%)
    val luminosity: Int,          // Luminosidad en Lux (ej. 800 lux)
    val waterLevelOk: Boolean,    // Estado del sensor de agua (true si hay agua suficiente)
    val pumpActive: Boolean       // Estado de la bomba de agua (true si está regando)
)