package com.example.stocktracker.presentation.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.ui.theme.Green
import com.example.stocktracker.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDashboardScreen(
    onStockClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTimeRange by remember { mutableStateOf("1D") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Market Dashboard")
                        Text(
                            "Real-time market insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Market Indices
            item {
                Text(
                    "Market Indices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoading && state.marketIndices.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.marketIndices) { index ->
                            MarketIndexCard(index = index)
                        }
                    }
                }
            }

            // Portfolio Summary
            item {
                PortfolioSummaryCard(
                    portfolioValue = state.portfolioValue,
                    todayGain = state.todayGain,
                    totalReturn = state.totalReturn
                )
            }

            // Time Range Selector
            item {
                TimeRangeSelector(
                    selected = selectedTimeRange,
                    onSelect = { selectedTimeRange = it }
                )
            }

            // MARKET CHART - FIXED
            item {
                MarketChartCard(
                    data = state.chartData,
                    isLoading = state.isChartLoading,
                    timeRange = selectedTimeRange
                )
            }

            // Top Gainers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Top Gainers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    TextButton(onClick = { }) {
                        Text("See All")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.topGainers) { stock ->
                        MoverCard(
                            stock = stock,
                            isGainer = true,
                            onClick = { onStockClick(stock.symbol) }
                        )
                    }
                }
            }

            // Top Losers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Top Losers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Red
                    )
                    TextButton(onClick = { }) {
                        Text("See All")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.topLosers) { stock ->
                        MoverCard(
                            stock = stock,
                            isGainer = false,
                            onClick = { onStockClick(stock.symbol) }
                        )
                    }
                }
            }

            // Sector Performance
            item {
                Text(
                    "Sector Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SectorPerformanceCard()
            }

            // AI Insights
            item {
                AIInsightsCard()
            }

            // Error display
            state.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketIndexCard(index: MarketIndex) {
    val isPositive = index.changePercent >= 0
    val color = if (isPositive) Green else Red

    Card(
        modifier = Modifier.width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        // REMOVED: Custom background color that was causing distortion
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                index.symbol,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                String.format("%,.2f", index.value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward
                    else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${if (isPositive) "+" else ""}${String.format("%.2f", index.changePercent)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(
    portfolioValue: Double,
    todayGain: Double,
    totalReturn: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Portfolio Value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$${String.format("%,.2f", portfolioValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PortfolioStat(
                        "Today's Gain",
                        "+$${String.format("%.2f", todayGain)}",
                        Green
                    )
                    PortfolioStat(
                        "Total Return",
                        "+${String.format("%.1f", totalReturn)}%",
                        Green
                    )
                    PortfolioStat("Stocks", "12", Color.White)
                }
            }
        }
    }
}

@Composable
private fun PortfolioStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun TimeRangeSelector(selected: String, onSelect: (String) -> Unit) {
    val ranges = listOf("1D", "1W", "1M", "3M", "1Y", "ALL")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ranges.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// FIXED: Market Chart Card with proper implementation
@Composable
private fun MarketChartCard(
    data: List<com.example.stocktracker.data.remote.dto.ChartPoint>,
    isLoading: Boolean,
    timeRange: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                data.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Market Trend Chart",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "No data available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                else -> {
                    // REAL CHART IMPLEMENTATION
                    MarketTrendChart(data = data)
                }
            }
        }
    }
}

// FIXED: Proper Canvas Chart Implementation
@Composable
private fun MarketTrendChart(data: List<ChartPoint>) {
    val isPositive = data.last().close >= data.first().close
    val lineColor = if (isPositive) Green else Red

    // Calculate min/max for proper scaling
    val minPrice = data.minOf { it.close }
    val maxPrice = data.maxOf { it.close }
    val priceRange = maxPrice - minPrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Market Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", data.last().close)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
                Text(
                    "${if (isPositive) "+" else ""}${String.format("%.2f", ((data.last().close - data.first().close) / data.first().close * 100))}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = lineColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart Canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            for (i in 0..4) {
                val y = (height / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Create smooth path
            if (data.size > 1) {
                val points = data.mapIndexed { index, point ->
                    val x = (index.toFloat() / (data.size - 1)) * width
                    val normalizedPrice = if (priceRange > 0) {
                        ((point.close - minPrice) / priceRange).toFloat()
                    } else 0.5f
                    val y = height - (normalizedPrice * height)
                    Offset(x, y)
                }

                // Draw gradient fill
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        quadraticBezierTo(prev.x, prev.y, midX, (prev.y + curr.y) / 2)
                    }

                    lineTo(points.last().x, points.last().y)
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            lineColor.copy(alpha = 0.05f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        quadraticBezierTo(prev.x, prev.y, midX, (prev.y + curr.y) / 2)
                    }
                }

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Draw points
                points.forEachIndexed { index, point ->
                    if (index % maxOf(1, data.size / 8) == 0 || index == data.size - 1) {
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = point
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4f,
                            center = point,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // Date labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                data.first().date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                data.last().date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MoverCard(stock: TopMover, isGainer: Boolean, onClick: () -> Unit) {
    val color = if (isGainer) Green else Red

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        // REMOVED: Custom background that was causing color distortion
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stock.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stock.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$${String.format("%.2f", stock.price)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "${if (isGainer) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SectorPerformanceCard() {
    val sectors = listOf(
        "Technology" to 2.34,
        "Healthcare" to -0.56,
        "Finance" to 1.23,
        "Energy" to -1.45,
        "Consumer" to 0.78
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            sectors.forEachIndexed { index, (name, change) ->
                SectorBar(name = name, change = change)
                if (index < sectors.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SectorBar(name: String, change: Double) {
    val isPositive = change >= 0
    val color = if (isPositive) Green else Red
    val barWidth = (kotlin.math.abs(change) / 3.0).coerceIn(0.0, 1.0).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${if (isPositive) "+" else ""}${String.format("%.2f", change)}%",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AIInsightsCard() {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI Market Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    InsightItem(
                        icon = Icons.Default.TrendingUp,
                        title = "Bullish Trend Detected",
                        description = "Tech sector showing strong momentum"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InsightItem(
                        icon = Icons.Default.Warning,
                        title = "Volatility Alert",
                        description = "Market volatility increased by 15%"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InsightItem(
                        icon = Icons.Default.Lightbulb,
                        title = "Recommendation",
                        description = "Consider diversifying into healthcare"
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Data classes
data class MarketIndex(
    val name: String,
    val symbol: String,
    val value: Double,
    val change: Double,
    val changePercent: Double
)

data class TopMover(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val volume: String
)

data class ChartPoint(
    val timestamp: Long,
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)