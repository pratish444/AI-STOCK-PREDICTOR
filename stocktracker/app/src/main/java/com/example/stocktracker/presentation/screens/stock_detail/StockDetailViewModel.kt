package com.example.stocktracker.presentation.screens.stock_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.data.ml.MLRepository
import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.domain.model.*
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val repository: StockRepository,
    private val mlRepository: MLRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val symbol: String = savedStateHandle.get<String>("symbol") ?: ""

    private val _state = MutableStateFlow(StockDetailState())
    val state: StateFlow<StockDetailState> = _state.asStateFlow()

    init {
        loadStockDetails()
        loadChartData("1M")
        loadMLPrediction()
    }

    private fun loadStockDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getStock(symbol)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            stock = result.data,
                            isLoading = false,
                            error = null
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
        }
    }

    fun loadChartData(interval: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingChart = true, selectedInterval = interval) }
            when (val result = repository.getChartData(symbol, interval)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            chartData = result.data ?: emptyList(),
                            isLoadingChart = false
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingChart = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val currentStock = _state.value.stock ?: return@launch
            if (currentStock.isInWatchlist) {
                repository.removeFromWatchlist(symbol)
            } else {
                repository.addToWatchlist(symbol)
            }
        }
    }

    fun refreshAll() {
        loadStockDetails()
        loadChartData(_state.value.selectedInterval)
        loadMLPrediction()
    }

    // ========== ML Functions ==========

    fun loadMLPrediction() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPrediction = true) }

            val chartData = _state.value.chartData
            if (chartData.size < 10) {
                _state.update {
                    it.copy(
                        isLoadingPrediction = false,
                        error = "Insufficient data for prediction (need 10+ days)"
                    )
                }
                return@launch
            }

            // Convert ChartPoint to PricePoint using DOUBLE
            val pricePoints = chartData.map { point ->
                PricePoint(
                    timestamp = point.timestamp,
                    open = point.open.toDouble(),
                    high = point.high.toDouble(),
                    low = point.low.toDouble(),
                    close = point.close.toDouble(),
                    volume = point.volume.toDouble()
                )
            }

            when (val result = mlRepository.getPrediction(symbol, pricePoints)) {
                is Resource.Success -> {
                    result.data?.let { prediction ->
                        _state.update {
                            it.copy(
                                prediction = prediction,
                                isLoadingPrediction = false,
                                error = null
                            )
                        }

                        if (prediction.source.contains("cloud")) {
                            loadSentimentAnalysis()
                        }
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingPrediction = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadSentimentAnalysis() {
        viewModelScope.launch {
            val mockNewsTexts = listOf(
                "$symbol reports strong quarterly earnings growth",
                "Analysts upgrade $symbol price target amid market optimism",
                "Market volatility affects $symbol trading volume"
            )

            when (val result = mlRepository.analyzeSentiment(mockNewsTexts, symbol)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(sentiment = result.data)
                    }
                }
                is Resource.Error -> {
                    // Don't show error for sentiment
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class StockDetailState(
    val stock: Stock? = null,
    val chartData: List<ChartPoint> = emptyList(),
    val selectedInterval: String = "1M",
    val isLoading: Boolean = false,
    val isLoadingChart: Boolean = false,
    val isLoadingPrediction: Boolean = false,
    val prediction: PredictionResult? = null,
    val sentiment: SentimentResult? = null,
    val error: String? = null
)