@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.vendors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
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
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.ui.common.EmptyState
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold

@Composable
fun VendorsScreen(
    onBack: () -> Unit,
    viewModel: VendorsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Vendor?>(null) }
    val categoryNames = state.categories.associate { it.id to it.name }

    ScreenScaffold(
        title = "Vendors",
        onBack = onBack,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add vendor")
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
            if (state.vendors.isEmpty()) {
                EmptyState("No vendors yet. Add one to auto-fill categories.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.vendors, key = { it.id }) { vendor ->
                        ListItem(
                            headlineContent = { Text(vendor.name) },
                            supportingContent = {
                                val category = vendor.defaultCategoryId?.let { categoryNames[it] }
                                Text(
                                    listOfNotNull(category, vendor.matchAliases.joinToString().ifBlank { null })
                                        .joinToString(" · ")
                                        .ifBlank { "No default category" },
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editing = vendor }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { viewModel.delete(vendor) }) {
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
        VendorDialog(
            title = "New vendor",
            categories = state.categories,
            initial = Vendor(name = ""),
            onDismiss = { creating = false },
            onConfirm = { name, categoryId, aliases ->
                viewModel.create(name, categoryId, aliases)
                creating = false
            },
        )
    }

    editing?.let { vendor ->
        VendorDialog(
            title = "Edit vendor",
            categories = state.categories,
            initial = vendor,
            onDismiss = { editing = null },
            onConfirm = { name, categoryId, aliases ->
                viewModel.update(vendor, name, categoryId, aliases)
                editing = null
            },
        )
    }
}

@Composable
private fun VendorDialog(
    title: String,
    categories: List<Category>,
    initial: Vendor,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var aliases by remember { mutableStateOf(initial.matchAliases.joinToString(", ")) }
    var categoryId by remember { mutableStateOf(initial.defaultCategoryId) }
    var menuOpen by remember { mutableStateOf(false) }
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "None"

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
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Default category: $categoryName")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                categoryId = null
                                menuOpen = false
                            },
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text("Match aliases (comma separated)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), categoryId, aliases) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
