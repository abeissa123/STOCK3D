package com.example.ui.screens

import android.widget.Toast
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

    val context = LocalContext.current

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

    Scaffold(
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
                    IconButton(
                        onClick = {
                            val path = viewModel.exportStockToCSV()
                            if (path != null) {
                                Toast.makeText(context, "Exporté : $path", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Échec de l'exportation", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Exporter CSV", tint = CyanNeon)
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
                    Column {
                        Text(
                            "Bonjour, Gestionnaire 👋",
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
                        onClick = { viewModel.resetDatabase() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SlateMedium),
                        modifier = Modifier.border(0.5.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Réinitialiser", tint = Color.LightGray)
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
