package com.example.floradata.navigation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.floradata.data.PlantRepository
import java.io.File

val AppBgColor = Color(0xFFF4F6F5)
val CardBgColor = Color(0xFFFFFFFF)
val PrimaryGreen = Color(0xFF0F4C3A)
val TextDark = Color(0xFF2C3E50)

fun createImageUri(context: Context): Uri {
    val directory = File(context.getExternalFilesDir(null), "Pictures")
    if (!directory.exists()) directory.mkdirs()
    val file = File(directory, "plant_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

sealed class BottomScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomScreen("home", "Inicio", Icons.Default.Home)
    object Plants : BottomScreen("plants", "Mis Plantas", Icons.Default.LocalFlorist)
    object Alerts : BottomScreen("alerts", "Avisos", Icons.Default.Notifications)
    object Profile : BottomScreen("profile", "Perfil", Icons.Default.Person)
}

@Composable
fun MainAppScreen() {
    // Si no ha completado el onboarding, mostramos la pantalla de bienvenida
    if (!PlantRepository.isOnboardingCompleted) {
        OnboardingScreen(onFinish = { PlantRepository.completeOnboarding() })
        return
    }

    val navController = rememberNavController()
    val items = listOf(BottomScreen.Home, BottomScreen.Plants, BottomScreen.Alerts, BottomScreen.Profile)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(PlantRepository.snackbarMessage) {
        PlantRepository.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            PlantRepository.snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,       // Icono en blanco al seleccionarlo
                            unselectedIconColor = Color.Gray,      // Icono en gris cuando no está activo
                            selectedTextColor = PrimaryGreen,      // Texto de la sección activa
                            unselectedTextColor = Color.Gray,      // Texto de las secciones inactivas
                            indicatorColor = PrimaryGreen          // Fondo en pastilla verde detrás del icono seleccionado
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomScreen.Home.route) { HomeScreen(navController) }
            composable(BottomScreen.Plants.route) { PlantsScreen(navController) }
            composable(BottomScreen.Alerts.route) { AlertsScreen(navController) }
            composable(BottomScreen.Profile.route) { ProfileScreen(navController) }

            composable("login_screen") { LoginScreen(navController) }

            composable(
                route = "section_detail/{sectionName}",
                arguments = listOf(navArgument("sectionName") { type = NavType.StringType })
            ) { backStackEntry ->
                val sectionName = backStackEntry.arguments?.getString("sectionName") ?: "General"
                SectionDetailScreen(sectionName = sectionName, navController = navController)
            }

            composable("add_plant_step1") { AddPlantStep1Screen(navController) }

            composable(
                route = "add_plant_step2/{plantName}/{imageUri}",
                arguments = listOf(
                    navArgument("plantName") { type = NavType.StringType },
                    navArgument("imageUri") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val plantName = backStackEntry.arguments?.getString("plantName") ?: ""
                val imageUri = backStackEntry.arguments?.getString("imageUri")
                val decodedUri = if (imageUri == "null") null else imageUri
                AddPlantStep2Screen(plantName = plantName, imageUri = decodedUri, navController = navController)
            }

            composable(
                route = "plant_detail/{plantId}",
                arguments = listOf(navArgument("plantId") { type = NavType.IntType })
            ) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getInt("plantId") ?: 1
                PlantDetailScreen(plantId = plantId, navController = navController)
            }
        }
    }
}

// ----------------------------------------------------
// PANTALLA DE ONBOARDING
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    val titles = listOf(
        "Bienvenido a FloraData",
        "Monitoreo Ambiental IoT",
        "Control y Riego Inteligente"
    )

    val descriptions = listOf(
        "La solución integral para la gestión de tus cultivos, plantas y espacios verdes de forma profesional.",
        "Visualiza temperatura, humedad de la tierra y luminosidad en tiempo real con alertas y avisos instantáneos.",
        "Organiza tus plantas por conjuntos, controla bombas de riego y mantén tus configuraciones seguras localmente."
    )

    val icons = listOf(
        Icons.Default.LocalFlorist,
        Icons.Default.Thermostat,
        Icons.Default.WaterDrop
    )

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icons[page], contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(60.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = titles[page],
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = descriptions[page],
                    style = TextStyle(fontSize = 15.sp, color = Color.Gray),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) PrimaryGreen else Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onFinish()
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(
                text = if (pagerState.currentPage == 2) "Comenzar" else "Siguiente",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ----------------------------------------------------
// 1. PESTAÑA INICIO (Con Logo Corporativo Clickeable)
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    val plants = PlantRepository.plants
    val alerts = PlantRepository.alerts
    val plantPagerState = rememberPagerState(pageCount = { plants.size.coerceAtLeast(1) })
    val alertPagerState = rememberPagerState(pageCount = { alerts.size.coerceAtLeast(1) })

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Hola de nuevo,", style = TextStyle(fontSize = 14.sp, color = Color.Gray))
                Text(
                    text = if (PlantRepository.isLoggedIn) PlantRepository.userName else "Invitado",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                )
            }

            // Logo Corporativo FloraData en la esquina superior derecha
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable {
                        navController.navigate(BottomScreen.Profile.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.floradata.R.drawable.ic_floradata_logo),
                    contentDescription = "Logo FloraData",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(45.dp).clip(CircleShape).background(Color(0xFFE0F2F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF00796B))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nivel del Estanque de Agua", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark))
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { PlantRepository.tankWaterLevel / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00796B),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
                Text("${PlantRepository.tankWaterLevel}%", fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
            }
        }

        Text("Tus Plantas", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen))

        if (plants.isNotEmpty()) {
            HorizontalPager(
                state = plantPagerState,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) { page ->
                val plant = plants[page]
                Card(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).clickable { navController.navigate("plant_detail/${plant.id}") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(plant.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Conjunto: ${plant.section}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("Humedad Tierra: ${plant.soilHumidity}%", color = Color(0xFF81C784), fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBgColor)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay plantas registradas", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        Text("Avisos Recientes", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen))
        if (alerts.isNotEmpty()) {
            HorizontalPager(
                state = alertPagerState,
                modifier = Modifier.fillMaxWidth().height(90.dp)
            ) { page ->
                val alert = alerts[page]
                Card(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).clickable { navController.navigate("plant_detail/${alert.plantId}") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(alert.time, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.plantName, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 13.sp)
                            Text(alert.message, color = Color(0xFF37474F), fontSize = 12.sp, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBgColor)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Todo en orden, sin avisos recientes", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. PESTAÑA MIS PLANTAS
// ----------------------------------------------------
@Composable
fun PlantsScreen(navController: androidx.navigation.NavController) {
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    var sectionToDelete by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Mis Plantas",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                )
            }

            val sections = PlantRepository.sections
            val plants = PlantRepository.plants

            if (plants.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalFlorist,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text(
                                "No tienes plantas registradas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextDark
                            )
                            Text(
                                "Añade tu primera planta para comenzar el monitoreo.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sections) { section ->
                    val plantList = plants.filter { it.section == section }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("section_detail/$section") },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryGreen)
                                    Text(
                                        text = section,
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    )
                                }
                                IconButton(
                                    onClick = { sectionToDelete = section },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar Conjunto",
                                        tint = Color.LightGray
                                    )
                                }
                            }

                            if (plantList.isEmpty()) {
                                Text(
                                    "No hay plantas en este conjunto.",
                                    style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(plantList) { plant ->
                                        Card(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(140.dp)
                                                .clickable { navController.navigate("plant_detail/${plant.id}") },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = AppBgColor),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryGreen.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (plant.imageUri != null) {
                                                        AsyncImage(
                                                            model = plant.imageUri,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.LocalFlorist,
                                                            contentDescription = null,
                                                            tint = PrimaryGreen,
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = plant.name,
                                                        style = TextStyle(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextDark
                                                        ),
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Humedad: ${plant.soilHumidity}%",
                                                        style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Botón de agregar sección integrado de forma fluida
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { showAddSectionDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Agregar sección",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Botón flotante (*FloatingActionButton*) posicionado de manera segura
        FloatingActionButton(
            onClick = { navController.navigate("add_plant_step1") },
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar Planta")
        }

        if (showAddSectionDialog) {
            AlertDialog(
                onDismissRequest = { showAddSectionDialog = false },
                title = { Text("Nuevo Conjunto") },
                text = {
                    OutlinedTextField(
                        value = newSectionName,
                        onValueChange = { newSectionName = it },
                        label = { Text("Nombre del conjunto") },
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSectionName.isNotBlank()) {
                                PlantRepository.addSection(newSectionName)
                                newSectionName = ""
                                showAddSectionDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancelar") }
                }
            )
        }

        sectionToDelete?.let { section ->
            AlertDialog(
                onDismissRequest = { sectionToDelete = null },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Deseas eliminar el conjunto '$section'? Las plantas que contiene pasarán al conjunto 'General' y no se borrarán.") },
                confirmButton = {
                    Button(
                        onClick = {
                            PlantRepository.deleteSection(section)
                            sectionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { sectionToDelete = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ----------------------------------------------------
// 3. PESTAÑA DE DETALLE DE CONJUNTO
// ----------------------------------------------------
@Composable
fun SectionDetailScreen(sectionName: String, navController: androidx.navigation.NavController) {
    val plantList = PlantRepository.plants.filter { it.section == sectionName }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGreen)
            }
            Text(text = "Conjunto: $sectionName", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen))
        }

        if (plantList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay plantas en este conjunto.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(plantList) { plant ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("plant_detail/${plant.id}") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (plant.imageUri != null) {
                                    AsyncImage(model = plant.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.LocalFlorist, contentDescription = null, tint = PrimaryGreen)
                                }
                            }
                            Column {
                                Text(plant.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark))
                                Text("Humedad Tierra: ${plant.soilHumidity}%", style = TextStyle(fontSize = 13.sp, color = Color.Gray))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. FLUJO DE CREACIÓN: PASO 1
// ----------------------------------------------------
@Composable
fun AddPlantStep1Screen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    var plantName by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && tempCameraUri != null) imageUri = tempCameraUri
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGreen)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paso 1: Datos de la Planta", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark))
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(CardBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("Sin foto seleccionada", color = Color.Gray, fontSize = 13.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CardBgColor, contentColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Galería")
            }

            Button(
                onClick = {
                    val uri = createImageUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CardBgColor, contentColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cámara")
            }
        }

        OutlinedTextField(
            value = plantName,
            onValueChange = { plantName = it },
            label = { Text("Nombre de la planta") },
            textStyle = TextStyle(color = TextDark, fontSize = 16.sp), // Color de texto visible
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (plantName.isNotBlank()) {
                    val encodedUri = Uri.encode(imageUri?.toString() ?: "null")
                    navController.navigate("add_plant_step2/$plantName/$encodedUri")
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Siguiente: Elegir Conjunto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ----------------------------------------------------
// 5. FLUJO DE CREACIÓN: PASO 2
// ----------------------------------------------------
@Composable
fun AddPlantStep2Screen(plantName: String, imageUri: String?, navController: androidx.navigation.NavController) {
    var selectedSection by remember { mutableStateOf(PlantRepository.sections.firstOrNull() ?: "General") }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGreen)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paso 2: Seleccionar Conjunto", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark))
        }

        Text("¿A qué conjunto pertenece $plantName?", style = TextStyle(color = Color.Gray))

        PlantRepository.sections.forEach { section ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedSection = section },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (selectedSection == section) PrimaryGreen else CardBgColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = if (selectedSection == section) Color.White else PrimaryGreen)
                    Text(section, style = TextStyle(fontWeight = FontWeight.Bold, color = if (selectedSection == section) Color.White else TextDark))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                PlantRepository.addPlant(plantName, selectedSection, imageUri)
                navController.popBackStack("plants", inclusive = false)
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Finalizar y Guardar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ----------------------------------------------------
// 6. PANTALLA DE DETALLE DE PLANTA
// ----------------------------------------------------
@Composable
fun PlantDetailScreen(plantId: Int, navController: androidx.navigation.NavController) {
    val plant = PlantRepository.plants.find { it.id == plantId } ?: return
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(plant.name) }
    var editedImageUri by remember { mutableStateOf(plant.imageUri) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) editedImageUri = uri.toString()
    }

    val tempDanger = plant.isOnline && (plant.airTemperature < 18.0 || plant.airTemperature > 28.0)
    val tempHigh = plant.airTemperature > 28.0
    val soilDanger = plant.isOnline && (plant.soilHumidity < 30 || plant.soilHumidity > 70)
    val soilHigh = plant.soilHumidity > 70
    val airHumDanger = plant.isOnline && (plant.airHumidity < 30 || plant.airHumidity > 60)
    val airHumHigh = plant.airHumidity > 60
    val luxDanger = plant.isOnline && (plant.luminosity < 400 || plant.luminosity > 1400)
    val luxHigh = plant.luminosity > 1400

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Encabezado con retroceso, título y botón de edición
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGreen)
                }
                Text(text = "Detalle de Planta", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark))
                IconButton(onClick = {
                    if (isEditing) {
                        PlantRepository.updatePlantInfo(plant.id, editedName, plant.section, editedImageUri)
                    }
                    isEditing = !isEditing
                }) {
                    Icon(imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryGreen)
                }
            }
        }

        item {
            // Tarjeta de Información de la Planta y Banner Offline
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBgColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.1f))
                            .then(if (isEditing) Modifier.clickable { galleryLauncher.launch("image/*") } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentUri = if (isEditing) editedImageUri else plant.imageUri
                        if (currentUri != null) {
                            AsyncImage(model = currentUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.LocalFlorist, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(45.dp))
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Nombre") },
                            textStyle = TextStyle(color = TextDark, fontSize = 16.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Text(plant.name, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen), textAlign = TextAlign.Center)
                        Text("Conjunto: ${plant.section}", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)

                        if (!plant.isOnline) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.SignalWifiOff, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                    Text("Sensor Offline / Sin señal", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Sensores Ambientales", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorWidget(title = "Temp. Aire", value = "${plant.airTemperature} °C", icon = Icons.Default.Thermostat, isDanger = tempDanger, isTooHigh = tempHigh, isOnline = plant.isOnline, modifier = Modifier.weight(1f))
                SensorWidget(title = "Hum. Tierra", value = "${plant.soilHumidity}%", icon = Icons.Default.Water, isDanger = soilDanger, isTooHigh = soilHigh, isOnline = plant.isOnline, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorWidget(title = "Hum. Aire", value = "${plant.airHumidity}%", icon = Icons.Default.Air, isDanger = airHumDanger, isTooHigh = airHumHigh, isOnline = plant.isOnline, modifier = Modifier.weight(1f))
                SensorWidget(title = "Luminosidad", value = "${plant.luminosity} Lux", icon = Icons.Default.WbSunny, isDanger = luxDanger, isTooHigh = luxHigh, isOnline = plant.isOnline, modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Eliminar Planta", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar la planta '${plant.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        PlantRepository.deletePlant(plant.id)
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// Widget de Sensor actualizado con soporte para estado Offline y SignalWifiOff
@Composable
fun SensorWidget(
    title: String,
    value: String,
    icon: ImageVector,
    isDanger: Boolean,
    isTooHigh: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val displayValue = if (isOnline) value else "--"

    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, contentDescription = null, tint = if (isOnline) PrimaryGreen else Color.Gray, modifier = Modifier.size(18.dp))
                    Text(title, style = TextStyle(fontSize = 12.sp, color = Color.Gray))
                }
                Text(displayValue, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isOnline) TextDark else Color.Gray))
            }

            if (!isOnline) {
                // Icono de Wi-Fi sin señal a la derecha
                Icon(
                    imageVector = Icons.Default.SignalWifiOff,
                    contentDescription = "Sin señal",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isDanger) {
                Icon(
                    imageVector = if (isTooHigh) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Peligro",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 7. PESTAÑA AVISOS
// ----------------------------------------------------
@Composable
fun AlertsScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Avisos", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen))

        val alerts = PlantRepository.alerts
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF81C784).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
                    }
                    Text("Todo en orden", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("No hay avisos ni alertas pendientes en tus cultivos.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("plant_detail/${alert.plantId}") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = alert.time,
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray),
                                modifier = Modifier.width(70.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = alert.plantName, style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 14.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = alert.message, style = TextStyle(color = TextDark, fontSize = 13.sp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. PESTAÑA PERFIL
// ----------------------------------------------------
@Composable
fun ProfileScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape).background(PrimaryGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (PlantRepository.isLoggedIn) PlantRepository.userName else "Modo Invitado",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
            )
            Text(
                text = if (PlantRepository.isLoggedIn) PlantRepository.userEmail else "Fase de Pruebas",
                style = TextStyle(fontSize = 13.sp, color = Color.Gray)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Configuraciones del Sistema", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(AppBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Riego automático", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark))
                            Text("Basado en humedad de tierra", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                        }
                    }
                    Switch(
                        checked = PlantRepository.autoWateringEnabled,
                        onCheckedChange = { PlantRepository.toggleAutoWatering(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(AppBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Notificaciones de avisos", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark))
                            Text("Alertas de estanque y sensores", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                        }
                    }
                    Switch(
                        checked = PlantRepository.pushNotificationsEnabled,
                        onCheckedChange = { PlantRepository.togglePushNotifications(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(AppBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Modo ahorro IoT", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark))
                            Text("Optimización de energía en nodos", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                        }
                    }
                    Switch(
                        checked = PlantRepository.iotPowerSaving,
                        onCheckedChange = { PlantRepository.toggleIotPowerSaving(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(AppBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Alertas sonoras", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark))
                            Text("Sonido en avisos críticos", style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                        }
                    }
                    Switch(
                        checked = PlantRepository.soundAlertsEnabled,
                        onCheckedChange = { PlantRepository.toggleSoundAlerts(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (PlantRepository.isLoggedIn) {
            Button(
                onClick = { PlantRepository.logout() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { navController.navigate("login_screen") },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Iniciar Sesión / Registrarse", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 9. PANTALLA DE INICIO DE SESIÓN / REGISTRO
// ----------------------------------------------------
@Composable
fun LoginScreen(navController: androidx.navigation.NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isNewAccount by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBgColor).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(PrimaryGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalFlorist, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isNewAccount) "Crear Nueva Cuenta" else "Iniciar Sesión en FloraData",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isNewAccount) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    PlantRepository.login(email, if (isNewAccount) name else null)
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(if (isNewAccount) "Registrarse" else "Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { isNewAccount = !isNewAccount }) {
            Text(
                text = if (isNewAccount) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí",
                color = PrimaryGreen
            )
        }
    }
}