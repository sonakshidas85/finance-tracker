package com.budgettracker.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Pure data model for the budget tracker. This file has NO Android imports so it can be
 * unit-tested (and copy/pasted into the standalone `verification/` harness) with plain
 * `kotlinc`/`kotlin` on the JVM.
 */

@Serializable
data class Category(
    val id: String,           // UUID
    val name: String,
    val percent: Float,       // 0-100, share of that period's spending pool
    val spent: Double         // amount spent so far this period, in rupees
)

@Serializable
data class PeriodBudget(
    val categories: List<Category>,
    val periodStamp: String   // e.g. "2026-W27" or "2026-M07" - used to detect rollover and reset `spent`
)

@Serializable
data class BudgetState(
    val monthlySalary: Double,
    val savingsGoalPercent: Float,   // 0-60
    val weekly: PeriodBudget,
    val monthly: PeriodBudget,
    // Direct, manually-entered weekly spending pool (NOT derived from monthlySalary - see
    // BudgetCalculations doc comment). Defaults to 3000.0 for callers deserializing an old
    // DataStore blob saved before this field existed - kotlinx.serialization would otherwise
    // require the field to be present; the default here means old JSON without this key still
    // decodes cleanly instead of crashing (simple default-if-missing fallback, no real migration
    // machinery needed for a single added field with a safe default).
    val weeklyAllotment: Double = 3000.0
)

/**
 * Which period tab/widget is currently selected. Kept here (pure) so it can be shared by the
 * ViewModel, MainActivity intent-extra handling, and widget tap-target code without pulling in
 * Android imports.
 */
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

/**
 * Default seed data used on first launch (no DataStore value yet) and by "Clear all data".
 * Weekly and monthly periods now start with DIFFERENT category splits (by percent), each with
 * spent = 0.0, per the spec:
 *   Weekly:  Groceries 50%, Fun / discretionary 30%, Other 20%
 *   Monthly: Rent 40%, Groceries 15%, Transport 8%, Fun / discretionary 10%, Other 5%,
 *            Cat supplies 3%, Credit Card Payments 12%, Utility bill 7%
 */
object BudgetDefaults {

    const val DEFAULT_SALARY = 50000.0
    const val DEFAULT_SAVINGS_PERCENT = 20f
    const val DEFAULT_WEEKLY_ALLOTMENT = 3000.0

    /**
     * Deterministic (non-random) ids are intentionally NOT used here - each call generates a
     * fresh UUID for every category so that repeated "clear all data" operations don't collide
     * with any previously-serialized rows. Callers (repository / seed logic) are expected to call
     * this once and persist the result.
     */
    fun seedWeeklyCategories(): List<Category> = listOf(
        Category(id = UUID.randomUUID().toString(), name = "Groceries", percent = 50f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Fun / discretionary", percent = 30f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Other", percent = 20f, spent = 0.0)
    )

    fun seedMonthlyCategories(): List<Category> = listOf(
        Category(id = UUID.randomUUID().toString(), name = "Rent", percent = 40f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Groceries", percent = 15f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Transport", percent = 8f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Fun / discretionary", percent = 10f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Other", percent = 5f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Cat supplies", percent = 3f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Credit Card Payments", percent = 12f, spent = 0.0),
        Category(id = UUID.randomUUID().toString(), name = "Utility bill", percent = 7f, spent = 0.0)
    )

    fun seedBudgetState(weeklyStamp: String, monthlyStamp: String): BudgetState = BudgetState(
        monthlySalary = DEFAULT_SALARY,
        savingsGoalPercent = DEFAULT_SAVINGS_PERCENT,
        weekly = PeriodBudget(categories = seedWeeklyCategories(), periodStamp = weeklyStamp),
        monthly = PeriodBudget(categories = seedMonthlyCategories(), periodStamp = monthlyStamp),
        weeklyAllotment = DEFAULT_WEEKLY_ALLOTMENT
    )
}
