package com.example.floradata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.floradata.data.SensorRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Estado mutable local para simular la activación y desactivación de la bomba de agua
    var sensorData by remember { mutableStateOf(SensorRepository.mockSensorData) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FloraData - Monitoreo de Plantas") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Estado del Sistema de Riego y Estanque
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Estado del Sistema de Riego",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (sensorData.waterLevelOk) "Nivel de agua en estanque: Óptimo 💧" else "¡Alerta: Estanque sin agua! ⚠️",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (sensorData.pumpActive) "Bomba Activa (Regando)" else "Bomba Apagada",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = sensorData.pumpActive,
                            onCheckedChange = { active ->
                                sensorData = sensorData.copy(pumpActive = active)
                            }
                        )
                    }
                }
            }

            Text(
                text = "Lecturas Ambientales Actuales",
                style = MaterialTheme.typography.titleLarge
            )

            // Primera fila de sensores (Temperatura y Humedad de la Tierra)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SensorCard(
                    title = "Temperatura Aire",
                    value = "${sensorData.airTemperature} °C",
                    modifier = Modifier.weight(1f)
                )
                SensorCard(
                    title = "Humedad Tierra",
                    value = "${sensorData.soilHumidity}%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Segunda fila de sensores (Humedad del Aire y Luminosidad)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SensorCard(
                    title = "Humedad Aire",
                    value = "${sensorData.airHumidity}%",
                    modifier = Modifier.weight(1f)
                )
                SensorCard(
                    title = "Luminosidad",
                    value = "${sensorData.luminosity} Lux",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SensorCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}