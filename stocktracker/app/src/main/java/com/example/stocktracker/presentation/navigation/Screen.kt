package com.example.stocktracker.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object News : Screen("news")
    data object Alerts : Screen("alerts")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")

    data object StockDetail : Screen("stock_detail/{symbol}") {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
    }
}