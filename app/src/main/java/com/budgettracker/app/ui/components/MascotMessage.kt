package com.budgettracker.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgettracker.app.R
import com.budgettracker.app.ui.theme.BudgetTheme

/**
 * Reusable mascot illustration + message, used for both empty states (e.g. "no categories yet")
 * and error states (e.g. the app-lock "unlock required" screen) per the user's request - same
 * mascot art (`ic_mascot.xml`) everywhere, only the headline/body copy and optional action button
 * change per call site. Keeping one shared composable (rather than separate empty/error
 * components) means any future mascot art update only has to happen in one drawable.
 */
@Composable
fun MascotMessage(
    headline: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_mascot),
            contentDescription = null,
            modifier = Modifier.height(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = BudgetTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}
