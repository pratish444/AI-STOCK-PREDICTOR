package com.example.stocktracker.presentation.screens.search

import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _state.update {
            it.copy(
                trendingStocks = getMockTrendingStocks(),
                popularCategories = getMockCategories(),
                recentSearches = getMockRecentSearches()
            )
        }

        viewModelScope.launch {
            repository.getWatchlistStocks().collect { stocks ->
                _state.update {
                    it.copy(watchlistSymbols = stocks.map { s -> s.symbol }.toSet())
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }

        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400)
                performSearch()
            }
        } else {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun performSearch() {
        val query = _state.value.searchQuery
        if (query.length < 2) return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }

            when (val result = repository.searchStocks(query)) {
                is Resource.Success -> {
                    addToRecentSearches(query)

                    _state.update {
                        it.copy(
                            searchResults = result.data ?: emptyList(),
                            isSearching = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isSearching = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun addToRecentSearches(query: String) {
        val current = _state.value.recentSearches.toMutableList()
        current.remove(query)
        current.add(0, query)
        _state.update {
            it.copy(recentSearches = current.take(5))
        }
    }

    fun clearSearch() {
        _state.update {
            it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
        }
    }

    fun clearRecentSearches() {
        _state.update { it.copy(recentSearches = emptyList()) }
    }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch {
            if (_state.value.watchlistSymbols.contains(symbol)) {
                repository.removeFromWatchlist(symbol)
            } else {
                repository.addToWatchlist(symbol)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun getMockTrendingStocks(): List<TrendingStock> {
        return listOf(
            TrendingStock("NVDA", "NVIDIA", 875.30, 5.67, 1, 245),
            TrendingStock("TSLA", "Tesla", 175.40, 4.23, 2, 189),
            TrendingStock("AMD", "AMD Inc", 178.90, 3.89, 3, 156),
            TrendingStock("AAPL", "Apple", 185.60, 2.12, 4, 134),
            TrendingStock("MSFT", "Microsoft", 415.20, 1.89, 5, 98)
        )
    }

    private fun getMockCategories(): List<SearchCategory> {
        return listOf(
            SearchCategory(
                "Technology",
                androidx.compose.material.icons.Icons.Default.Computer,
                142,
                androidx.compose.ui.graphics.Color(0xFF2196F3)
            ),
            SearchCategory(
                "Healthcare",
                androidx.compose.material.icons.Icons.Default.LocalHospital,
                89,
                androidx.compose.ui.graphics.Color(0xFF4CAF50)
            ),
            SearchCategory(
                "Finance",
                androidx.compose.material.icons.Icons.Default.AccountBalance,
                76,
                androidx.compose.ui.graphics.Color(0xFFFF9800)
            ),
            SearchCategory(
                "Energy",
                androidx.compose.material.icons.Icons.Default.Bolt,
                54,
                androidx.compose.ui.graphics.Color(0xFFFF5722)
            ),
            SearchCategory(
                "Consumer",
                androidx.compose.material.icons.Icons.Default.ShoppingCart,
                98,
                androidx.compose.ui.graphics.Color(0xFF9C27B0)
            ),
            SearchCategory(
                "Crypto",
                androidx.compose.material.icons.Icons.Default.CurrencyBitcoin,
                32,
                androidx.compose.ui.graphics.Color(0xFF795548)
            )
        )
    }

    private fun getMockRecentSearches(): List<String> {
        return listOf("AAPL", "TSLA", "NVDA", "MSFT")
    }
}

data class SearchState(
    val searchQuery: String = "",
    val searchResults: List<Stock> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val trendingStocks: List<TrendingStock> = emptyList(),
    val popularCategories: List<SearchCategory> = emptyList(),
    val watchlistSymbols: Set<String> = emptySet(),
    val error: String? = null
)