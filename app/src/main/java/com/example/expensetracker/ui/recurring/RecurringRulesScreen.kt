@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.recurring

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.domain.CategoryRules
import com.paolorossi.expensetracker.domain.Money
import com.paolorossi.expensetracker.ui.common.AmountField
import com.paolorossi.expensetracker.ui.common.DateField
import com.paolorossi.expensetracker.ui.common.EmptyState
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold
import java.time.LocalDate

@Composable
fun RecurringRulesScreen(
    onBack: () -> Unit,
    viewModel: RecurringRulesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    val categoryNames = state.categories.associate { it.id to it.name }

    ScreenScaffold(
        title = "Recurring rules",
        onBack = onBack,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            ErrorText(state.error, Modifier.padding(horizontal = 16.dp))
            if (state.rules.isEmpty()) {
                EmptyState("No recurring rules yet.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rules, key = { it.id }) { rule ->
                        val category = categoryNames[rule.categoryId] ?: "No category"
                        ListItem(
                            headlineContent = {
                                Text("${rule.reason} · ${Money.formatPence(rule.amountPence)}")
                            },
                            supportingContent = {
                                Text("Day ${rule.dayOfMonth} · $category · ${if (rule.active) "Active" else "Paused"}")
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = rule.active,
                                        onCheckedChange = { viewModel.setActive(rule, it) },
                                    )
                                    IconButton(onClick = { editing = rule }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { viewModel.delete(rule) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (creating) {
        RecurringRuleDialog(
            title = "New recurring rule",
            categories = state.categories,
            vendors = state.vendors,
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { rule ->
                viewModel.save(rule)
                creating = false
            },
        )
    }

    editing?.let { rule ->
        RecurringRuleDialog(
            title = "Edit recurring rule",
            categories = state.categories,
            vendors = state.vendors,
            initial = rule,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                viewModel.save(updated)
                editing = null
            },
        )
    }
}

@Composable
private fun RecurringRuleDialog(
    title: String,
    categories: List<Category>,
    vendors: List<Vendor>,
    initial: RecurringRule?,
    onDismiss: () -> Unit,
    onConfirm: (RecurringRule) -> Unit,
) {
    var type by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var reason by remember { mutableStateOf(initial?.reason.orEmpty()) }
    var amount by remember { mutableStateOf(initial?.amountPence?.let(Money::toPlainString).orEmpty()) }
    var dayOfMonth by remember { mutableStateOf((initial?.dayOfMonth ?: 1).toString()) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var vendorId by remember { mutableStateOf(initial?.vendorId) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var hasEndDate by remember { mutableStateOf(initial?.endDate != null) }
    var endDate by remember { mutableStateOf(initial?.endDate ?: LocalDate.now()) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var categoryMenu by remember { mutableStateOf(false) }
    var vendorMenu by remember { mutableStateOf(false) }

    val availableCategories = CategoryRules.categoriesFor(type, categories)
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "Choose category"
    val vendorName = vendors.firstOrNull { it.id == vendorId }?.name ?: "No vendor"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    singleLine = true,
                )

                AmountField(value = amount, onValueChange = { amount = it })

                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { dayOfMonth = it.filter(Char::isDigit).take(2) },
                    label = { Text("Day of month (1-31)") },
                    singleLine = true,
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(categoryName)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenu = false
                                },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vendorMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(vendorName)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = vendorMenu, onDismissRequest = { vendorMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("No vendor") },
                            onClick = {
                                vendorId = null
                                vendorMenu = false
                            },
                        )
                        vendors.forEach { vendor ->
                            DropdownMenuItem(
                                text = { Text(vendor.name) },
                                onClick = {
                                    vendorId = vendor.id
                                    vendorMenu = false
                                },
                            )
                        }
                    }
                }

                DateField(label = "Start date", date = startDate, onDateChange = { startDate = it })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                    Text("Has end date", modifier = Modifier.padding(start = 8.dp))
                }
                if (hasEndDate) {
                    DateField(label = "End date", date = endDate, onDateChange = { endDate = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Text("Active", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pence = Money.parsePoundsToPence(amount) ?: 0L
                    val day = dayOfMonth.toIntOrNull() ?: 0
                    onConfirm(
                        RecurringRule(
                            id = initial?.id.orEmpty(),
                            type = type,
                            reason = reason.trim(),
                            amountPence = pence,
                            categoryId = categoryId.orEmpty(),
                            vendorId = vendorId,
                            dayOfMonth = day,
                            startDate = startDate,
                            endDate = if (hasEndDate) endDate else null,
                            active = active,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
