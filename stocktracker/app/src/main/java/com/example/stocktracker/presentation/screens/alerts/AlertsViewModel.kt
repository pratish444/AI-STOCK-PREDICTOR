package com.example.stocktracker.presentation.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.Alert
import com.example.stocktracker.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsState())
    val state: StateFlow<AlertsState> = _state.asStateFlow()

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            repository.getAllAlerts()
                .catch { e ->
                    _state.update { it.copy(error = e.localizedMessage, isLoading = false) }
                }
                .collectLatest { alerts ->
                    _state.update {
                        it.copy(
                            alerts = alerts,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun createAlert(symbol: String, targetPrice: Double, alertType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val stockName = symbol

            val alert = Alert(
                id = 0,
                symbol = symbol,
                stockName = stockName,
                alertType = alertType,
                targetPrice = targetPrice,
                isEnabled = true,
                createdAt = System.currentTimeMillis(),
                isTriggered = false
            )

            when (val result = repository.createAlert(alert)) {
                is com.example.stocktracker.domain.util.Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }
                is com.example.stocktracker.domain.util.Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to create alert"
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun toggleAlert(alertId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertStatus(alertId, isEnabled)
        }
    }

    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class AlertsState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)