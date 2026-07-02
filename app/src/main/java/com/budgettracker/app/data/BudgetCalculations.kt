package com.budgettracker.app.data

/**
 * Pure calculation functions - no Android imports. Exact formulas per spec:
 *
 *   Weekly is now a direct, manually-entered allotment (NOT derived from monthlySalary):
 *     weeklyPool = weeklyAllotment
 *
 *   Monthly remains salary + savings-percent driven, unchanged:
 *     monthlyIncome  = monthlySalary
 *     monthlySavings = monthlyIncome * (savingsGoalPercent / 100)
 *     monthlyPool    = monthlyIncome - monthlySavings
 *
 *   categoryAllocation = periodPool * (category.percent / 100)
 *   categoryRemaining  = categoryAllocation - category.spent
 *
 * "Left to allocate" footer = periodPool - sum(categoryAllocation for all categories).
 *
 * NOTE: the old weeklyIncome = monthlySalary * 12 / 52 / weeklySavings / derived-weeklyPool
 * formulas have been removed entirely - weekly no longer derives anything from monthlySalary.
 */
object BudgetCalculations {

    fun monthlyIncome(monthlySalary: Double): Double = monthlySalary

    fun monthlySavings(monthlySalary: Double, savingsGoalPercent: Float): Double =
        monthlyIncome(monthlySalary) * (savingsGoalPercent / 100.0)

    fun monthlyPool(monthlySalary: Double, savingsGoalPercent: Float): Double =
        monthlyIncome(monthlySalary) - monthlySavings(monthlySalary, savingsGoalPercent)

    /** Weekly pool is simply the user-entered weeklyAllotment directly - no derivation. */
    fun weeklyPool(weeklyAllotment: Double): Double = weeklyAllotment

    /** Pool for whichever period is active - convenience dispatcher. */
    fun periodPool(
        period: BudgetPeriod,
        monthlySalary: Double,
        savingsGoalPercent: Float,
        weeklyAllotment: Double
    ): Double =
        when (period) {
            BudgetPeriod.WEEKLY -> weeklyPool(weeklyAllotment)
            BudgetPeriod.MONTHLY -> monthlyPool(monthlySalary, savingsGoalPercent)
        }

    fun categoryAllocation(periodPool: Double, category: Category): Double =
        periodPool * (category.percent / 100.0)

    fun categoryRemaining(periodPool: Double, category: Category): Double =
        categoryAllocation(periodPool, category) - category.spent

    /** Sum of allocations across all categories in a period. */
    fun totalAllocated(periodPool: Double, categories: List<Category>): Double =
        categories.sumOf { categoryAllocation(periodPool, it) }

    /** Sum of percent across all categories - used for the "Allocated X%" header indicator. */
    fun totalAllocatedPercent(categories: List<Category>): Float =
        categories.sumOf { it.percent.toDouble() }.toFloat()

    /** Sum of amount spent so far across all categories in a period. */
    fun totalSpent(categories: List<Category>): Double =
        categories.sumOf { it.spent }

    /**
     * "Left to allocate" footer = periodPool - sum(categoryAllocation for all categories).
     * Positive => still room to allocate (green). Negative => over-allocated (red/warning).
     */
    fun leftToAllocate(periodPool: Double, categories: List<Category>): Double =
        periodPool - totalAllocated(periodPool, categories)

    /** Remaining spending pool for widget display: pool minus total spent across categories. */
    fun remainingPool(periodPool: Double, categories: List<Category>): Double =
        periodPool - totalSpent(categories)

    /**
     * Progress fraction (spent / allocation) clamped to [0,1] for progress-bar rendering.
     * Returns 0.0 if allocation is zero or negative (nothing to divide by / nothing allocated).
     */
    fun progressFraction(spent: Double, allocation: Double): Float {
        if (allocation <= 0.0) return 0f
        val raw = (spent / allocation).toFloat()
        return raw.coerceIn(0f, 1f)
    }

    /**
     * Overall period progress fraction (totalSpent / pool) clamped to [0,1], used by the widget's
     * slim progress bar.
     */
    fun overallProgressFraction(periodPool: Double, categories: List<Category>): Float =
        progressFraction(totalSpent(categories), periodPool)

    /** Basic input validation: reject negative salary by clamping to 0. */
    fun clampSalary(value: Double): Double = if (value < 0.0) 0.0 else value

    /** Basic input validation: reject negative weekly allotment by clamping to 0 (same rule as salary). */
    fun clampWeeklyAllotment(value: Double): Double = if (value < 0.0) 0.0 else value

    /** Basic input validation: clamp savings goal percent to [0, 60]. */
    fun clampSavingsPercent(value: Float): Float = value.coerceIn(0f, 60f)

    /** Basic input validation: clamp a category percent to [0, 100]. */
    fun clampCategoryPercent(value: Float): Float = value.coerceIn(0f, 100f)

    /** Basic input validation: reject negative spent amounts by clamping to 0. */
    fun clampSpent(value: Double): Double = if (value < 0.0) 0.0 else value

    /**
     * Top N categories by allocation (descending), used by the medium widget layout to show the
     * top 3 categories BY ALLOCATION.
     */
    fun topCategoriesByAllocation(periodPool: Double, categories: List<Category>, n: Int): List<Category> =
        categories.sortedByDescending { categoryAllocation(periodPool, it) }.take(n)
}
