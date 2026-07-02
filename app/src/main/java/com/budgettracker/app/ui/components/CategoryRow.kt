package com.budgettracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.app.data.BudgetCalculations
import com.budgettracker.app.data.Category
import com.budgettracker.app.data.CurrencyFormat
import com.budgettracker.app.ui.theme.BudgetTheme
import com.budgettracker.app.ui.theme.MonospaceNumberStyle

/**
 * One editable category row: name field, percent slider (0-100, clamps), spent input (rejects
 * negative), remaining-amount label (emerald if >=0, coral/"over" if negative), thin progress bar
 * (spent/allocation, clamped 0-1 visually even if over).
 *
 * DELETE AFFORDANCE CHOICE: a plain trailing IconButton (X) rather than SwipeToDismissBox. This
 * keeps the row's gesture surface simple (sliders and text fields already consume horizontal drag
 * gestures within the row, and swipe-to-dismiss over a slider is an awkward, easy-to-mis-trigger
 * combination) - see README "Assumptions & tradeoffs" for the full rationale.
 */
@Composable
fun CategoryRow(
    category: Category,
    periodPool: Double,
    onNameChange: (String) -> Unit,
    onPercentChange: (Float) -> Unit,
    onSpentChange: (Double) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allocation = BudgetCalculations.categoryAllocation(periodPool, category)
    val remaining = BudgetCalculations.categoryRemaining(periodPool, category)
    val progress = BudgetCalculations.progressFraction(category.spent, allocation)
    val isOver = remaining < 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete category",
                        tint = BudgetTheme.colors.textMuted
                    )
                }
            }

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    text = "Allocation",
                    style = MaterialTheme.typography.labelSmall,
                    color = BudgetTheme.colors.textMuted,
                    modifier = Modifier.width(72.dp)
                )
                Slider(
                    value = category.percent,
                    onValueChange = { onPercentChange(BudgetCalculations.clampCategoryPercent(it)) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CurrencyFormat.formatPercent(category.percent),
                    style = MonospaceNumberStyle.copy(fontSize = 13.sp),
                    modifier = Modifier.width(48.dp)
                )
            }

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = if (category.spent == 0.0) "" else formatPlainAmount(category.spent),
                    onValueChange = { text ->
                        val parsed = text.toDoubleOrNull()
                        if (text.isBlank()) {
                            onSpentChange(0.0)
                        } else if (parsed != null) {
                            onSpentChange(BudgetCalculations.clampSpent(parsed))
                        }
                        // Non-numeric input is simply ignored (rejected) - the field will not
                        // update, per "rejects negative input" / basic input validation at the
                        // input-handling layer, not just visually.
                    },
                    label = { Text("Spent") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = BudgetTheme.colors.textMuted
                    )
                    Text(
                        text = if (isOver) {
                            "Over by ${CurrencyFormat.formatINR(-remaining)}"
                        } else {
                            CurrencyFormat.formatINR(remaining)
                        },
                        style = MonospaceNumberStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isOver) BudgetTheme.colors.warning else BudgetTheme.colors.positive
                    )
                }
            }

            ThinProgressBar(
                fraction = progress,
                progressColor = if (isOver) BudgetTheme.colors.warning else BudgetTheme.colors.positive,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Formats a Double as a plain (no currency symbol/grouping) editable numeric string for text fields. */
private fun formatPlainAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
