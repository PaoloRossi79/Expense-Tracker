@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.ui.common.AmountField
import com.paolorossi.expensetracker.ui.common.CategoryPicker
import com.paolorossi.expensetracker.ui.common.DateField
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold
import java.time.LocalDate

@Composable
fun AddTransactionScreen(
    type: TransactionType,
    transactionId: String?,
    recurringRuleId: String?,
    dueDate: LocalDate?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel,
) {
    LaunchedEffect(type, transactionId, recurringRuleId, dueDate) {
        viewModel.load(type, transactionId, recurringRuleId, dueDate)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    val title =
        when {
            state.isRecurringConfirm -> "Confirm recurring"
            state.isEditing -> "Edit transaction"
            state.type == TransactionType.EXPENSE -> "Add expense"
            else -> "Add money in"
        }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AmountField(value = state.amount, onValueChange = viewModel::onAmountChange)

            OutlinedTextField(
                value = state.vendor,
                onValueChange = viewModel::onVendorChange,
                label = { Text(if (state.type == TransactionType.EXPENSE) "Vendor" else "Source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(label = "Date", date = state.date, onDateChange = viewModel::onDateChange)

            CategoryPicker(
                label = "Category",
                categories = state.availableCategories,
                selectedId = state.categoryId,
                onSelect = viewModel::onCategorySelect,
                onCreate = viewModel::createCategory,
            )

            ErrorText(state.error)

            Button(
                onClick = viewModel::save,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isRecurringConfirm) "Confirm" else "Save")
            }
        }
    }
}
