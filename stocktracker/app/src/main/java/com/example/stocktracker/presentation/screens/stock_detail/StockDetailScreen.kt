package com.example.stocktracker.presentation.screens.stock_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stocktracker.components.MLPredictionCard
import com.example.stocktracker.components.SentimentDetailCard
import com.example.stocktracker.ui.theme.Green
import com.example.stocktracker.ui.theme.Red
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    onBackClick: () -> Unit,
    onSetAlert: (String, Double) -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (state.stock?.isInWatchlist == true)
                                Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (state.stock?.isInWatchlist == true)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.stock == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.stock?.let { stock ->
                            // Price Header
                            StockInfoHeader(stock = stock)

                            HorizontalDivider()

                            // Chart Section
                            ChartSection(
                                chartData = state.chartData,
                                selectedInterval = state.selectedInterval,
                                onIntervalChange = { viewModel.loadChartData(it) },
                                isLoading = state.isLoadingChart
                            )

                            HorizontalDivider()

                            // ML Prediction Card (NEW)
                            MLPredictionCard(
                                prediction = state.prediction,
                                isLoading = state.isLoadingPrediction,
                                onRefresh = { viewModel.loadMLPrediction() },
                                onSetAlert = { targetPrice ->
                                    onSetAlert(symbol, targetPrice)
                                }
                            )

                            HorizontalDivider()

                            // Sentiment Analysis (NEW)
                            state.sentiment?.let { sentiment ->
                                SentimentDetailCard(sentiment = sentiment)
                                HorizontalDivider()
                            }

                            // Statistics
                            StockStatistics(stock = stock)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockInfoHeader(stock: com.example.stocktracker.domain.model.Stock) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) Green else Red

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stock.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$${String.format("%.2f", stock.currentPrice)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = changeColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changeAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "(${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ChartSection(
    chartData: List<com.example.stocktracker.data.remote.dto.ChartPoint>,
    selectedInterval: String,
    onIntervalChange: (String) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Price Chart",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Interval Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val intervals = listOf("1D", "1W", "1M", "3M", "1Y", "5Y")
            intervals.forEach { interval ->
                FilterChip(
                    selected = selectedInterval == interval,
                    onClick = { onIntervalChange(interval) },
                    label = { Text(interval) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            chartData.isNotEmpty() -> {
                val chartEntryModel = remember(chartData) {
                    entryModelOf(*chartData.mapIndexed { index, point ->
                        index.toFloat() to point.close.toFloat()
                    }.toTypedArray())
                }

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(),
                        model = chartEntryModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chart data available")
                }
            }
        }
    }
}

@Composable
fun StockStatistics(stock: com.example.stocktracker.domain.model.Stock) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatisticRow("Open", "$${String.format("%.2f", stock.openPrice)}")
        StatisticRow("Previous Close", "$${String.format("%.2f", stock.previousClose)}")
        StatisticRow("Day High", "$${String.format("%.2f", stock.dayHigh)}")
        StatisticRow("Day Low", "$${String.format("%.2f", stock.dayLow)}")
        StatisticRow("Volume", String.format("%,d", stock.volume))
    }
}

@Composable
fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}