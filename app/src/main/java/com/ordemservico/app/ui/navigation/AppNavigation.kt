package com.ordemservico.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ordemservico.app.ui.screens.*
import com.ordemservico.app.ui.viewmodels.AuthViewModel
import com.ordemservico.app.ui.viewmodels.OrderViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateOrder : Screen("create_order")
    object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: String) = "edit_order/$orderId"
    }
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val orderViewModel: OrderViewModel = viewModel()

    val startDestination = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                orderViewModel = orderViewModel,
                onCreateOrder = { navController.navigate(Screen.CreateOrder.route) },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateOrder.route) {
            CreateEditOrderScreen(
                authViewModel = authViewModel,
                orderViewModel = orderViewModel,
                orderId = null,
                onSuccess = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId)) {
                        popUpTo(Screen.CreateOrder.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.EditOrder.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            CreateEditOrderScreen(
                authViewModel = authViewModel,
                orderViewModel = orderViewModel,
                orderId = orderId,
                onSuccess = {
                    navController.navigate(Screen.OrderDetail.createRoute(orderId)) {
                        popUpTo(Screen.EditOrder.createRoute(orderId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            OrderDetailScreen(
                orderId = orderId,
                authViewModel = authViewModel,
                orderViewModel = orderViewModel,
                onEdit = { navController.navigate(Screen.EditOrder.createRoute(orderId)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
