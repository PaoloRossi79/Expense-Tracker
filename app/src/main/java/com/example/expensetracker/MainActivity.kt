package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.expensetracker.ui.common.PlaceholderScreen
import com.example.expensetracker.ui.home.MainScreen
import com.example.expensetracker.ui.home.MainViewModel
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                var destination by mutableStateOf(Destination.MAIN)
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    when (destination) {
                        Destination.MAIN -> MainScreen(
                            state = MainViewModel().uiState.value,
                            onAddExpense = { destination = Destination.ADD_EXPENSE },
                            onAddIncome = { destination = Destination.ADD_INCOME },
                            onManageCategories = { destination = Destination.CATEGORIES },
                            onRecurring = { destination = Destination.RECURRING },
                        )
                        Destination.ADD_EXPENSE -> PlaceholderScreen("Add Expense")
                        Destination.ADD_INCOME -> PlaceholderScreen("Add Income")
                        Destination.CATEGORIES -> PlaceholderScreen("Categories & Vendors")
                        Destination.RECURRING -> PlaceholderScreen("Recurring")
                    }
                }
            }
        }
    }
}

private enum class Destination {
    MAIN,
    ADD_EXPENSE,
    ADD_INCOME,
    CATEGORIES,
    RECURRING,
}