package com.paolorossi.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paolorossi.expensetracker.data.recurring.RecurringEditRequest
import com.paolorossi.expensetracker.navigation.AppNavigation
import com.paolorossi.expensetracker.ui.AppViewModelFactory
import com.paolorossi.expensetracker.ui.auth.RootGate
import com.paolorossi.expensetracker.ui.auth.RootState
import com.paolorossi.expensetracker.ui.auth.RootViewModel
import com.paolorossi.expensetracker.ui.theme.ExpenseTrackerTheme

/**
 * Single-activity host. Extends [FragmentActivity] because `BiometricPrompt`
 * requires it (Design/03 auth flow).
 */
class MainActivity : FragmentActivity() {
    private val editRequest = mutableStateOf<RecurringEditRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        editRequest.value = RecurringEditRequest.from(intent)

        setContent {
            ExpenseTrackerTheme {
                val rootViewModel: RootViewModel = viewModel(factory = AppViewModelFactory)
                val state by rootViewModel.state.collectAsStateWithLifecycle()

                when (val current = state) {
                    is RootState.Ready ->
                        AppNavigation(
                            user = current.user,
                            editRequest = editRequest.value,
                            onEditConsumed = { editRequest.value = null },
                            onSignOut = rootViewModel::signOut,
                        )

                    else ->
                        RootGate(
                            state = current,
                            onSignIn = { rootViewModel.signIn(this@MainActivity) },
                            onUnlocked = rootViewModel::onUnlocked,
                            onSignOut = rootViewModel::signOut,
                            onRetry = rootViewModel::refreshMembership,
                        )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        editRequest.value = RecurringEditRequest.from(intent)
    }
}
