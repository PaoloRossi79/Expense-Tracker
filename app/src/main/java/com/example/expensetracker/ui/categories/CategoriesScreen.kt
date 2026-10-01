@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.ui.common.EmptyState
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold

@Composable
fun CategoriesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CategoriesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }

    ScreenScaffold(
        title = "Categories",
        onBack = onBack,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
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
            if (state.categories.isEmpty()) {
                EmptyState("No categories yet. Add one to get started.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.categories, key = { it.id }) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            supportingContent = { Text(category.type.name.lowercase()) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editing = category }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { viewModel.delete(category) }) {
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
        CategoryDialog(
            title = "New category",
            initialName = "",
            initialType = CategoryType.EXPENSE,
            showType = true,
            onDismiss = { creating = false },
            onConfirm = { name, type ->
                viewModel.create(name, type)
                creating = false
            },
        )
    }

    editing?.let { category ->
        CategoryDialog(
            title = "Rename category",
            initialName = category.name,
            initialType = category.type,
            showType = false,
            onDismiss = { editing = null },
            onConfirm = { name, _ ->
                viewModel.rename(category, name)
                editing = null
            },
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialType: CategoryType,
    showType: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, CategoryType) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                if (showType) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == CategoryType.EXPENSE,
                            onClick = { type = CategoryType.EXPENSE },
                            label = { Text("Expense") },
                        )
                        FilterChip(
                            selected = type == CategoryType.INCOME,
                            onClick = { type = CategoryType.INCOME },
                            label = { Text("Income") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), type) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
