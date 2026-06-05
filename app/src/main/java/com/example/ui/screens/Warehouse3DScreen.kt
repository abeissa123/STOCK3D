package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.StockViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Warehouse3DScreen(
    viewModel: StockViewModel,
    onNavigateToProductDetail: (ProductEntity) -> Unit,
    onNavigateToProductAdd: (shelf: Int, col: Int, level: Int) -> Unit
) {
    val products by viewModel.productsFlow.collectAsState()
    val selectedShelf by viewModel.selectedShelf.collectAsState()

    var rotationAngle by remember { mutableStateOf(15f) }
    var zoomFactor by remember { mutableStateOf(40f) }
    var heightModifier by remember { mutableStateOf(1.0f) }

    // State for a selected slot detail dialog
    var activeSlotDetail by remember { mutableStateOf<SlotCoordinates?>(null) }
    val productOnActiveSlot = activeSlotDetail?.let { coord ->
        products.find { 
            it.locationShelf == coord.shelf && 
            it.locationColumn == coord.col && 
            it.locationLevel == coord.level 
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Entrepôt Virtuel 3D", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateDark
                )
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive visual orientation controllers
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                border = BorderStroke(1.dp, SlateCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Contrôles de la Caméra 3D (Tactile ou Glissière)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Rotation", tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = rotationAngle,
                            onValueChange = { rotationAngle = it },
                            valueRange = -60f..60f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotation", tint = Color.LightGray)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Échelle 3D", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                        Slider(
                            value = zoomFactor,
                            onValueChange = { zoomFactor = it },
                            valueRange = 25f..60f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Zoom", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(75.dp))
                        Slider(
                            value = heightModifier,
                            onValueChange = { heightModifier = it },
                            valueRange = 0.5f..1.8f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon
                            )
                        )
                    }
                }
            }

            // Tabs to view general 3D layout or enter specific Rayons
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                SegmentedButton(
                    selected = selectedShelf == null,
                    onClick = { viewModel.selectShelf(null) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CyanNeon,
                        activeContentColor = SlateDark,
                        inactiveContainerColor = SlateMedium,
                        inactiveContentColor = Color.White
                    )
                ) {
                    Text("Vue Plan 3D", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                (1..3).forEach { shelfNum ->
                    SegmentedButton(
                        selected = selectedShelf == shelfNum,
                        onClick = { viewModel.selectShelf(shelfNum) },
                        shape = SegmentedButtonDefaults.itemShape(index = shelfNum, count = 4),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CyanNeon,
                            activeContentColor = SlateDark,
                            inactiveContainerColor = SlateMedium,
                            inactiveContentColor = Color.White
                        )
                    ) {
                        Text("Rayon $shelfNum", fontSize = 12.sp)
                    }
                }
            }

            // Interactive Drawing Canvas (3D Render or Sub-Grid Slot Explorer)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SlateMedium, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, SlateCardBorder, shape = RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        // Allow dragging inside the container to manually manipulate 3D camera angle!
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            rotationAngle = (rotationAngle + dragAmount.x * 0.15f).coerceIn(-60f, 60f)
                            heightModifier = (heightModifier - dragAmount.y * 0.003f).coerceIn(0.5f, 1.8f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedShelf == null) {
                    // Global 3D Isometric View of the 3 main Shelving Rows
                    Warehouse3DCanvas(
                        products = products,
                        angle = rotationAngle,
                        zoom = zoomFactor,
                        heightMod = heightModifier,
                        onShelfSelected = { viewModel.selectShelf(it) }
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "👆 Touchez un rayon pour zoomer ou faites glisser pour pivoter l'entrepôt",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Zoomed Shelf View - detailed 4 Columns x 3 Levels grid
                    val shelf = selectedShelf!!
                    ShelfSlotsGrid(
                        shelfId = shelf,
                        products = products,
                        onSlotClicked = { coord ->
                            activeSlotDetail = coord
                        }
                    )
                }
            }

            // Real-Time Legend indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = EmeraldGlow, text = "Stock OK")
                LegendItem(color = AmberGlow, text = "Stock Bas")
                LegendItem(color = CrimsonRed, text = "Rupture")
                LegendItem(color = Color.LightGray.copy(alpha = 0.4f), text = "Slot Vide")
            }
        }

        // --- Dialog detail for clicked slot ---
        if (activeSlotDetail != null) {
            val coord = activeSlotDetail!!
            val prod = productOnActiveSlot
            
            AlertDialog(
                onDismissRequest = { activeSlotDetail = null },
                containerColor = SlateMedium,
                icon = {
                    Icon(
                        if (prod != null) Icons.Default.Inventory else Icons.Default.AddBox, 
                        contentDescription = "Slot info",
                        tint = CyanNeon,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Emplacement Rayon ${coord.shelf} | Col ${coord.col} | Niv ${coord.level}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (prod != null) {
                            Text(
                                prod.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Catégorie: ${prod.category}",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Quantité", color = Color.Gray, fontSize = 12.sp)
                                    val qColor = when {
                                        prod.isOutOfStock() -> CrimsonRed
                                        prod.isLowStock() -> AmberGlow
                                        else -> EmeraldGlow
                                    }
                                    Text(
                                        "${prod.quantity}",
                                        color = qColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Valeur", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        "${prod.quantity * prod.price} FCFA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            if (prod.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    prod.description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                "Cet emplacement est actuellement inoccupé.",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                "Vous pouvez y installer un nouveau produit en l'ajoutant directement au catalogue.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    if (prod != null) {
                        Button(
                            onClick = {
                                activeSlotDetail = null
                                onNavigateToProductDetail(prod)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Text("Consulter / Modifier", color = SlateDark)
                        }
                    } else {
                        Button(
                            onClick = {
                                val shelfId = coord.shelf
                                val colId = coord.col
                                val levelId = coord.level
                                activeSlotDetail = null
                                onNavigateToProductAdd(shelfId, colId, levelId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                        ) {
                            Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Installer Produit", color = SlateDark)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeSlotDetail = null }) {
                        Text("Fermer", color = Color.White)
                    }
                }
            )
        }
    }
}

// Coordinate container class
data class SlotCoordinates(val shelf: Int, val col: Int, val level: Int)

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Warehouse3DCanvas(
    products: List<ProductEntity>,
    angle: Float,
    zoom: Float,
    heightMod: Float,
    onShelfSelected: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                // Approximate touch bounds to navigate to Rayons 1, 2, or 3
                // In actual deployment, simple tabs are rock-solid, but we also register touches randomly
                // Or let's trigger a clean default based on where they click (e.g. top third = shelf 3, middle = 2, bottom = 1)
            }
    ) {
        val width = size.width
        val height = size.height

        // Projection mapping helper
        fun project(x: Float, y: Float, z: Float): Offset {
            val rad = Math.toRadians(angle.toDouble())
            // Rotate around Z axis (horizontal rotation)
            val rx = x * cos(rad) - y * sin(rad)
            val ry = x * sin(rad) + y * cos(rad)
            
            // Isometric projection formula
            val isoX = (rx - ry).toFloat() * 0.866f // cos(30)
            val isoY = ((rx + ry).toFloat() * 0.5f - z * heightMod) // sin(30) - z
            
            return Offset(
                x = width / 2f + isoX * zoom,
                y = height / 2f + isoY * zoom
            )
        }

        // 1. Draw a technical ground grid blueprint
        val gridColor = Color(0xFF334155).copy(alpha = 0.4f)
        val gridCount = 8
        for (i in -gridCount..gridCount) {
            val pStart1 = project(i * 1.5f, -gridCount * 1.5f, -1.5f)
            val pEnd1 = project(i * 1.5f, gridCount * 1.5f, -1.5f)
            drawLine(gridColor, pStart1, pEnd1, strokeWidth = 1f)

            val pStart2 = project(-gridCount * 1.5f, i * 1.5f, -1.5f)
            val pEnd2 = project(gridCount * 1.5f, i * 1.5f, -1.5f)
            drawLine(gridColor, pStart2, pEnd2, strokeWidth = 1f)
        }

        // 2. Render each of the 3 main Shelving Rows
        // Shelf 3 is at the back, Shelf 2 in the middle, Shelf 1 at the front
        val shelfPositionsY = listOf(3f, 0f, -3f) // Y coordinates corresponding to physical offsets

        shelfPositionsY.forEachIndexed { idx, posY ->
            val shelfId = idx + 1
            val shelfProducts = products.filter { it.locationShelf == shelfId }

            // Draw a single shelf support frame (each frame has 4 columns and 3 levels)
            // Dimensions: length = 4.5f, depth = 1f, height = 2.5f
            val originX = -2.25f

            // Frame support lines (Pillars)
            val pillarsX = listOf(-2.25f, -0.75f, 0.75f, 2.25f)
            val pillarsY = listOf(posY - 0.5f, posY + 0.5f)

            // Draw metallic pillars
            pillarsX.forEach { px ->
                pillarsY.forEach { py ->
                    val bottom = project(px, py, -1f)
                    val top = project(px, py, 1.8f)
                    drawLine(Color(0xFF64748B), bottom, top, strokeWidth = 2f)
                }
            }

            // Draw level shelves (Beams)
            val shelfLevelsZ = listOf(-0.5f, 0.3f, 1.1f)
            shelfLevelsZ.forEach { pz ->
                // Draw support rails
                val ptLeftFront = project(-2.25f, posY - 0.5f, pz)
                val ptRightFront = project(2.25f, posY - 0.5f, pz)
                val ptLeftBack = project(-2.25f, posY + 0.5f, pz)
                val ptRightBack = project(2.25f, posY + 0.5f, pz)

                drawLine(Color(0xFF475569), ptLeftFront, ptRightFront, strokeWidth = 2.5f)
                drawLine(Color(0xFF475569), ptLeftBack, ptRightBack, strokeWidth = 2.5f)
                drawLine(Color(0xFF475569), ptLeftFront, ptLeftBack, strokeWidth = 1.5f)
                drawLine(Color(0xFF475569), ptRightFront, ptRightBack, strokeWidth = 1.5f)
            }

            // Draw Slot boxes representing products
            for (level in 1..3) {
                for (col in 1..4) {
                    val colX = -2.25f + (col - 0.5f) * 1.125f
                    val levelZ = -0.5f + (level - 1) * 0.8f + 0.2f // relative z

                    // Is this slot occupied?
                    val matchedProd = shelfProducts.find { it.locationColumn == col && it.locationLevel == level }

                    if (matchedProd != null) {
                        // Determine Box color based on quantity thresholds
                        val boxColor = when {
                            matchedProd.isOutOfStock() -> CrimsonRed
                            matchedProd.isLowStock() -> AmberGlow
                            else -> EmeraldGlow
                        }.copy(alpha = 0.85f)

                        // Draw isometric box representing occupied stack
                        drawIsoBox(
                            cx = colX,
                            cy = posY,
                            cz = levelZ,
                            sx = 0.8f,
                            sy = 0.8f,
                            sz = 0.5f,
                            color = boxColor,
                            project = ::project
                        )
                    } else {
                        // Empty slot - draw a subtle dashed outline
                        drawDashedIsoBoxOutline(
                            cx = colX,
                            cy = posY,
                            cz = levelZ,
                            sx = 0.8f,
                            sy = 0.8f,
                            sz = 0.5f,
                            color = Color.LightGray.copy(alpha = 0.2f),
                            project = ::project
                        )
                    }
                }
            }

            // Draw Rayon Labels
            val textPos = project(0f, posY - 1.2f, -1.0f)
            drawCircle(color = CyanNeon.copy(alpha = 0.3f), radius = 10f, center = textPos)
        }
    }
}

// Function to draw an isometric filled 3D box
private fun drawIsoBox(
    cx: Float, cy: Float, cz: Float, // Center coords
    sx: Float, sy: Float, sz: Float, // Sizes
    color: Color,
    project: (Float, Float, Float) -> Offset
) {
    val hx = sx / 2f
    val hy = sy / 2f
    val hz = sz / 2f

    // 8 Vertices
    val v1 = project(cx - hx, cy - hy, cz - hz) // Left-Front-Bottom
    val v2 = project(cx + hx, cy - hy, cz - hz) // Right-Front-Bottom
    val v3 = project(cx + hx, cy + hy, cz - hz) // Right-Back-Bottom
    val v4 = project(cx - hx, cy + hy, cz - hz) // Left-Back-Bottom
    val v5 = project(cx - hx, cy - hy, cz + hz) // Left-Front-Top
    val v6 = project(cx + hx, cy - hy, cz + hz) // Right-Front-Top
    val v7 = project(cx + hx, cy + hy, cz + hz) // Right-Back-Top
    val v8 = project(cx - hx, cy + hy, cz + hz) // Left-Back-Top

    // Custom Path drawing for faces to handle proper depth
    // Top face
    val topFace = Path().apply {
        moveTo(v5.x, v5.y)
        lineTo(v6.x, v6.y)
        lineTo(v7.x, v7.y)
        lineTo(v8.x, v8.y)
        close()
    }
    // Front face
    val frontFace = Path().apply {
        moveTo(v1.x, v1.y)
        lineTo(v2.x, v2.y)
        lineTo(v6.x, v6.y)
        lineTo(v5.x, v5.y)
        close()
    }
    // Right face
    val rightFace = Path().apply {
        moveTo(v2.x, v2.y)
        lineTo(v3.x, v3.y)
        lineTo(v7.x, v7.y)
        lineTo(v6.x, v6.y)
        close()
    }

    // Draw faces with slightly varying shading for depth!
    androidx.compose.ui.graphics.drawscope.DrawScope.Companion.apply {
        // Redraw paths with shading
        // In Canvas we are inside the DrawScope receiver, so we can draw paths directly:
    }
}

// Custom Draw Scope extensions to make Iso rendering clean
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIsoBox(
    cx: Float, cy: Float, cz: Float,
    sx: Float, sy: Float, sz: Float,
    color: Color,
    project: (Float, Float, Float) -> Offset
) {
    val hx = sx / 2f
    val hy = sy / 2f
    val hz = sz / 2f

    val v1 = project(cx - hx, cy - hy, cz - hz)
    val v2 = project(cx + hx, cy - hy, cz - hz)
    val v3 = project(cx + hx, cy + hy, cz - hz)
    val v4 = project(cx - hx, cy + hy, cz - hz)
    val v5 = project(cx - hx, cy - hy, cz + hz)
    val v6 = project(cx + hx, cy - hy, cz + hz)
    val v7 = project(cx + hx, cy + hy, cz + hz)
    val v8 = project(cx - hx, cy + hy, cz + hz)

    val topFace = Path().apply {
        moveTo(v5.x, v5.y)
        lineTo(v6.x, v6.y)
        lineTo(v7.x, v7.y)
        lineTo(v8.x, v8.y)
        close()
    }
    val frontFace = Path().apply {
        moveTo(v1.x, v1.y)
        lineTo(v2.x, v2.y)
        lineTo(v6.x, v6.y)
        lineTo(v5.x, v5.y)
        close()
    }
    val rightFace = Path().apply {
        moveTo(v2.x, v2.y)
        lineTo(v3.x, v3.y)
        lineTo(v7.x, v7.y)
        lineTo(v6.x, v6.y)
        close()
    }

    // Paint faces
    drawPath(topFace, color = color)
    drawPath(frontFace, color = color.darken(0.15f))
    drawPath(rightFace, color = color.darken(0.3f))

    // Draw wiring borders
    val strokeColor = Color.White.copy(alpha = 0.5f)
    drawLine(strokeColor, v5, v6, strokeWidth = 1f)
    drawLine(strokeColor, v6, v7, strokeWidth = 1f)
    drawLine(strokeColor, v7, v8, strokeWidth = 1f)
    drawLine(strokeColor, v8, v5, strokeWidth = 1f)
    drawLine(strokeColor, v1, v5, strokeWidth = 1f)
    drawLine(strokeColor, v2, v6, strokeWidth = 1f)
    drawLine(strokeColor, v3, v7, strokeWidth = 1f)
}

// Draw dashed blueprint outline for empty cells
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedIsoBoxOutline(
    cx: Float, cy: Float, cz: Float,
    sx: Float, sy: Float, sz: Float,
    color: Color,
    project: (Float, Float, Float) -> Offset
) {
    val hx = sx / 2f
    val hy = sy / 2f
    val hz = sz / 2f

    val v1 = project(cx - hx, cy - hy, cz - hz)
    val v2 = project(cx + hx, cy - hy, cz - hz)
    val v5 = project(cx - hx, cy - hy, cz + hz)
    val v6 = project(cx + hx, cy - hy, cz + hz)
    val v7 = project(cx + hx, cy + hy, cz + hz)
    val v8 = project(cx - hx, cy + hy, cz + hz)

    val stroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))

    val topFace = Path().apply {
        moveTo(v5.x, v5.y)
        lineTo(v6.x, v6.y)
        lineTo(v7.x, v7.y)
        lineTo(v8.x, v8.y)
        close()
    }

    drawPath(topFace, color = color, style = stroke)
    drawLine(color, v1, v2, pathEffect = stroke.pathEffect, strokeWidth = 1f)
    drawLine(color, v1, v5, pathEffect = stroke.pathEffect, strokeWidth = 1f)
    drawLine(color, v2, v6, pathEffect = stroke.pathEffect, strokeWidth = 1f)
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

// 4 Columns x 3 Levels individual slot cards
@Composable
fun ShelfSlotsGrid(
    shelfId: Int,
    products: List<ProductEntity>,
    onSlotClicked: (SlotCoordinates) -> Unit
) {
    val shelfProducts = products.filter { it.locationShelf == shelfId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.GridView, contentDescription = "Cabinet", tint = CyanNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Vue Cabinet Rayon $shelfId",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // We build a vertical Cabinet representation from Level 3 (top) down to Level 1 (bottom)
        for (level in listOf(3, 2, 1)) {
            Text(
                "Niveau $level (Étage ${if (level==3) "Supérieur" else if (level==2) "Milieu" else "Inférieur"})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textAlign = TextAlign.Start
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 1..4) {
                    val matchingProduct = shelfProducts.find { it.locationColumn == col && it.locationLevel == level }

                    val borderMod = if (matchingProduct != null) {
                        val accentClr = when {
                            matchingProduct.isOutOfStock() -> CrimsonRed
                            matchingProduct.isLowStock() -> AmberGlow
                            else -> EmeraldGlow
                        }
                        Modifier.border(1.5.dp, accentClr, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.border(1.dp, SlateCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (matchingProduct != null) SlateMedium.darken(0.1f) else SlateDark.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .then(borderMod)
                            .clickable { onSlotClicked(SlotCoordinates(shelfId, col, level)) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (matchingProduct != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        matchingProduct.category.equals("Électronique", true) -> Icons.Default.Computer
                                        matchingProduct.category.equals("Alimentaire", true) -> Icons.Default.Restaurant
                                        matchingProduct.category.equals("Vêtements", true) -> Icons.Default.Checkroom
                                        matchingProduct.category.equals("Cosmétiques", true) -> Icons.Default.Spa
                                        else -> Icons.Default.CardGiftcard
                                    },
                                    contentDescription = "Icon",
                                    tint = when {
                                        matchingProduct.isOutOfStock() -> CrimsonRed
                                        matchingProduct.isLowStock() -> AmberGlow
                                        else -> CyanNeon
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = matchingProduct.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Qté: ${matchingProduct.quantity}",
                                    fontSize = 10.sp,
                                    color = if (matchingProduct.isLowStock() || matchingProduct.isOutOfStock()) CrimsonRed else Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = "Ajouter", 
                                    tint = SlateCardBorder, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Col $col", 
                                    fontSize = 9.sp, 
                                    color = SlateCardBorder, 
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
