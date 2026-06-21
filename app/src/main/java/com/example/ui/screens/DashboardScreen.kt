package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.TransactionEntity
import com.example.ui.StockViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.graphics.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StockViewModel,
    onNavigateToScans: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToSales: () -> Unit
) {
    val products by viewModel.productsFlow.collectAsState()
    val transactions by viewModel.transactionsFlow.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val lowStockAlertsCount by viewModel.lowStockCount.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()

    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    val shareFile = { filePath: String, mimeType: String ->
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.stock3d.kxbzqy.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Partager le rapport via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur de partage : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Compute key statistics
    val totalProductsCount = products.size
    val totalPhysicalItems = products.sumOf { it.quantity }
    val totalValuation = products.sumOf { it.quantity * it.price }

    // Grouping for custom charts
    val categoryDistribution = products.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.quantity } }
    
    // Sort transactions chronologically for the flow graph
    val latestTransactions = transactions.take(15)

    // Formatter for FCFA
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = 0
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1200.dp),
            topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Tableau de Bord Stock3D", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Exporter", tint = CyanNeon)
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false },
                            modifier = Modifier.background(SlateMedium)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Rapport PDF Complet", color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    val path = viewModel.exportStockToPDF()
                                    if (path != null) {
                                        Toast.makeText(context, "PDF généré avec succès ! 📄", Toast.LENGTH_SHORT).show()
                                        shareFile(path, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "Échec de la génération PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Inventaire Excel (CSV)", color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showExportMenu = false
                                    val path = viewModel.exportStockToCSV()
                                    if (path != null) {
                                        Toast.makeText(context, "CSV exporté avec succès ! 📊", Toast.LENGTH_SHORT).show()
                                        shareFile(path, "text/csv")
                                    } else {
                                        Toast.makeText(context, "Échec de l'exportation CSV", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.authService.logout()
                            Toast.makeText(context, "Session de déconnexion réussie", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion", tint = CrimsonRed)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateDark
                )
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val friendlyName = remember(userEmail) {
                            userEmail?.substringBefore("@")
                                ?.split(".")
                                ?.joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
                                ?: "Gestionnaire"
                        }
                        Text(
                            "Bonjour, $friendlyName 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Voici l'état actuel de votre entrepôt.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    
                    FilledIconButton(
                        onClick = { 
                            viewModel.refreshApp() 
                            Toast.makeText(context, "Stock3D actualisé ! ✅", Toast.LENGTH_SHORT).show()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SlateMedium),
                        modifier = Modifier.border(0.5.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.LightGray)
                    }
                }
            }

            // Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // KPI Total Value
                    DashboardKpiCard(
                        modifier = Modifier.weight(1.2f),
                        title = "Valeur Active Stock",
                        value = "${currencyFormatter.format(totalValuation)} FCFA",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = CyanNeon,
                        subtitle = "$totalPhysicalItems articles physiques"
                    )

                    // KPI Alerts count
                    DashboardKpiCard(
                        modifier = Modifier.weight(0.8f),
                        title = "Alertes Stock",
                        value = "$lowStockAlertsCount",
                        icon = Icons.Default.Warning,
                        accentColor = if (lowStockAlertsCount > 0) CrimsonRed else EmeraldGlow,
                        subtitle = if (lowStockAlertsCount > 0) "Rupture ou stock bas" else "Tous les niveaux OK"
                    )
                }
            }

            // Real-Time Alert Details Listing Section
            if (lowStockAlertsCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateMedium),
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Alertes de Stock",
                                        tint = CrimsonRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Seuils Critiques Atteints ($lowStockAlertsCount)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.setCategoryFilter("🚨 Alertes")
                                        onNavigateToProducts()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CyanNeon),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Régler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val alertProductsList = products.filter { it.isLowStock() || it.isOutOfStock() }
                            alertProductsList.take(3).forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Rayon ${prod.locationShelf}-${prod.locationColumn}-${prod.locationLevel} • Seuil d'alerte: ${prod.minThreshold}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    val pillColor = if (prod.isOutOfStock()) CrimsonRed else AmberGlow
                                    val qtyText = if (prod.isOutOfStock()) "RUPTURE" else "${prod.quantity} Restants"
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(pillColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .border(0.5.dp, pillColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            qtyText,
                                            color = pillColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Large POS wide landing card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSales() },
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(EmeraldGlow.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = "Caisse", tint = EmeraldGlow)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Caisse & Vente Directe",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Enregistrez des ventes et encaissez l'argent en direct",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Ouvrir",
                            tint = Color.LightGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMiniKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Produits Référencés",
                        value = "$totalProductsCount",
                        icon = Icons.Default.Inventory2,
                        accentColor = Color.White
                    )
                    DashboardMiniKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Scan Scanner",
                        value = "Démarrer",
                        icon = Icons.Default.QrCodeScanner,
                        accentColor = CyanNeon,
                        onClick = onNavigateToScans
                    )
                }
            }

            // Custom category distribution ring chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Répartition par Catégorie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (products.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun produit en stock", color = Color.Gray)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Draw Category Donuts Chart
                                Box(
                                    modifier = Modifier.size(130.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryDonutChart(categoryDistribution)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Catégories", fontSize = 10.sp, color = Color.Gray)
                                        Text("${categoryDistribution.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Legend listing items
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val totalUnits = categoryDistribution.values.sum().toFloat()
                                    categoryDistribution.entries.take(4).forEachIndexed { index, entry ->
                                        val percent = if (totalUnits > 0) (entry.value / totalUnits * 100).toInt() else 0
                                        val color = getCategoryColor(index)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(entry.key, fontSize = 12.sp, color = Color.White, maxLines = 1)
                                            }
                                            Text("$percent%", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1: Graphiques de Ventes Journalières et Hebdomadaires
            item {
                var selectedChartTab by remember { mutableStateOf(0) } // 0 = Daily, 1 = Weekly
                
                // Compute lists
                val dailyList = remember(transactions) {
                    val list = mutableListOf<Pair<String, Double>>()
                    val sdf = SimpleDateFormat("dd/MM", Locale.FRANCE)
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -i)
                        val dayStart = cal.apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val dayEnd = cal.apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        
                        val dateStr = sdf.format(Date(dayStart))
                        val sum = transactions
                            .filter { it.type == "SORTIE" && it.timestamp in dayStart..dayEnd }
                            .sumOf { it.totalAmount }
                        list.add(dateStr to sum)
                    }
                    list
                }

                val weeklyList = remember(transactions) {
                    val list = mutableListOf<Pair<String, Double>>()
                    val sdfWeek = SimpleDateFormat("dd/MM", Locale.FRANCE)
                    for (i in 3 downTo 0) {
                        val calStart = Calendar.getInstance()
                        calStart.add(Calendar.DAY_OF_YEAR, -((i + 1) * 7 - 1))
                        calStart.set(Calendar.HOUR_OF_DAY, 0)
                        calStart.set(Calendar.MINUTE, 0)
                        calStart.set(Calendar.SECOND, 0)
                        calStart.set(Calendar.MILLISECOND, 0)
                        val weekStart = calStart.timeInMillis

                        val calEnd = Calendar.getInstance()
                        calEnd.add(Calendar.DAY_OF_YEAR, -(i * 7))
                        calEnd.set(Calendar.HOUR_OF_DAY, 23)
                        calEnd.set(Calendar.MINUTE, 59)
                        calEnd.set(Calendar.SECOND, 59)
                        calEnd.set(Calendar.MILLISECOND, 999)
                        val weekEnd = calEnd.timeInMillis

                        val label = if (i == 0) "Cette Sem." else "Sem. -${i}"
                        val sum = transactions
                            .filter { it.type == "SORTIE" && it.timestamp in weekStart..weekEnd }
                            .sumOf { it.totalAmount }
                        list.add(label to sum)
                    }
                    list
                }

                val currentPeriodTotal = if (selectedChartTab == 0) {
                    dailyList.sumOf { it.second }
                } else {
                    weeklyList.sumOf { it.second }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Analyses des Ventes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    if (selectedChartTab == 0) "Sur les 7 derniers jours" else "Sur les 4 dernières semaines",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            
                            // Segmented control pills
                            Row(
                                modifier = Modifier
                                    .background(SlateDark, RoundedCornerShape(20.dp))
                                    .border(1.dp, SlateCardBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedChartTab == 0) CyanNeon else Color.Transparent,
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable { selectedChartTab = 0 }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Jour",
                                        color = if (selectedChartTab == 0) SlateDark else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedChartTab == 1) CyanNeon else Color.Transparent,
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable { selectedChartTab = 1 }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Sem.",
                                        color = if (selectedChartTab == 1) SlateDark else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Total KPI
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${currencyFormatter.format(currentPeriodTotal)} FCFA",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = CyanNeon
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ventes cumulées",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Render Graph
                        if (selectedChartTab == 0) {
                            SimpleLineChart(
                                data = dailyList,
                                accentColor = CyanNeon,
                                formatCurrency = { "${currencyFormatter.format(it)} F" }
                            )
                        } else {
                            SimpleBarChart(
                                data = weeklyList,
                                accentColor = EmeraldGlow,
                                formatCurrency = { "${currencyFormatter.format(it)} F" }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Custom labels Row below
                        val dataToRender = if (selectedChartTab == 0) dailyList else weeklyList
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dataToRender.forEach { (label, value) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (value > 0) {
                                            if (value >= 1000) {
                                                "${(value / 1000).toInt()}k"
                                            } else {
                                                "${value.toInt()}"
                                            }
                                        } else "0",
                                        color = if (value > 0) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Historique Complet des Transactions (Flux Entrée / Sortie)
            item {
                var historyFilter by remember { mutableStateOf("TOUT") } // "TOUT", "ENTRÉE", "SORTIE"
                
                val filteredHistory = remember(transactions, historyFilter) {
                    when (historyFilter) {
                        "ENTRÉE" -> transactions.filter { it.type == "ENTRÉE" }
                        "SORTIE" -> transactions.filter { it.type == "SORTIE" }
                        else -> transactions
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Historique",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Historique des Flux",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Text(
                                "${filteredHistory.size} total",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Small chips row for filtering
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("TOUT" to "Tout", "SORTIE" to "Ventes 💻", "ENTRÉE" to "Stocks 📦").forEach { (key, label) ->
                                val selected = historyFilter == key
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selected) CyanNeon.copy(alpha = 0.15f) else SlateDark,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) CyanNeon else SlateCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { historyFilter = key }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) CyanNeon else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        if (filteredHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Aucune transaction enregistrée",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredHistory.take(8).forEach { tx ->
                                    val isSale = tx.type == "SORTIE"
                                    val statusColor = if (isSale) EmeraldGlow else CyanNeon
                                    val icon = if (isSale) Icons.Default.Payments else Icons.Default.Inventory2
                                    val descPrefix = if (isSale) "Vendu" else "Reçu"
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SlateDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .border(0.5.dp, SlateCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = tx.type,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(10.dp))
                                            
                                            Column {
                                                Text(
                                                    tx.productName,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "${descPrefix} : ${tx.quantity} unité(s) • ${tx.operatorName}",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "${if (isSale) "" else "+"}${currencyFormatter.format(tx.totalAmount)} F",
                                                color = if (isSale) EmeraldGlow else CyanNeon,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            val txTime = remember(tx.timestamp) {
                                                val sdf = SimpleDateFormat("dd/MM à HH:mm", Locale.FRANCE)
                                                sdf.format(Date(tx.timestamp))
                                            }
                                            Text(
                                                text = txTime,
                                                color = Color.Gray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Activity Log (Console terminal styling)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = "Logs", tint = CyanNeon, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Journal Log Logistique (Temps Réel)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(CrimsonRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CrimsonRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(CrimsonRed, RoundedCornerShape(3.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LIVE", color = CrimsonRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            color = SlateDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                reverseLayout = false,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(notifications) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("Alerte", ignoreCase = true)) CrimsonRed else if (log.contains("Nouveau", ignoreCase = true)) CyanNeon else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Spacer
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    }
}

@Composable
fun DashboardKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun DashboardMiniKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Card(
        modifier = modifier.then(clickableModifier),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = Color.Gray, fontSize = 11.sp)
                Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CategoryDonutChart(distribution: Map<String, Int>) {
    val total = distribution.values.sum().toFloat()
    val entries = distribution.entries.toList()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(distribution) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        entries.forEachIndexed { index, entry ->
            val sweep = if (total > 0) (entry.value / total * 360f) else 0f
            val color = getCategoryColor(index)
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep * animatedProgress.value,
                useCenter = false,
                style = Stroke(width = 25f),
                size = Size(size.width - 25f, size.height - 25f),
                alpha = 0.9f
            )
            startAngle += sweep
        }
    }
}

fun getCategoryColor(index: Int): Color {
    val colors = listOf(
        CyanNeon,
        EmeraldGlow,
        AmberGlow,
        Color(0xFFA855F7), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6)  // Blue
    )
    val safeIndex = ((index % colors.size) + colors.size) % colors.size
    return colors[safeIndex]
}

val BoxBorder = Modifier.border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))

@Composable
fun SimpleLineChart(
    data: List<Pair<String, Double>>,
    accentColor: Color,
    formatCurrency: (Double) -> String
) {
    if (data.isEmpty()) return
    val maxVal = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(100.0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(SlateDark, RoundedCornerShape(12.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = if (data.size > 1) width / (data.size - 1) else width
            
            // Draw 4 soft horizontal gridlines
            val gridCount = 4
            for (i in 0..gridCount) {
                val gridY = height * (1f - i.toFloat() / gridCount)
                drawLine(
                    color = SlateCardBorder.copy(alpha = 0.4f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }
            
            // Generate points
            val points = data.mapIndexed { idx, item ->
                val x = idx * spacing
                val ratio = (item.second / maxVal).toFloat()
                val y = height - (ratio * height)
                Offset(x, y)
            }
            
            if (points.isNotEmpty()) {
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, height)
                    lineTo(points[0].x, height)
                    close()
                }
                
                // Draw dynamic gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
                
                // Draw main connection line
                drawPath(
                    path = strokePath,
                    color = accentColor,
                    style = Stroke(width = 4f)
                )
                
                // Draw nodes
                points.forEach { point ->
                    drawCircle(
                        color = accentColor,
                        radius = 8f,
                        center = point
                    )
                    drawCircle(
                        color = SlateDark,
                        radius = 4f,
                        center = point
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    accentColor: Color,
    formatCurrency: (Double) -> String
) {
    if (data.isEmpty()) return
    val maxVal = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(100.0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(SlateDark, RoundedCornerShape(12.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = (width / data.size) * 0.45f
            val spacing = width / data.size
            
            // Draw 4 soft horizontal gridlines
            val gridCount = 4
            for (i in 0..gridCount) {
                val gridY = height * (1f - i.toFloat() / gridCount)
                drawLine(
                    color = SlateCardBorder.copy(alpha = 0.4f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1f
                )
            }
            
            data.forEachIndexed { idx, item ->
                val ratio = (item.second / maxVal).toFloat()
                val barHeight = (ratio * height).coerceAtLeast(5f) // min height so it's slightly visible
                val x = (idx * spacing) + (spacing - barWidth) / 2
                val y = height - barHeight
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.4f))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }
    }
}

