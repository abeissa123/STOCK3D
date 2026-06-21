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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
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
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(viewModel = viewModel, onLoginSuccess = {})
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Handle preselected coordinates across screens when navigating back & forth
                    var preselectedCoords by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
                    var preselectedSku by remember { mutableStateOf<String?>(null) }

                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isExpanded = configuration.screenWidthDp >= 600

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SlateDark,
                    bottomBar = {
                        if (!isExpanded) {
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
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (!isExpanded) innerPadding.calculateBottomPadding() else 0.dp)
                    ) {
                        if (isExpanded) {
                            NavigationRail(
                                containerColor = SlateMedium,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = "Stock3D",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(36.dp).padding(4.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                NavigationRailItem(
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
                                            contentDescription = "Dashboard"
                                        ) 
                                    },
                                    label = { Text("Tableau", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                NavigationRailItem(
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
                                            contentDescription = "Entrepôt"
                                        ) 
                                    },
                                    label = { Text("3D", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                NavigationRailItem(
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
                                    label = { Text("Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                NavigationRailItem(
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
                                    label = { Text("Caisse", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                NavigationRailItem(
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
                                    label = { Text("Scanner", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                NavigationRailItem(
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
                                            contentDescription = "IA"
                                        ) 
                                    },
                                    label = { Text("IA", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SlateDark,
                                        selectedTextColor = CyanNeon,
                                        indicatorColor = CyanNeon,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
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
                                    preselectedSku = preselectedSku,
                                    onClearPreselection = { 
                                        preselectedCoords = null 
                                        preselectedSku = null
                                    }
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
                                        preselectedSku = sku
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

                        // --- Real-time Visual Alerts Overlay for Low Stock / Ruptures ---
                        val activeAlerts by viewModel.activeAlerts.collectAsState()
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeAlerts.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .zIndex(99f)
                        ) {
                            val alertProduct = activeAlerts.firstOrNull()
                            if (alertProduct != null) {
                                val isRupture = alertProduct.quantity <= 0
                                val cardBorderColor = if (isRupture) Color(0xFFFF5252) else Color(0xFFFFAB40)
                                val containerBg = Color(0xFF1E222D)
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSearchQuery(alertProduct.name)
                                            navController.navigate("catalog")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = containerBg),
                                    border = BorderStroke(1.5.dp, cardBorderColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isRupture) Color(0xFFFF5252).copy(alpha = 0.15f) else Color(0xFFFFAB40).copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isRupture) Icons.Default.Error else Icons.Default.Warning,
                                                contentDescription = "Alerte stock",
                                                tint = if (isRupture) Color(0xFFFF5252) else Color(0xFFFFAB40),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isRupture) "ALERTE RUPTURE DE STOCK !" else "ALERTE NIVEAU STOCK BAS !",
                                                color = if (isRupture) Color(0xFFFF5252) else Color(0xFFFFAB40),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = alertProduct.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Quantité: ${alertProduct.quantity} (Seuil: ${alertProduct.minThreshold})",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "• Rayon ${alertProduct.locationShelf}",
                                                    color = CyanNeon,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        if (activeAlerts.size > 1) {
                                            Surface(
                                                color = SlateMedium,
                                                shape = CircleShape,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "+${activeAlerts.size - 1}",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                viewModel.dismissAlert(alertProduct.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Fermer l'alerte",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } // closes Box
                    } // closes Row
                } // closes innerPadding lambda
                } // closes else block
            } // closes Theme
        }
    }
}
