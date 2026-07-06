package com.budgettracker.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.budgettracker.app.data.BudgetPeriod
import com.budgettracker.app.data.BudgetRepository
import com.budgettracker.app.ui.MainViewModel
import com.budgettracker.app.ui.components.MascotMessage
import com.budgettracker.app.ui.screens.HomeScreen
import com.budgettracker.app.ui.screens.SettingsScreen
import com.budgettracker.app.ui.showBiometricGate
import com.budgettracker.app.ui.theme.BudgetTrackerTheme

/**
 * Single-activity host for the Compose UI. Reads the `EXTRA_SELECTED_PERIOD` intent extra (set by
 * both widgets' tap PendingIntents) to select the initial Weekly/Monthly tab.
 *
 * Extends `FragmentActivity` (not the plain `androidx.activity.ComponentActivity`) because
 * `BiometricPrompt`'s constructor requires a `FragmentActivity` host to attach its internal
 * confirmation fragment - `FragmentActivity` still supports Compose's `setContent` exactly like
 * `ComponentActivity` does, so this is a drop-in superclass swap with no behavior change.
 */
class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_SELECTED_PERIOD = "selected_period"
        const val PERIOD_WEEKLY = "WEEKLY"
        const val PERIOD_MONTHLY = "MONTHLY"
    }

    private val viewModel: MainViewModel by viewModels {
        val repository = BudgetRepository.getInstance(applicationContext)
        MainViewModel.Factory(repository, periodFromIntent(intent))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent this Activity's contents from appearing in screenshots, the recents-app
        // thumbnail, or screen recordings - budget/salary data shouldn't leak that way to anyone
        // who picks up or screen-shares an unlocked/borrowed phone.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            BudgetTrackerTheme {
                var showSettings by remember { mutableStateOf(false) }
                // isUnlocked starts false whenever app-lock is on, re-gating content on every
                // fresh process/Activity creation (e.g. after task-switch-and-back or process
                // death) - it is never persisted, by design.
                var isUnlocked by remember { mutableStateOf(false) }
                // Bumped to re-trigger the LaunchedEffect below when the user taps "Retry".
                var unlockAttempt by remember { mutableStateOf(0) }

                val state by viewModel.budgetState.collectAsState()
                val selectedPeriod by viewModel.selectedPeriod.collectAsState()
                val appLockEnabled by viewModel.appLockEnabled.collectAsState()
                val maskWidgetAmounts by viewModel.maskWidgetAmounts.collectAsState()

                if (appLockEnabled && !isUnlocked) {
                    LaunchedEffect(unlockAttempt) {
                        showBiometricGate(onSuccess = { isUnlocked = true })
                    }
                    LockGateScreen(onRetry = { unlockAttempt++ })
                } else if (showSettings) {
                    SettingsScreen(
                        onBack = { showSettings = false },
                        onClearAllData = { viewModel.clearAllData() },
                        appLockEnabled = appLockEnabled,
                        onAppLockEnabledChange = { viewModel.setAppLockEnabled(it) },
                        maskWidgetAmounts = maskWidgetAmounts,
                        onMaskWidgetAmountsChange = { viewModel.setMaskWidgetAmounts(it) }
                    )
                } else {
                    HomeScreen(
                        state = state,
                        selectedPeriod = selectedPeriod,
                        onSelectPeriod = { viewModel.selectPeriod(it) },
                        onSalaryChange = { viewModel.updateSalary(it) },
                        onSavingsPercentChange = { viewModel.updateSavingsGoalPercent(it) },
                        onWeeklyAllotmentChange = { viewModel.updateWeeklyAllotment(it) },
                        onCategoryNameChange = { category, name ->
                            viewModel.updateCategory(selectedPeriod, category.copy(name = name))
                        },
                        onCategoryPercentChange = { category, percent ->
                            viewModel.updateCategory(selectedPeriod, category.copy(percent = percent))
                        },
                        onCategorySpentChange = { category, spent ->
                            viewModel.updateCategory(selectedPeriod, category.copy(spent = spent))
                        },
                        onDeleteCategory = { category ->
                            viewModel.deleteCategory(selectedPeriod, category.id)
                        },
                        onAddCategory = { viewModel.addCategory(selectedPeriod) },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.selectPeriod(periodFromIntent(intent))
    }

    private fun periodFromIntent(intent: Intent?): BudgetPeriod {
        return when (intent?.getStringExtra(EXTRA_SELECTED_PERIOD)) {
            PERIOD_MONTHLY -> BudgetPeriod.MONTHLY
            else -> BudgetPeriod.WEEKLY
        }
    }
}

/**
 * Simple placeholder shown instead of budget content while app-lock is enabled and the user
 * hasn't successfully authenticated yet this Activity lifetime. Never renders any budget data.
 */
@Composable
private fun LockGateScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MascotMessage(
            headline = "Bean's guarding your wallet",
            body = "Prove it's you before we talk numbers."
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
