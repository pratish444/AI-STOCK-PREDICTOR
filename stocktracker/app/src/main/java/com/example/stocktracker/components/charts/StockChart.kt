package com.example.stocktracker.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.ui.theme.Green
import com.example.stocktracker.ui.theme.Red
import kotlin.math.roundToInt

@Composable
fun StockChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    showLabels: Boolean = true,
    animate: Boolean = true
) {
    if (data.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val isUpTrend = data.last().close >= data.first().close
    val lineColor = if (isUpTrend) Green else Red

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        } else {
            animationProgress.snapTo(1f)
        }
    }

    val minPrice = data.minOf { it.close }
    val maxPrice = data.maxOf { it.close }
    val priceRange = maxPrice - minPrice

    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    var selectedIndex by remember { mutableStateOf(-1) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = getIndexFromOffset(offset.x, size.width, data.size)
                            selectedIndex = index
                            selectedPoint = data.getOrNull(index)
                        },
                        onDrag = { change, _ ->
                            val index = getIndexFromOffset(change.position.x, size.width, data.size)
                            selectedIndex = index
                            selectedPoint = data.getOrNull(index)
                        },
                        onDragEnd = {
                            selectedPoint = null
                            selectedIndex = -1
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 40.dp.toPx()

            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            if (showGrid) {
                drawGrid(padding, chartWidth, chartHeight)
            }

            val points = data.mapIndexed { index, point ->
                val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
                val normalizedPrice = ((point.close - minPrice) / priceRange).toFloat()
                val y = padding + chartHeight - (normalizedPrice * chartHeight)
                ChartPointData(x, y, point)
            }

            val animatedPoints = points.mapIndexed { index, point ->
                val progress = (index.toFloat() / points.size) * animationProgress.value
                val clampedProgress = kotlin.math.min(progress * 1.5f, 1f)
                point.copy(
                    y = padding + chartHeight - ((point.y - padding) * clampedProgress)
                )
            }

            val fillPath = Path().apply {
                moveTo(animatedPoints.first().x, height - padding)

                animatedPoints.forEachIndexed { index, point ->
                    if (index == 0) {
                        lineTo(point.x, point.y)
                    } else {
                        val prev = animatedPoints[index - 1]
                        val midX = (prev.x + point.x) / 2
                        quadraticBezierTo(prev.x, prev.y, midX, (prev.y + point.y) / 2)
                        if (index == animatedPoints.size - 1) {
                            lineTo(point.x, point.y)
                        }
                    }
                }

                lineTo(animatedPoints.last().x, height - padding)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.4f),
                        lineColor.copy(alpha = 0.05f)
                    ),
                    startY = padding,
                    endY = height - padding
                )
            )

            val linePath = Path().apply {
                animatedPoints.forEachIndexed { index, point ->
                    if (index == 0) {
                        moveTo(point.x, point.y)
                    } else {
                        val prev = animatedPoints[index - 1]
                        val midX = (prev.x + point.x) / 2
                        quadraticBezierTo(prev.x, prev.y, midX, (prev.y + point.y) / 2)
                        if (index == animatedPoints.size - 1) {
                            lineTo(point.x, point.y)
                        }
                    }
                }
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            if (selectedIndex >= 0 && selectedIndex < animatedPoints.size) {
                val point = animatedPoints[selectedIndex]

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(point.x, padding),
                    end = Offset(point.x, height - padding),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )

                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(point.x, point.y)
                )
                drawCircle(
                    color = lineColor,
                    radius = 6f,
                    center = Offset(point.x, point.y)
                )
            }
        }

        selectedPoint?.let { point ->
            ChartTooltip(
                point = point,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (showLabels) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    String.format("%.2f", maxPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    String.format("%.2f", (maxPrice + minPrice) / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    String.format("%.2f", minPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(
    padding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.15f)

    for (i in 0..4) {
        val y = padding + (chartHeight / 4) * i
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1f
        )
    }

    for (i in 0..6) {
        val x = padding + (chartWidth / 6) * i
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = 1f
        )
    }
}

private fun getIndexFromOffset(x: Float, width: Int, dataSize: Int): Int {
    val padding = 40.dp.value
    val chartWidth = width - padding * 2
    val relativeX = (x - padding).coerceIn(0f, chartWidth)
    return ((relativeX / chartWidth) * (dataSize - 1)).roundToInt().coerceIn(0, dataSize - 1)
}

@Composable
private fun ChartTooltip(point: ChartPoint, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(min = 120.dp)
        ) {
            Text(
                point.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$${String.format("%.2f", point.close)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            androidx.compose.foundation.layout.Row {
                Text(
                    "O: ${String.format("%.2f", point.open)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "H: ${String.format("%.2f", point.high)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private data class ChartPointData(
    val x: Float,
    val y: Float,
    val data: ChartPoint
)

@Composable
fun SparklineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2f
) {
    if (data.size < 2) return

    val isPositive = data.last() >= data.first()
    val color = if (isPositive) Green else Red

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val min = data.minOrNull() ?: 0.0
        val max = data.maxOrNull() ?: 1.0
        val range = max - min

        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - ((value - min) / range).toFloat() * height
            Offset(x, y)
        }

        val path = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

