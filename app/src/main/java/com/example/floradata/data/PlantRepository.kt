package com.example.floradata.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Plant(
    val id: Int,
    val name: String,
    val section: String,
    val airTemperature: Double,
    val soilHumidity: Int,
    val airHumidity: Int,
    val luminosity: Int,
    val waterLevelOk: Boolean,
    val pumpActive: Boolean,
    val imageUri: String? = null,
    val isOnline: Boolean = true // Estado de conexión IoT del sensor
)

data class PlantAlert(
    val id: Int,
    val plantId: Int,
    val plantName: String,
    val message: String,
    val time: String
)

object PlantRepository {
    private const val PREF_NAME = "floradata_prefs"
    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        prefs = appContext!!.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadData()
        isInitialized = true
    }

    private fun persistImageIfNeeded(uriString: String?): String? {
        if (uriString.isNullOrBlank() || uriString == "null") return null
        if (uriString.startsWith("/")) return uriString

        val ctx = appContext ?: return uriString
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = ctx.contentResolver.openInputStream(uri) ?: return uriString
            val file = File(ctx.filesDir, "plant_${System.currentTimeMillis()}.jpg")
            val outputStream = file.outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
        }
    }

    private fun saveData() {
        if (!isInitialized) return
        val editor = prefs.edit()

        val sectionsArray = JSONArray(sections)
        editor.putString("sections", sectionsArray.toString())

        val plantsArray = JSONArray()
        for (p in plants) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("section", p.section)
                put("airTemperature", p.airTemperature)
                put("soilHumidity", p.soilHumidity)
                put("airHumidity", p.airHumidity)
                put("luminosity", p.luminosity)
                put("waterLevelOk", p.waterLevelOk)
                put("pumpActive", p.pumpActive)
                put("imageUri", p.imageUri ?: "")
                put("isOnline", p.isOnline)
            }
            plantsArray.put(obj)
        }
        editor.putString("plants", plantsArray.toString())

        val alertsArray = JSONArray()
        for (a in alerts) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("plantId", a.plantId)
                put("plantName", a.plantName)
                put("message", a.message)
                put("time", a.time)
            }
            alertsArray.put(obj)
        }
        editor.putString("alerts", alertsArray.toString())

        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.putString("userName", userName)
        editor.putString("userEmail", userEmail)
        editor.putInt("tankWaterLevel", tankWaterLevel)
        editor.putBoolean("autoWateringEnabled", autoWateringEnabled)
        editor.putBoolean("pushNotificationsEnabled", pushNotificationsEnabled)
        editor.putBoolean("iotPowerSaving", iotPowerSaving)
        editor.putBoolean("soundAlertsEnabled", soundAlertsEnabled)
        editor.putBoolean("isOnboardingCompleted", isOnboardingCompleted)

        editor.apply()
    }

    private fun loadData() {
        val sectionsStr = prefs.getString("sections", null)
        if (sectionsStr != null) {
            try {
                val arr = JSONArray(sectionsStr)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                if (list.isNotEmpty()) sections = list
            } catch (e: Exception) { e.printStackTrace() }
        }

        val plantsStr = prefs.getString("plants", null)
        if (plantsStr != null) {
            try {
                val arr = JSONArray(plantsStr)
                val list = mutableListOf<Plant>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val img = obj.getString("imageUri")
                    val plantId = obj.getInt("id")

                    // FORZAR LA MONSTERA (ID 2) OFFLINE PARA PRUEBAS, el resto lee su estado normal
                    val online = if (plantId == 2) false else try { obj.getBoolean("isOnline") } catch (e: Exception) { true }

                    list.add(
                        Plant(
                            id = plantId,
                            name = obj.getString("name"),
                            section = obj.getString("section"),
                            airTemperature = obj.getDouble("airTemperature"),
                            soilHumidity = obj.getInt("soilHumidity"),
                            airHumidity = obj.getInt("airHumidity"),
                            luminosity = obj.getInt("luminosity"),
                            waterLevelOk = obj.getBoolean("waterLevelOk"),
                            pumpActive = obj.getBoolean("pumpActive"),
                            imageUri = if (img.isEmpty()) null else img,
                            isOnline = online
                        )
                    )
                }
                if (list.isNotEmpty()) plants = list
            } catch (e: Exception) { e.printStackTrace() }
        }

        val alertsStr = prefs.getString("alerts", null)
        if (alertsStr != null) {
            try {
                val arr = JSONArray(alertsStr)
                val list = mutableListOf<PlantAlert>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        PlantAlert(
                            id = obj.getInt("id"),
                            plantId = obj.getInt("plantId"),
                            plantName = obj.getString("plantName"),
                            message = obj.getString("message"),
                            time = obj.getString("time")
                        )
                    )
                }
                if (list.isNotEmpty()) alerts = list
            } catch (e: Exception) { e.printStackTrace() }
        }

        isLoggedIn = prefs.getBoolean("isLoggedIn", isLoggedIn)
        userName = prefs.getString("userName", userName) ?: userName
        userEmail = prefs.getString("userEmail", userEmail) ?: userEmail
        tankWaterLevel = prefs.getInt("tankWaterLevel", tankWaterLevel)
        autoWateringEnabled = prefs.getBoolean("autoWateringEnabled", autoWateringEnabled)
        pushNotificationsEnabled = prefs.getBoolean("pushNotificationsEnabled", pushNotificationsEnabled)
        iotPowerSaving = prefs.getBoolean("iotPowerSaving", iotPowerSaving)
        soundAlertsEnabled = prefs.getBoolean("soundAlertsEnabled", soundAlertsEnabled)
        isOnboardingCompleted = prefs.getBoolean("isOnboardingCompleted", isOnboardingCompleted)
    }

    var sections by mutableStateOf(listOf("Sala de estar", "Jardín", "Balcón"))

    var plants by mutableStateOf(
        listOf(
            Plant(1, "Ficus Benjamina", "Sala de estar", 24.2, 35, 45, 820, true, false, null, true),
            Plant(2, "Monstera Deliciosa", "Jardín", 22.8, 60, 50, 1100, true, true, null, false), // <--- CONFIGURADA OFFLINE PARA PRUEBAS
            Plant(3, "Cactus San Pedro", "Balcón", 28.5, 15, 30, 1500, false, false, null, true)
        )
    )

    var alerts by mutableStateOf(
        listOf(
            PlantAlert(1, 3, "Cactus San Pedro", "¡Alerta: Estanque sin agua en el sector Balcón!", "14:20 hrs"),
            PlantAlert(2, 1, "Ficus Benjamina", "Humedad de tierra baja (35%)", "12:05 hrs")
        )
    )

    var isLoggedIn by mutableStateOf(false)
    var userName by mutableStateOf("Estudiante INACAP")
    var userEmail by mutableStateOf("floradata@inacap.cl")
    var tankWaterLevel by mutableStateOf(80)

    var snackbarMessage by mutableStateOf<String?>(null)

    fun showMessage(msg: String) {
        snackbarMessage = msg
    }

    var autoWateringEnabled by mutableStateOf(true)
    var pushNotificationsEnabled by mutableStateOf(true)
    var iotPowerSaving by mutableStateOf(false)
    var soundAlertsEnabled by mutableStateOf(true)
    var isOnboardingCompleted by mutableStateOf(false)

    fun toggleAutoWatering(enabled: Boolean) { autoWateringEnabled = enabled; saveData() }
    fun togglePushNotifications(enabled: Boolean) { pushNotificationsEnabled = enabled; saveData() }
    fun toggleIotPowerSaving(enabled: Boolean) { iotPowerSaving = enabled; saveData() }
    fun toggleSoundAlerts(enabled: Boolean) { soundAlertsEnabled = enabled; saveData() }

    fun completeOnboarding() {
        isOnboardingCompleted = true
        saveData()
    }

    fun login(email: String, name: String?) {
        userEmail = email
        if (!name.isNullOrBlank()) {
            userName = name
        }
        isLoggedIn = true
        saveData()
        showMessage("¡Sesión iniciada con éxito!")
    }

    fun logout() {
        isLoggedIn = false
        saveData()
        showMessage("Sesión cerrada")
    }

    fun togglePump(plantId: Int) {
        plants = plants.map { plant ->
            if (plant.id == plantId) {
                val newStatus = !plant.pumpActive
                showMessage(if (newStatus) "Bomba activada para ${plant.name}" else "Bomba desactivada")
                plant.copy(pumpActive = newStatus)
            } else plant
        }
        saveData()
    }

    fun updatePlantInfo(plantId: Int, newName: String, newSection: String, newImageUri: String?) {
        val persistentUri = persistImageIfNeeded(newImageUri)
        plants = plants.map { plant ->
            if (plant.id == plantId) {
                showMessage("Planta actualizada correctamente")
                plant.copy(
                    name = newName,
                    section = newSection,
                    imageUri = persistentUri ?: plant.imageUri
                )
            } else plant
        }
        saveData()
    }

    fun addPlant(name: String, section: String, imageUri: String?) {
        val persistentUri = persistImageIfNeeded(imageUri)
        val newId = if (plants.isEmpty()) 1 else plants.maxOf { it.id } + 1
        val newPlant = Plant(
            id = newId,
            name = name,
            section = section,
            airTemperature = 23.5,
            soilHumidity = 50,
            airHumidity = 45,
            luminosity = 900,
            waterLevelOk = true,
            pumpActive = false,
            imageUri = persistentUri,
            isOnline = true
        )
        plants = plants + newPlant
        saveData()
        showMessage("¡Planta '$name' agregada con éxito!")
    }

    fun deletePlant(plantId: Int) {
        plants = plants.filter { it.id != plantId }
        saveData()
        showMessage("Planta eliminada")
    }

    fun addSection(sectionName: String) {
        if (sectionName.isNotBlank() && !sections.contains(sectionName)) {
            sections = sections + sectionName
            saveData()
            showMessage("Conjunto '$sectionName' creado")
        }
    }

    fun deleteSection(sectionName: String) {
        sections = sections.filter { it != sectionName }
        if (!sections.contains("General")) {
            sections = sections + "General"
        }
        plants = plants.map { plant ->
            if (plant.section == sectionName) plant.copy(section = "General") else plant
        }
        saveData()
        showMessage("Conjunto '$sectionName' eliminado")
    }
}