package com.example.stocktracker.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getWatchlistStocks()
                .catch { e ->
                    _state.update { it.copy(error = e.localizedMessage) }
                }
                .collect { stocks ->
                    _state.update {
                        it.copy(
                            watchlist = stocks.map { s ->
                                HomeStock(
                                    symbol = s.symbol,
                                    name = s.name,
                                    currentPrice = s.currentPrice,
                                    changePercent = s.changePercent,
                                    chartData = listOf(
                                        s.currentPrice * 0.95,
                                        s.currentPrice * 0.97,
                                        s.currentPrice * 0.96,
                                        s.currentPrice * 0.98,
                                        s.currentPrice
                                    )
                                )
                            },
                            isLoading = false
                        )
                    }
                }
        }

        _state.update {
            it.copy(
                topGainers = getMockGainers(),
                topLosers = getMockLosers()
            )
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    private fun getMockGainers(): List<HomeStock> {
        return listOf(
            HomeStock("NVDA", "NVIDIA", 875.30, 5.67, listOf(820.0, 835.0, 845.0, 860.0, 875.3)),
            HomeStock("TSLA", "Tesla", 175.40, 4.23, listOf(165.0, 168.0, 170.0, 172.0, 175.4)),
            HomeStock("AMD", "AMD", 178.90, 3.89, listOf(168.0, 170.0, 173.0, 175.0, 178.9))
        )
    }

    private fun getMockLosers(): List<HomeStock> {
        return listOf(
            HomeStock("INTC", "Intel", 42.30, -3.45, listOf(45.0, 44.5, 44.0, 43.2, 42.3)),
            HomeStock("AAPL", "Apple", 185.60, -2.12, listOf(192.0, 190.0, 188.0, 187.0, 185.6)),
            HomeStock("MSFT", "Microsoft", 415.20, -1.89, listOf(425.0, 422.0, 420.0, 418.0, 415.2))
        )
    }
}

data class HomeState(
    val watchlist: List<HomeStock> = emptyList(),
    val topGainers: List<HomeStock> = emptyList(),
    val topLosers: List<HomeStock> = emptyList(),
    val portfolioValue: Double = 24562.80,
    val dayChange: Double = 245.60,
    val dayChangePercent: Double = 1.02,
    val marketStatus: String = "Open",
    val nextMarketOpen: String = "Tomorrow 9:30 AM",
    val isLoading: Boolean = true,
    val error: String? = null
)