package com.example.stocktracker.presentation.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.News
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewsState())
    val state: StateFlow<NewsState> = _state.asStateFlow()

    init {
        loadNews()
        observeNews()
    }

    private fun observeNews() {
        viewModelScope.launch {
            repository.getAllNews()
                .catch { e ->
                    _state.update { it.copy(error = e.localizedMessage, isLoading = false) }
                }
                .collectLatest { news ->
                    _state.update {
                        it.copy(
                            newsList = news,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    private fun loadNews() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.refreshNews()) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message ?: "Failed to load news"
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            when (val result = repository.refreshNews()) {
                is Resource.Success -> {
                    _state.update { it.copy(isRefreshing = false) }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = result.message ?: "Failed to refresh"
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class NewsState(
    val newsList: List<News> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)