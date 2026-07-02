package com.budgettracker.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.app.data.BudgetCalculations
import com.budgettracker.app.data.BudgetRepository
import com.budgettracker.app.data.BudgetState
import com.budgettracker.app.data.Category
import com.budgettracker.app.ui.showBiometricGate
import com.budgettracker.app.ui.theme.BudgetTheme
import com.budgettracker.app.ui.theme.BudgetTrackerTheme
import com.budgettracker.app.ui.theme.MonospaceNumberStyle
import kotlinx.coroutines.launch

/**
 * Small floating popup Activity launched from the WEEKLY widget only (both small and medium
 * layouts - see widget/WidgetContent.kt), used for the "Add spend" entry point: an exact typed
 * amount against any of the CURRENT weekly period's categories.
 *
 * Uses `Theme.BudgetTracker.Dialog` (see themes.xml / AndroidManifest.xml) so it renders as a
 * small centered floating card over the home screen instead of a full-screen app launch -
 * RemoteViews/Glance widgets can't host a real text field, so this standalone popup Activity is
 * how the widget offers a typed-amount entry point at all.
 *
 * SECURITY: gated behind the same BiometricPrompt check as MainActivity when app-lock is enabled
 * (via the shared [showBiometricGate] helper) - otherwise this popup would be a silent bypass
 * around the app lock the user turned on. On failed/cancelled auth we just finish() immediately;
 * the widget itself remains on the home screen and the user can tap it again.
 */
class QuickAddSpendActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = BudgetRepository.getInstance(applicationContext)

        setContent {
            BudgetTrackerTheme {
                // null = appLockEnabled not loaded yet (render nothing rather than flash the
                // popup content before we know whether a gate is required).
                var appLockEnabled by remember { mutableStateOf<Boolean?>(null) }
                var isGateResolved by remember { mutableStateOf(false) }
                var isUnlocked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    repository.appLockEnabled.collect { appLockEnabled = it }
                }

                when {
                    appLockEnabled == null -> Unit

                    appLockEnabled == false -> {
                        QuickAddSpendPopup(
                            repository = repository,
                            onDismiss = { finish() }
                        )
                    }

                    !isGateResolved -> {
                        LaunchedEffect(Unit) {
                            showBiometricGate(
                                onSuccess = {
                                    isUnlocked = true
                                    isGateResolved = true
                                },
                                onFailure = {
                                    // Don't show a stuck screen for this small popup - the widget
                                    // remains, the user can tap it again.
                                    finish()
                                }
                            )
                        }
                    }

                    isUnlocked -> {
                        QuickAddSpendPopup(
                            repository = repository,
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSpendPopup(
    repository: BudgetRepository,
    onDismiss: () -> Unit
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as QuickAddSpendActivity
    var state by remember { mutableStateOf<BudgetState?>(null) }
    LaunchedEffect(Unit) {
        // Read live from the repository (not a stale snapshot) - the CURRENT weekly period's
        // categories, same source of truth MainViewModel/both widgets use.
        repository.budgetState.collect { state = it }
    }
    val categories = state?.weekly?.categories.orEmpty()
    var selectedCategory by remember(categories) { mutableStateOf<Category?>(categories.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add a spend", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.bodySmall,
                    color = BudgetTheme.colors.textMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCategory?.name ?: "No categories")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Amount", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        // Reject negative input at the input-handling layer, same style as
                        // HomeScreen/SettingsScreen numeric fields - the repository re-clamps too.
                        val parsed = text.toDoubleOrNull()
                        amountText = if (text.isBlank() || (parsed != null && parsed >= 0.0)) text else amountText
                    },
                    leadingIcon = { Text("₹", style = MonospaceNumberStyle) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val category = selectedCategory
                    val parsedAmount = amountText.toDoubleOrNull()
                    val canAdd = category != null && parsedAmount != null && parsedAmount >= 0.0
                    TextButton(
                        enabled = canAdd,
                        onClick = {
                            if (category != null && parsedAmount != null) {
                                val clamped = BudgetCalculations.clampSpent(parsedAmount)
                                activity.lifecycleScope.launch {
                                    // Shared "apply a weekly spend" flow (BudgetRepository.
                                    // applyWeeklySpend), followed by the same widget-refresh
                                    // mechanism BudgetApplication's own DataStore observer uses -
                                    // no second update path invented.
                                    repository.applyWeeklySpend(category.id, clamped)
                                    BudgetApplication.refreshWeeklyWidget(activity.applicationContext)
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text("Add", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
