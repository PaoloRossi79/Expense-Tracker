@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paolorossi.expensetracker.data.auth.AuthUser
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.recurring.RecurringEditRequest
import com.paolorossi.expensetracker.ui.AppViewModelFactory
import com.paolorossi.expensetracker.ui.addtransaction.AddTransactionScreen
import com.paolorossi.expensetracker.ui.categories.CategoriesScreen
import com.paolorossi.expensetracker.ui.home.HomeScreen
import com.paolorossi.expensetracker.ui.members.AddMemberScreen
import com.paolorossi.expensetracker.ui.recurring.RecurringRulesScreen
import com.paolorossi.expensetracker.ui.transactions.TransactionsScreen
import com.paolorossi.expensetracker.ui.vendors.VendorsScreen
import java.time.LocalDate

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavigation(
    user: AuthUser,
    editRequest: RecurringEditRequest?,
    onEditConsumed: () -> Unit,
    onSignOut: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory = AppViewModelFactory,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')

    val bottomItems =
        listOf(
            BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
            BottomItem(Routes.ADD_EXPENSE, "Expense", Icons.Filled.ShoppingCart),
            BottomItem(Routes.ADD_INCOME, "Income", Icons.AutoMirrored.Filled.TrendingUp),
            BottomItem(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Filled.List),
            BottomItem(Routes.CATEGORIES, "Categories", Icons.Filled.Label),
        )
    val bottomRoutes =
        setOf(
            Routes.HOME,
            Routes.ADD_EXPENSE,
            Routes.ADD_INCOME,
            Routes.TRANSACTIONS,
            Routes.CATEGORIES,
        )
    val showBottomBar = currentRoute in bottomRoutes
    val selectedRoute = currentRoute

    fun navigateTo(route: String) {
        if (route == Routes.HOME) {
            if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(editRequest) {
        if (editRequest != null) {
            navController.navigate(
                Routes.confirmRecurring(editRequest.ruleId, editRequest.dueDate.toString()),
            )
            onEditConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = selectedRoute == item.route,
                            onClick = { navigateTo(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    user = user,
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                    onOpenVendors = { navController.navigate(Routes.VENDORS) },
                    onOpenMembers = { navController.navigate(Routes.MEMBERS) },
                    onSignOut = onSignOut,
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onEdit = { transaction ->
                        navController.navigate(Routes.edit(transaction.id, transaction.type))
                    },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(viewModel = viewModel(factory = viewModelFactory))
            }
            composable(Routes.VENDORS) {
                VendorsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(Routes.RECURRING) {
                RecurringRulesScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(Routes.MEMBERS) {
                AddMemberScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(
                route = Routes.ADD_EXPENSE_ROUTE,
                arguments =
                    listOf(
                        navArgument("transactionId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("ruleId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("dueDate") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
            ) { entry ->
                val type = TransactionType.EXPENSE
                val transactionId = entry.arguments?.getString("transactionId").orEmpty().ifBlank { null }
                val ruleId = entry.arguments?.getString("ruleId").orEmpty().ifBlank { null }
                val dueDate =
                    entry.arguments?.getString("dueDate").orEmpty().ifBlank { null }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                AddTransactionScreen(
                    type = type,
                    transactionId = transactionId,
                    recurringRuleId = ruleId,
                    dueDate = dueDate,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
            composable(
                route = Routes.ADD_INCOME_ROUTE,
                arguments =
                    listOf(
                        navArgument("transactionId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("ruleId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("dueDate") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
            ) { entry ->
                val type = TransactionType.INCOME
                val transactionId = entry.arguments?.getString("transactionId").orEmpty().ifBlank { null }
                val ruleId = entry.arguments?.getString("ruleId").orEmpty().ifBlank { null }
                val dueDate =
                    entry.arguments?.getString("dueDate").orEmpty().ifBlank { null }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                AddTransactionScreen(
                    type = type,
                    transactionId = transactionId,
                    recurringRuleId = ruleId,
                    dueDate = dueDate,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel(factory = viewModelFactory),
                )
            }
        }
    }
}
