package com.budgettracker.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.budgettracker.app.ui.theme.BudgetTheme

/**
 * Settings screen, reachable from the settings icon in HomeScreen's top bar. Contains a
 * "Clear all data" button that shows a confirmation AlertDialog before wiping DataStore back to
 * first-run seeded defaults, plus the optional app-lock and widget-masking privacy toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearAllData: () -> Unit,
    appLockEnabled: Boolean = false,
    onAppLockEnabledChange: (Boolean) -> Unit = {},
    maskWidgetAmounts: Boolean = false,
    onMaskWidgetAmountsChange: (Boolean) -> Unit = {}
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // BiometricManager reports whether the device has a secure lock screen (PIN/pattern/password
    // or biometric enrolled) configured at all - without one, BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    // authentication can never succeed, so we block enabling the toggle and explain why instead.
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Require unlock to open app", style = MaterialTheme.typography.bodyLarge)
                    if (!canAuthenticate) {
                        Text(
                            text = "No screen lock (PIN, pattern, or biometric) is set up on this device, so app lock can't be enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BudgetTheme.colors.textMuted
                        )
                    }
                }
                Switch(
                    checked = appLockEnabled && canAuthenticate,
                    enabled = canAuthenticate,
                    onCheckedChange = onAppLockEnabledChange
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mask amounts on home-screen widgets", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Widgets are visible on the lock screen without unlocking the app - enable this to hide real amounts there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BudgetTheme.colors.textMuted
                    )
                }
                Switch(
                    checked = maskWidgetAmounts,
                    onCheckedChange = onMaskWidgetAmountsChange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Reset your salary, savings goal, and all categories (both weekly and monthly) back to the default seeded values. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = BudgetTheme.colors.textMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BudgetTheme.colors.warning)
            ) {
                Text("Clear all data")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Clear all data?") },
            text = { Text("This will erase your salary, savings goal, and every category for both weekly and monthly budgets, restoring the default seeded categories. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAllData()
                    showConfirmDialog = false
                }) {
                    Text("Clear", color = BudgetTheme.colors.warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
