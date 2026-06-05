package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.ProductEntity
import com.example.data.StockRepository
import com.example.ui.StockViewModel
import com.example.ui.StockViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateMedium
import com.example.ui.theme.CyanNeon

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Asynchronously instantiate Database and Repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = StockRepository(database.productDao)

        // 2. Instantiate our central ViewModel through our Factory
        val factory = StockViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[StockViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Handle preselected coordinates across screens when navigating back & forth
                var preselectedCoords by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
                var preselectedSku by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SlateDark,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SlateMedium,
                            tonalElevation = 8.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Navigation Item: Dashboard
                            NavigationBarItem(
                                selected = currentRoute == "dashboard",
                                onClick = {
                                    navController.navigate("dashboard") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute == "dashboard") Icons.Default.Dashboard else Icons.Outlined.Dashboard, 
                                        contentDescription = "Tableau de Bord"
                                    ) 
                                },
                                label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )

                            // Navigation Item: 3D Warehouse representation
                            NavigationBarItem(
                                selected = currentRoute == "warehouse_3d",
                                onClick = {
                                    navController.navigate("warehouse_3d") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute == "warehouse_3d") Icons.Default.ViewInAr else Icons.Outlined.ViewInAr, 
                                        contentDescription = "Entrepôt 3D"
                                    ) 
                                },
                                label = { Text("Entrepôt 3D", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )

                            // Navigation Item: Products catalog/list
                            NavigationBarItem(
                                selected = currentRoute?.startsWith("catalog") == true,
                                onClick = {
                                    navController.navigate("catalog") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute?.startsWith("catalog") == true) Icons.Default.Inventory2 else Icons.Outlined.Inventory2, 
                                        contentDescription = "Catalogue"
                                    ) 
                                },
                                label = { Text("Catalogue", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )

                            // Navigation Item: POS Cash register (Vente)
                            NavigationBarItem(
                                selected = currentRoute == "sales",
                                onClick = {
                                    navController.navigate("sales") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute == "sales") Icons.Default.Payments else Icons.Outlined.Payments, 
                                        contentDescription = "Caisse"
                                    ) 
                                },
                                label = { Text("Caisse", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )

                            // Navigation Item: Barcode Scanner
                            NavigationBarItem(
                                selected = currentRoute == "scanner",
                                onClick = {
                                    navController.navigate("scanner") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute == "scanner") Icons.Default.QrCodeScanner else Icons.Outlined.QrCodeScanner, 
                                        contentDescription = "Scanner"
                                    ) 
                                },
                                label = { Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )

                            // Navigation Item: Gemini Predictive Analyzer
                            NavigationBarItem(
                                selected = currentRoute == "ai_prediction",
                                onClick = {
                                    navController.navigate("ai_prediction") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        if (currentRoute == "ai_prediction") Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome, 
                                        contentDescription = "IA Prédictive"
                                    ) 
                                },
                                label = { Text("IA", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDark,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // --- Route 1: Dashboard ---
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToScans = { navController.navigate("scanner") },
                                    onNavigateToProducts = { navController.navigate("catalog") },
                                    onNavigateToSales = { navController.navigate("sales") }
                                )
                            }

                            // --- Route 2: 3D warehouse isometric ---
                            composable("warehouse_3d") {
                                Warehouse3DScreen(
                                    viewModel = viewModel,
                                    onNavigateToProductDetail = { product ->
                                        // Focus search/list filter on the clicked item catalog
                                        viewModel.setSearchQuery(product.name)
                                        navController.navigate("catalog")
                                    },
                                    onNavigateToProductAdd = { shelf, col, lvl ->
                                        preselectedCoords = Triple(shelf, col, lvl)
                                        navController.navigate("catalog")
                                    }
                                )
                            }

                            // --- Route 3: Catalog master overview ---
                            composable("catalog") {
                                ProductListScreen(
                                    viewModel = viewModel,
                                    preselectedCoordinates = preselectedCoords,
                                    onClearPreselection = { preselectedCoords = null }
                                )
                            }

                            // --- Route 3b: Point of Sale Terminal ---
                            composable("sales") {
                                SalesScreen(
                                    viewModel = viewModel,
                                    onNavigateToCatalog = { navController.navigate("catalog") }
                                )
                            }

                            // --- Route 4: Scanner Simulator ---
                            composable("scanner") {
                                ScanBarcodeScreen(
                                    viewModel = viewModel,
                                    onNavigateToProductDetail = { product ->
                                        viewModel.setSearchQuery(product.name)
                                        navController.navigate("catalog")
                                    },
                                    onNavigateToProductAddWithSku = { sku ->
                                        preselectedCoords = Triple(1, 1, 1)
                                        // Open standard additions
                                        viewModel.setSearchQuery("") // reset query
                                        navController.navigate("catalog")
                                    }
                                )
                            }

                            // --- Route 5: Predictive AI Report ---
                            composable("ai_prediction") {
                                AiPredictionScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
