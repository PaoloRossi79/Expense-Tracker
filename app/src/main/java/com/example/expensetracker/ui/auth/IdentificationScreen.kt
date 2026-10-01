@file:OptIn(ExperimentalMaterial3Api::class)

package com.paolorossi.expensetracker.ui.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.paolorossi.expensetracker.BuildConfig
import com.paolorossi.expensetracker.data.auth.BiometricGate
import com.paolorossi.expensetracker.ui.common.LoadingBox
import kotlinx.coroutines.launch

@Composable
fun RootGate(
    state: RootState,
    onSignIn: () -> Unit,
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        RootState.Loading -> LoadingBox()
        RootState.NotConfigured -> SetupRequiredScreen()
        is RootState.SignedOut -> SignInScreen(error = state.error, onSignIn = onSignIn)
        is RootState.NotAMember -> NotAMemberScreen(state.email, onSignOut, onRetry)
        RootState.Locked -> LockedScreen(onUnlocked = onUnlocked, onSignOut = onSignOut)
        is RootState.Ready -> LoadingBox()
    }
}

@Composable
private fun CenteredColumn(
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun SetupRequiredScreen() {
    CenteredColumn(
        title = "Setup required",
        body =
            "Firebase isn't configured yet. Complete Design/00-prerequisites.md, then add " +
                "app/google-services.json and set WEB_CLIENT_ID and HOUSEHOLD_ID in local.properties " +
                "(or gradle.properties).",
    ) {
        Text(
            "WEB_CLIENT_ID set: ${BuildConfig.WEB_CLIENT_ID.isNotBlank()}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "HOUSEHOLD_ID set: ${BuildConfig.HOUSEHOLD_ID.isNotBlank()}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SignInScreen(
    error: String?,
    onSignIn: () -> Unit,
) {
    CenteredColumn(
        title = "Welcome",
        body = "Sign in with the Google account on this phone to open your household ledger.",
    ) {
        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in with Google")
        }
        if (!error.isNullOrBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun NotAMemberScreen(
    email: String,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
) {
    CenteredColumn(
        title = "Not part of a household yet",
        body =
            "$email isn't on this household's member list. Ask a family member to add you " +
                "from the Add Member screen, or check the household document in Firestore.",
    ) {
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Retry")
        }
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}

@Composable
private fun LockedScreen(
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }

    fun attemptUnlock() {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            failed = true
            return
        }
        scope.launch {
            val ok = BiometricGate(activity).authenticate()
            if (ok) onUnlocked() else failed = true
        }
    }

    LaunchedEffect(Unit) { attemptUnlock() }

    CenteredColumn(
        title = "Locked",
        body = "Confirm it's you to continue. Your session stays signed in.",
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Button(onClick = { attemptUnlock() }, modifier = Modifier.fillMaxWidth()) {
            Text("Unlock")
        }
        if (failed) {
            Text(
                "Couldn't start the biometric prompt",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
