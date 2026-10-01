@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paolorossi.expensetracker.ui.common.ErrorText
import com.paolorossi.expensetracker.ui.common.ScreenScaffold

@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    viewModel: AddMemberViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Members", onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Household members", style = MaterialTheme.typography.titleMedium)
            val members = state.household?.memberEmails.orEmpty()
            if (members.isEmpty()) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                members.forEach { email ->
                    Text("• $email", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Add member by email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            state.message?.let { message ->
                Text(message, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = viewModel::add, modifier = Modifier.fillMaxWidth()) {
                Text("Add member")
            }
            Text(
                "Removing members isn't available yet (requirements §10).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
