package com.budgettracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.app.ui.theme.BudgetTheme
import com.budgettracker.app.ui.theme.MonospaceNumberStyle

/**
 * One of the stat cards (e.g. Income / Spent / Savings on Monthly, Spent / Saved on Weekly) shown
 * in a row on the home screen. Rounded ~14dp corners, subtle 1dp elevation, hairline border, no
 * gradients - per the visual design spec. `valueColor` defaults to the normal on-surface text
 * color; callers pass the emerald/coral convention colors (e.g. for the Weekly "Saved" card).
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BudgetTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = BudgetTheme.colors.textMuted
            )
            Text(
                text = value,
                style = MonospaceNumberStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = valueColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
