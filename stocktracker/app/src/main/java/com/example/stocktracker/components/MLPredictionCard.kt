package com.example.stocktracker.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stocktracker.domain.model.PredictionResult
import com.example.stocktracker.ui.theme.Green
import com.example.stocktracker.ui.theme.Red

@Composable
fun MLPredictionCard(
    prediction: PredictionResult?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSetAlert: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI Price Prediction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Source indicator
                    prediction?.let {
                        SourceIndicator(source = it.source, isOffline = it.isOffline)
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            when {
                isLoading -> LoadingContent()
                prediction == null -> EmptyContent(onRefresh)
                else -> PredictionContent(
                    prediction = prediction,
                    onSetAlert = onSetAlert
                )
            }
        }
    }
}

@Composable
private fun SourceIndicator(source: String, isOffline: Boolean) {
    val (icon, color, text) = when {
        isOffline || source.contains("on-device") -> Triple(
            Icons.Default.PhoneAndroid,
            Color(0xFFFF9800), // Orange
            "On-Device"
        )
        source.contains("fallback") -> Triple(
            Icons.Default.CloudOff,
            Color(0xFFFF5722), // Deep Orange
            "Fallback"
        )
        else -> Triple(
            Icons.Default.Cloud,
            MaterialTheme.colorScheme.primary,
            "Cloud"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Running ML models...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EmptyContent(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PsychologyAlt,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No prediction available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefresh) {
            Text("Generate Prediction")
        }
    }
}

@Composable
private fun PredictionContent(
    prediction: PredictionResult,
    onSetAlert: (Double) -> Unit
) {
    val trendColor = when (prediction.trend.lowercase()) {
        "bullish", "up" -> Green
        "bearish", "down" -> Red
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Trend Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Surface(
                color = trendColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (prediction.trend.lowercase()) {
                            "bullish", "up" -> Icons.Default.TrendingUp
                            "bearish", "down" -> Icons.Default.TrendingDown
                            else -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = trendColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prediction.trend.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = trendColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Confidence
            Column {
                Text(
                    text = "Confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                LinearProgressIndicator(
                    progress = { prediction.confidenceScores.firstOrNull()?.toFloat() ?: 0f },
                    modifier = Modifier.width(100.dp),
                    color = trendColor
                )
            }
        }

        // Price Change Prediction
        val changeColor = if (prediction.predictedChangePercent >= 0) Green else Red
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Predicted Change: ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${if (prediction.predictedChangePercent >= 0) "+" else ""}${String.format("%.2f", prediction.predictedChangePercent)}%",
                style = MaterialTheme.typography.bodyLarge,
                color = changeColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${String.format("%.2f", prediction.predictedHigh)} - ${String.format("%.2f", prediction.predictedLow)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // 7-Day Forecast
        if (prediction.predictions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                prediction.predictions.take(7).forEachIndexed { index, price ->
                    DayForecastItem(
                        day = "D${index + 1}",
                        price = price,
                        confidence = prediction.confidenceScores.getOrNull(index) ?: 0.5
                    )
                }
            }
        }

        // Model Info & Actions
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Model: ${prediction.modelVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "Updated: ${prediction.generatedAt.take(16)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Quick Action
            prediction.predictions.lastOrNull()?.let { targetPrice ->
                TextButton(onClick = { onSetAlert(targetPrice) }) {
                    Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Alert")
                }
            }
        }
    }
}

@Composable
private fun DayForecastItem(day: String, price: Double, confidence: Double) {
    val alpha = (confidence).coerceIn(0.3, 1.0).toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$${String.format("%.0f", price)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        // Confidence dot
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (confidence > 0.8) Green
                    else if (confidence > 0.6) MaterialTheme.colorScheme.primary
                    else Red.copy(alpha = alpha)
                )
        )
    }
}