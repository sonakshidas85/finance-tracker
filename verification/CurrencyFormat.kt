// Copy/adapted from app/src/main/java/com/budgettracker/app/data/CurrencyFormat.kt - identical
// logic (only java.* imports, no Android imports existed in the original).

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormat {

    private const val RUPEE = "₹" // ₹

    fun formatINR(amount: Double): String {
        // See app/src/main/java/.../CurrencyFormat.kt for why valueOf() is used instead of the
        // BigDecimal(Double) constructor (avoids binary-representation rounding surprises).
        val rounded = BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP)
        val isNegative = rounded.signum() < 0
        val absValue = rounded.abs().toBigInteger().toString()
        val grouped = groupIndian(absValue)
        return if (isNegative) "-$RUPEE$grouped" else "$RUPEE$grouped"
    }

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

    fun formatPercent(percent: Float): String {
        val rounded = Math.round(percent)
        return "$rounded%"
    }

    @Suppress("unused")
    private fun formatINR_NumberFormatReference(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
        return format.format(amount)
    }

    fun isNegative(amount: Double): Boolean = amount < 0.0

    fun abs(amount: Double): Double = abs(amount)
}
