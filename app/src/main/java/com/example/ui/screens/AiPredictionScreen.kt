package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StockViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPredictionScreen(
    viewModel: StockViewModel
) {
    val predictionResult by viewModel.aiPredictionResult.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

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
                        "Prédictions & Logistique IA", 
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
            // Promo Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                border = BorderStroke(1.dp, SlateCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyanNeon.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "IA", tint = CyanNeon, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Moteur Prédictif Gemini 3.5",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Analyse de flux financiers (FCFA), rotation et réapprovisionnements.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Results terminal container
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                border = BorderStroke(1.dp, SlateCardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    if (predictionResult.isEmpty() && !isAiLoading) {
                        // Empty / Initial state
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.TipsAndUpdates, 
                                contentDescription = "Indication", 
                                tint = CyanNeon.copy(alpha = 0.4f), 
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Moteur de Prédiction IA inactif",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Déclenchez l'analyse ci-dessous. Gemini collectera l'état réel de vos stocks localisés en FCFA et vos historiques d'entrées/sorties pour rédiger votre audit d'approvisionnement logistique.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 6.dp)
                            )
                        }
                    } else {
                        // Prediction results display
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    "RAPPORT LOGISTIQUE IA (STOCK3D)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanNeon,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            
                            item {
                                // Output formatted report text
                                Text(
                                    text = predictionResult,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (isAiLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SlateMedium.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyanNeon)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Consultation de l'IA en cours...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Modélisation de la chaîne logistique",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger action button
            Button(
                onClick = { viewModel.requestAiStockPrediction() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                enabled = !isAiLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, CyanNeon, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Générer", tint = SlateDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LANCER LA PRÉDICTION IA",
                        color = SlateDark,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
    }
}
