package com.example.stocktracker.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.data.ml.MLRepository
import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mlRepository: MLRepository,
    private val stockRepository: com.example.stocktracker.domain.repository.StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = mlRepository.getMarketOverview()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            marketIndices = result.data?.indices ?: emptyList(),
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }

            loadChartData("SPY")
            loadTopMovers()
        }
    }

    private fun loadChartData(symbol: String) {
        viewModelScope.launch {
            _state.update { it.copy(isChartLoading = true) }

            when (val result = stockRepository.getChartData(symbol, "1M")) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            chartData = result.data ?: emptyList(),
                            isChartLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    val mockData = generateMockChartData()
                    _state.update {
                        it.copy(
                            chartData = mockData,
                            isChartLoading = false
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun generateMockChartData(): List<ChartPoint> {
        val data = mutableListOf<ChartPoint>()
        var price = 4200.0
        val now = System.currentTimeMillis()

        for (i in 30 downTo 0) {
            val change = (Math.random() - 0.48) * 50
            price += change
            data.add(
                ChartPoint(
                    timestamp = now - (i * 24 * 60 * 60 * 1000),
                    date = "${i}/02",
                    open = price - 10,
                    high = price + 15,
                    low = price - 15,
                    close = price,
                    volume = (1000000 + Math.random() * 500000).toLong()
                )
            )
        }
        return data
    }

    private fun loadTopMovers() {
        val mockGainers = listOf(
            TopMover("NVDA", "NVIDIA Corp", 875.30, 5.67, "45.2M"),
            TopMover("TSLA", "Tesla Inc", 175.40, 4.23, "32.1M"),
            TopMover("AMD", "AMD Inc", 178.90, 3.89, "28.5M"),
            TopMover("PLTR", "Palantir", 24.50, 3.45, "18.2M")
        )

        val mockLosers = listOf(
            TopMover("INTC", "Intel Corp", 42.30, -3.45, "25.3M"),
            TopMover("AAPL", "Apple Inc", 185.60, -2.12, "30.1M"),
            TopMover("MSFT", "Microsoft", 415.20, -1.89, "22.4M"),
            TopMover("GOOGL", "Alphabet", 142.30, -1.56, "15.8M")
        )

        _state.update {
            it.copy(
                topGainers = mockGainers,
                topLosers = mockLosers
            )
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val isChartLoading: Boolean = false,
    val marketIndices: List<MarketIndex> = emptyList(),
    val chartData: List<ChartPoint> = emptyList(),
    val topGainers: List<TopMover> = emptyList(),
    val topLosers: List<TopMover> = emptyList(),
    val portfolioValue: Double = 24562.80,
    val todayGain: Double = 245.60,
    val totalReturn: Double = 12.5,
    val error: String? = null
)

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