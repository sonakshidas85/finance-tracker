package com.budgettracker.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.app.data.BudgetCalculations
import com.budgettracker.app.data.BudgetPeriod
import com.budgettracker.app.data.BudgetState
import com.budgettracker.app.data.Category
import com.budgettracker.app.data.CurrencyFormat
import com.budgettracker.app.data.PeriodBudget
import com.budgettracker.app.data.PeriodStamps
import com.budgettracker.app.ui.components.CategoryRow
import com.budgettracker.app.ui.components.StatCard
import com.budgettracker.app.ui.theme.BudgetTheme
import com.budgettracker.app.ui.theme.MonospaceNumberStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: BudgetState,
    selectedPeriod: BudgetPeriod,
    onSelectPeriod: (BudgetPeriod) -> Unit,
    onSalaryChange: (Double) -> Unit,
    onSavingsPercentChange: (Float) -> Unit,
    onWeeklyAllotmentChange: (Double) -> Unit,
    onCategoryNameChange: (Category, String) -> Unit,
    onCategoryPercentChange: (Category, Float) -> Unit,
    onCategorySpentChange: (Category, Double) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddCategory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val periodBudget: PeriodBudget = when (selectedPeriod) {
        BudgetPeriod.WEEKLY -> state.weekly
        BudgetPeriod.MONTHLY -> state.monthly
    }

    val periodPool = BudgetCalculations.periodPool(
        selectedPeriod, state.monthlySalary, state.savingsGoalPercent, state.weeklyAllotment
    )
    val totalSpent = BudgetCalculations.totalSpent(periodBudget.categories)
    val totalAllocatedPercent = BudgetCalculations.totalAllocatedPercent(periodBudget.categories)
    val leftToAllocate = BudgetCalculations.leftToAllocate(periodPool, periodBudget.categories)
    val isOverAllocated = totalAllocatedPercent > 100f

    // Monthly-only derived values (income/spent/savings stat row).
    val monthlyIncome = BudgetCalculations.monthlyIncome(state.monthlySalary)
    val monthlySavings = BudgetCalculations.monthlySavings(state.monthlySalary, state.savingsGoalPercent)

    // Weekly-only derived value (saved = allotment - spent; emerald if >=0, coral if negative).
    val weeklySaved = state.weeklyAllotment - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Tracker") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedPeriod == BudgetPeriod.WEEKLY,
                        onClick = { onSelectPeriod(BudgetPeriod.WEEKLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Weekly") }
                    SegmentedButton(
                        selected = selectedPeriod == BudgetPeriod.MONTHLY,
                        onClick = { onSelectPeriod(BudgetPeriod.MONTHLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Monthly") }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedPeriod == BudgetPeriod.WEEKLY) {
                item {
                    WeeklyAllotmentCard(
                        weeklyAllotment = state.weeklyAllotment,
                        onWeeklyAllotmentChange = onWeeklyAllotmentChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    val isSavedPositive = weeklySaved >= 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(label = "Spent", value = CurrencyFormat.formatINR(totalSpent), modifier = Modifier.weight(1f))
                        StatCard(
                            label = "Saved",
                            value = CurrencyFormat.formatINR(weeklySaved),
                            valueColor = if (isSavedPositive) BudgetTheme.colors.positive else BudgetTheme.colors.warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                item {
                    SalaryAndSavingsCard(
                        monthlySalary = state.monthlySalary,
                        savingsGoalPercent = state.savingsGoalPercent,
                        onSalaryChange = onSalaryChange,
                        onSavingsPercentChange = onSavingsPercentChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(label = "Income", value = CurrencyFormat.formatINR(monthlyIncome), modifier = Modifier.weight(1f))
                        StatCard(label = "Spent", value = CurrencyFormat.formatINR(totalSpent), modifier = Modifier.weight(1f))
                        StatCard(label = "Savings", value = CurrencyFormat.formatINR(monthlySavings), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Allocated ${CurrencyFormat.formatPercent(totalAllocatedPercent)}",
                        style = MonospaceNumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isOverAllocated) BudgetTheme.colors.warning else BudgetTheme.colors.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(periodBudget.categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    periodPool = periodPool,
                    onNameChange = { onCategoryNameChange(category, it) },
                    onPercentChange = { onCategoryPercentChange(category, it) },
                    onSpentChange = { onCategorySpentChange(category, it) },
                    onDelete = { onDeleteCategory(category) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            item {
                OutlinedButton(onClick = onAddCategory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(0.dp))
                    Text("  Add category")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                val periodLabel = if (selectedPeriod == BudgetPeriod.WEEKLY) "this week" else "this month"
                FooterBanner(leftToAllocate = leftToAllocate, periodLabel = periodLabel)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SalaryAndSavingsCard(
    monthlySalary: Double,
    savingsGoalPercent: Float,
    onSalaryChange: (Double) -> Unit,
    onSavingsPercentChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly salary", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = if (monthlySalary == 0.0) "" else formatPlainAmount(monthlySalary),
                onValueChange = { text ->
                    val parsed = text.toDoubleOrNull()
                    if (text.isBlank()) {
                        onSalaryChange(0.0)
                    } else if (parsed != null) {
                        // Reject negative input at the input-handling layer (not just visually):
                        // clampSalary floors at 0, and the ViewModel/repository re-clamps again.
                        onSalaryChange(BudgetCalculations.clampSalary(parsed))
                    }
                },
                leadingIcon = { Text("₹", style = MonospaceNumberStyle) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Savings goal", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = CurrencyFormat.formatPercent(savingsGoalPercent),
                    style = MonospaceNumberStyle.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Slider(
                value = savingsGoalPercent,
                onValueChange = { onSavingsPercentChange(BudgetCalculations.clampSavingsPercent(it)) },
                valueRange = 0f..60f
            )
        }
    }
}

/**
 * Weekly tab's top input: a single editable "Weekly allotment" numeric field, replacing the old
 * salary input + savings slider on this tab. Same ₹-prefixed, negative-rejecting input-validation
 * style as [SalaryAndSavingsCard]'s salary field.
 */
@Composable
private fun WeeklyAllotmentCard(
    weeklyAllotment: Double,
    onWeeklyAllotmentChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weekly allotment", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = if (weeklyAllotment == 0.0) "" else formatPlainAmount(weeklyAllotment),
                onValueChange = { text ->
                    val parsed = text.toDoubleOrNull()
                    if (text.isBlank()) {
                        onWeeklyAllotmentChange(0.0)
                    } else if (parsed != null) {
                        // Reject negative input at the input-handling layer (not just visually):
                        // clampWeeklyAllotment floors at 0, and the ViewModel/repository re-clamps again.
                        onWeeklyAllotmentChange(BudgetCalculations.clampWeeklyAllotment(parsed))
                    }
                },
                leadingIcon = { Text("₹", style = MonospaceNumberStyle) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FooterBanner(leftToAllocate: Double, periodLabel: String) {
    val isPositive = leftToAllocate >= 0.0
    val bg = if (isPositive) BudgetTheme.colors.positiveTint else BudgetTheme.colors.warningTint
    val fg = if (isPositive) BudgetTheme.colors.positive else BudgetTheme.colors.warning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isPositive) "Left to allocate $periodLabel" else "Over-allocated $periodLabel",
                style = MaterialTheme.typography.labelLarge,
                color = fg
            )
            Text(
                text = CurrencyFormat.formatINR(leftToAllocate),
                style = MonospaceNumberStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = fg
            )
        }
    }
}

private fun formatPlainAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
