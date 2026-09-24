package com.example.expensetracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Repeat
import androidx.compose.material3.icons.filled.ShoppingCart
import androidx.compose.material3.icons.filled.Thumbtack
import androidx.compose.material3.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

data class MainUiState(
    val totalInPence: Long,
    val totalOutPence: Long,
    val status: BalanceStatus,
) {
    val balancePence: Long get() = totalInPence - totalOutPence

    val statusLabel: String
        get() = when (status) {
            BalanceStatus.GREEN -> "Healthy"
            BalanceStatus.AMBER -> "Balanced"
            BalanceStatus.RED -> "Over budget"
        }

    val statusColor: Color
        get() = when (status) {
            BalanceStatus.GREEN -> Color(0xFF2E7D32)
            BalanceStatus.AMBER -> Color(0xFFF57F17)
            BalanceStatus.RED -> Color(0xFFC62828)
        }
}

@Composable
fun MainScreen(
    state: MainUiState,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onManageCategories: () -> Unit,
    onRecurring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TotalsCard(state = state)

        Text(
            text = "What would you like to do?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ActionGrid(
            onAddExpense = onAddExpense,
            onAddIncome = onAddIncome,
            onManageCategories = onManageCategories,
            onRecurring = onRecurring,
        )
    }
}

@Composable
private fun TotalsCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Balance", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrafficDot(state.statusColor)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        state.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = state.statusColor,
                    )
                }
            }
            Text(
                formatPence(state.balancePence),
                style = MaterialTheme.typography.headlineLarge,
                color = state.statusColor,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "In",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatPence(state.totalInPence),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatPence(state.totalOutPence),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFC62828),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficDot(color: Color) {
    Card(
        shape = MaterialTheme.shapes.circle,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.size(12.dp),
    ) {}
}

private data class Action(
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ActionGrid(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onManageCategories: () -> Unit,
    onRecurring: () -> Unit,
) {
    val actions = listOf(
        Action("Add Expense", "Money out", Icons.Filled.ShoppingCart, onAddExpense),
        Action("Add Income", "Money in", Icons.Filled.TrendingUp, onAddIncome),
        Action("Categories", "Manage categories", Icons.Filled.Thumbtack, onManageCategories),
        Action("Recurring", "Auto-posting rules", Icons.Filled.Repeat, onRecurring),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.take(2).forEach { ActionButton(it, Modifier.weight(1f)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.drop(2).forEach { ActionButton(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ActionButton(action: Action, modifier: Modifier = Modifier) {
    Button(
        onClick = action.onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(action.icon, contentDescription = action.label)
            Text(
                action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(action.subtitle, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    ExpenseTrackerTheme {
        MainScreen(
            state = MainUiState(
                totalInPence = 250_000,
                totalOutPence = 25_500,
                status = BalanceStatus.GREEN,
            ),
            onAddExpense = {},
            onAddIncome = {},
            onManageCategories = {},
            onRecurring = {},
        )
    }
}