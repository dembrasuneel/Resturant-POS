package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.billing.BillingScreen
import com.example.ui.home.HomeScreen
import com.example.ui.menu.MenuManagementScreen
import com.example.ui.openorders.OpenOrdersScreen
import com.example.ui.order.OrderScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.viewmodel.PosViewModel

object PosDestinations {
    const val HOME = "home"
    const val ORDER = "order/{orderId}"
    const val BILLING = "billing/{orderId}"
    const val OPEN_ORDERS = "open_orders"
    const val REPORTS = "reports"
    const val MENU = "menu"
    const val SETTINGS = "settings"

    fun orderRoute(orderId: Long) = "order/$orderId"
    fun billingRoute(orderId: Long) = "billing/$orderId"
}

@Composable
fun PosNavigation(viewModel: PosViewModel) {
    val navController = rememberNavController()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    NavHost(
        navController = navController,
        startDestination = PosDestinations.HOME
    ) {
        composable(PosDestinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateToOrder = { orderId ->
                    navController.navigate(PosDestinations.orderRoute(orderId))
                },
                onNavigateToOpenOrders = {
                    navController.navigate(PosDestinations.OPEN_ORDERS)
                },
                onNavigateToReports = {
                    navController.navigate(PosDestinations.REPORTS)
                },
                onNavigateToMenu = {
                    navController.navigate(PosDestinations.MENU)
                },
                onNavigateToSettings = {
                    navController.navigate(PosDestinations.SETTINGS)
                }
            )
        }

        composable(
            route = PosDestinations.ORDER,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            OrderScreen(
                orderId = orderId,
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBilling = { id ->
                    navController.navigate(PosDestinations.billingRoute(id))
                }
            )
        }

        composable(
            route = PosDestinations.BILLING,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            BillingScreen(
                orderId = orderId,
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPaymentCompleted = {
                    // Navigate back to Home and clear backstack to order
                    navController.navigate(PosDestinations.HOME) {
                        popUpTo(PosDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(PosDestinations.OPEN_ORDERS) {
            OpenOrdersScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onSelectOrder = { orderId ->
                    navController.navigate(PosDestinations.orderRoute(orderId))
                },
                onNavigateToBilling = { orderId ->
                    navController.navigate(PosDestinations.billingRoute(orderId))
                }
            )
        }

        composable(PosDestinations.REPORTS) {
            ReportsScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosDestinations.MENU) {
            MenuManagementScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PosDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
