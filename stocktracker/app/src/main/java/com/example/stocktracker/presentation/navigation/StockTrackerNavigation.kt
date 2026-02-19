package com.example.stocktracker.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.stocktracker.presentation.screens.alerts.EnhancedAlertsScreen
import com.example.stocktracker.presentation.screens.dashboard.AdvancedDashboardScreen
import com.example.stocktracker.presentation.screens.home.EnhancedHomeScreen
import com.example.stocktracker.presentation.screens.news.EnhancedNewsScreen
import com.example.stocktracker.presentation.screens.search.SearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTrackerNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
                    BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard),
                    BottomNavItem("News", Screen.News.route, Icons.Default.Article),
                    BottomNavItem("Alerts", Screen.Alerts.route, Icons.Default.Notifications)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                EnhancedHomeScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    },
                    onSearchClick = { navController.navigate(Screen.Search.route) }
                )
            }

            composable(Screen.Dashboard.route) {
                AdvancedDashboardScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }

            composable(Screen.News.route) {
                EnhancedNewsScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }

            composable(Screen.Alerts.route) {
                EnhancedAlertsScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    },
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: return@composable
                // Your existing StockDetailScreen
                // StockDetailScreen(
                //     symbol = symbol,
                //     onBackClick = { navController.navigateUp() }
                // )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)