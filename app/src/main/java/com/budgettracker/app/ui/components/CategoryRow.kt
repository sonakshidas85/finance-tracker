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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * One editable category row: name field, percent input field (0-100, clamps), spent input
 * (rejects negative), remaining-amount label (emerald if >=0, coral/"over" if negative), thin
 * progress bar (spent/allocation, clamped 0-1 visually even if over).
 *
 * INPUT LAG FIX: every field here used to bind `value =` directly to the `category` object that
 * flows all the way from encrypted DataStore -> repository Flow -> ViewModel -> HomeScreen -> this
 * row. Every keystroke called the on-change callback, which wrote through to DataStore and waited
 * for the Flow to re-emit a brand new `Category` before the field would show the new text - a full
 * disk-write-and-decrypt round trip per character, which reads as "laggy" and occasionally drops
 * keystrokes if a new emission lands mid-keystroke. Each field below now keeps its own local
 * `remember(category.id) { mutableStateOf(...) }` string, so what's on screen updates instantly
 * and independently of how long the DataStore write takes; persistence still happens on every
 * change, it just no longer blocks what you see. Keying by `category.id` (rather than an
 * unkeyed `remember`) is what makes this safe: if this row's underlying category is ever replaced
 * wholesale (e.g. "Clear all data" reseeds fresh categories with new ids), the local text
 * correctly re-syncs from the new category instead of showing stale text forever.
 *
 * PERCENT FIELD: previously a `Slider` (every drag pixel fired the same disk round trip, which is
 * far worse than a text field since dragging emits many more events than typing does) - per the
 * user's request this is now a plain validated 0-100 numeric field instead, using the same local
 * state pattern.
 *
 * DELETE AFFORDANCE CHOICE: a plain trailing IconButton (X) rather than SwipeToDismissBox. This
 * keeps the row's gesture surface simple - see README "Assumptions & tradeoffs" for the full
 * rationale.
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

    // Local, instantly-editable copies - see the class doc for why these exist. Keyed by
    // category.id so a genuinely different/reseeded category correctly resets these, but our own
    // writes flowing back through DataStore don't clobber what the user is mid-typing.
    var nameText by remember(category.id) { mutableStateOf(category.name) }
    var percentText by remember(category.id) { mutableStateOf(formatPlainPercent(category.percent)) }
    var spentText by remember(category.id) { mutableStateOf(if (category.spent == 0.0) "" else formatPlainAmount(category.spent)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onNameChange(it)
                    },
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
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { text ->
                        percentText = text
                        val parsed = text.toFloatOrNull()
                        if (text.isBlank()) {
                            onPercentChange(0f)
                        } else if (parsed != null) {
                            // Reject out-of-range input at the input-handling layer, not just
                            // visually: clampCategoryPercent caps at 0-100.
                            onPercentChange(BudgetCalculations.clampCategoryPercent(parsed))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text("%", style = MonospaceNumberStyle) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = spentText,
                    onValueChange = { text ->
                        spentText = text
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

/** Formats a Float percent as a plain editable numeric string (e.g. 45f -> "45", 12.5f -> "12.5"). */
private fun formatPlainPercent(value: Float): String {
    return if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
}
