package com.example.floradata.data

object SensorRepository {
    // Datos falsos que simulan la lectura de los sensores de las plantas
    val mockSensorData = SensorData(
        airTemperature = 24.2,  // 24.2 °C
        soilHumidity = 58,      // 58% de humedad en la tierra
        airHumidity = 45,       // 45% de humedad en el aire
        luminosity = 820,       // 820 Lux
        waterLevelOk = true,    // Hay suficiente agua en el estanque
        pumpActive = false      // La bomba de agua está apagada por defecto
    )
}