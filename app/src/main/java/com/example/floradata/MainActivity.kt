package com.example.floradata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.floradata.data.PlantRepository
import com.example.floradata.navigation.MainAppScreen
import com.example.floradata.ui.theme.FloraDataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar persistencia local
        PlantRepository.init(applicationContext)

        setContent {
            FloraDataTheme {
                MainAppScreen()
            }
        }
    }
}