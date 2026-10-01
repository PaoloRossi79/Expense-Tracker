@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.domain.SortField
import com.paolorossi.expensetracker.ui.common.EmptyState
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold
import com.paolorossi.expensetracker.ui.common.TransactionRow
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsScreen(
    onEdit: (Transaction) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: TransactionsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categoryById = state.categories.associateBy { it.id }

    ScreenScaffold(title = "Transactions", onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::showPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                }
                Text(
                    state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = viewModel::showNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }

            FilterBar(state = state, viewModel = viewModel)
            HorizontalDivider()
            ErrorText(state.error, Modifier.padding(horizontal = 16.dp))

            if (state.transactions.isEmpty()) {
                EmptyState("No transactions for this month yet.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.transactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            category = categoryById[transaction.categoryId],
                            canManage = transaction.createdByUid == state.currentUserUid,
                            onEdit = { onEdit(transaction) },
                            onDelete = { viewModel.requestDelete(transaction) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    state.deleteCandidate?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete transaction?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FilterBar(
    state: TransactionsUiState,
    viewModel: TransactionsViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.filter.type == null,
                onClick = { viewModel.setTypeFilter(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = state.filter.type == TransactionType.EXPENSE,
                onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) },
                label = { Text("Expense") },
            )
            FilterChip(
                selected = state.filter.type == TransactionType.INCOME,
                onClick = { viewModel.setTypeFilter(TransactionType.INCOME) },
                label = { Text("Income") },
            )

            CategoryFilter(state, viewModel)

            FilterChip(
                selected = state.sort.field == SortField.DATE,
                onClick = { viewModel.toggleSortField(SortField.DATE) },
                label = { Text("Date") },
            )
            FilterChip(
                selected = state.sort.field == SortField.AMOUNT,
                onClick = { viewModel.toggleSortField(SortField.AMOUNT) },
                label = { Text("Amount") },
            )
        }
    }
}

@Composable
private fun CategoryFilter(
    state: TransactionsUiState,
    viewModel: TransactionsViewModel,
) {
    var open by remember { mutableStateOf(false) }
    val selectedName = state.categories.firstOrNull { it.id == state.filter.categoryId }?.name

    Box {
        FilterChip(
            selected = state.filter.categoryId != null,
            onClick = { open = true },
            label = { Text(selectedName ?: "Category") },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("All categories") },
                onClick = {
                    viewModel.setCategoryFilter(null)
                    open = false
                },
            )
            state.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        viewModel.setCategoryFilter(category.id)
                        open = false
                    },
                )
            }
        }
    }
}
