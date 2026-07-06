package com.budgettracker.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * INR currency formatting. Pure - no Android imports.
 *
 * DESIGN CHOICE: we implement a MANUAL Indian-digit-grouping formatter (last 3 digits, then
 * groups of 2, e.g. 1234567 -> "12,34,567") instead of relying on
 * `NumberFormat.getCurrencyInstance(Locale("en","IN"))`, because ICU/CLDR data for en-IN grouping
 * is inconsistent across Android API levels 26+ (some OEM/API combinations fall back to western
 * 3-digit grouping or format the rupee symbol differently). The manual formatter below is
 * deterministic on every API level since it does zero locale-dependent digit grouping.
 *
 * ROUNDING CHOICE: amounts are rounded to the nearest whole rupee for display (HALF_UP), not shown
 * with paise/decimals. Budgets in this app operate at a whole-rupee granularity in every input
 * field (salary, spent), so showing ".00" everywhere added visual noise without adding precision.
 * This is documented here and should not be re-litigated per the task's own instructions.
 *
 * NEGATIVE CHOICE: negative amounts are displayed as "-₹1,234" - the minus sign precedes the
 * rupee symbol, not between the symbol and the digits. This matches how Android's own currency
 * formatters render negative currency and reads naturally ("negative twelve hundred rupees").
 */
object CurrencyFormat {

    private const val RUPEE = "₹" // ₹

    /**
     * The function used app-wide. Manual Indian digit-grouping, rounded to nearest rupee.
     *   formatINR(1234567.0)  -> "₹12,34,567"
     *   formatINR(1000.0)     -> "₹1,000"
     *   formatINR(0.0)        -> "₹0"
     *   formatINR(-500.0)     -> "-₹500"
     */
    fun formatINR(amount: Double): String {
        // BigDecimal.valueOf(Double) (NOT the BigDecimal(Double) constructor) is used
        // deliberately: the constructor captures the exact, often non-terminating binary
        // representation of the double (e.g. 2.675 is actually ~2.67499999999999982...), which
        // can silently round the "wrong" way relative to the decimal literal a user typed.
        // valueOf() goes through Double.toString() first, matching decimal-literal expectations.
        val rounded = BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP)
        val isNegative = rounded.signum() < 0
        val absValue = rounded.abs().toBigInteger().toString()
        val grouped = groupIndian(absValue)
        return if (isNegative) "-$RUPEE$grouped" else "$RUPEE$grouped"
    }

    /**
     * Groups a non-negative, plain (no sign, no decimal point) digit string using the Indian
     * numbering system: the last 3 digits form one group, then every 2 digits moving left form
     * subsequent groups.
     *   "1234567" -> "12,34,567"
     *   "1000"    -> "1,000"
     *   "0"       -> "0"
     *   "100"     -> "100"
     *   "99999999"-> "9,99,99,999"
     */
    fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.substring(digits.length - 3)
        var remaining = digits.substring(0, digits.length - 3)
        val groups = mutableListOf<String>()
        while (remaining.length > 2) {
            groups.add(0, remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            groups.add(0, remaining)
        }
        return (groups + lastThree).joinToString(",")
    }

    /**
     * Formats a bare percent value like "45%" (whole number, no decimals) - percents in this app
     * are always in [0,100] (or [0,60] for savings goal) and are typically edited via sliders that
     * already produce whole numbers, so no decimal precision is needed for display.
     */
    fun formatPercent(percent: Float): String {
        val rounded = Math.round(percent)
        return "$rounded%"
    }

    /**
     * Reference-only alternative kept for comparison per the spec ("You may keep a
     * NumberFormat-based version behind a flag/comment for reference"). NOT used app-wide because
     * of inconsistent ICU grouping behavior for en-IN across API levels. Left here purely for
     * documentation / potential future diagnostics.
     */
    @Suppress("unused")
    private fun formatINR_NumberFormatReference(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
        return format.format(amount)
    }

    /** True if [amount] is effectively negative (helper for UI styling decisions). */
    fun isNegative(amount: Double): Boolean = amount < 0.0

    /** Absolute-value helper, occasionally handy alongside [isNegative] for UI composition. */
    fun abs(amount: Double): Double = abs(amount)
}
