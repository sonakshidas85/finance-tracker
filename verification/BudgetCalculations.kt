// Copy/adapted from app/src/main/java/com/budgettracker/app/data/BudgetCalculations.kt - byte
// for byte identical logic (no Android imports existed in the original, so nothing to strip).
//
// Weekly is now a direct, manually-entered allotment (NOT derived from monthlySalary); the old
// weeklyIncome/weeklySavings/derived-weeklyPool formulas have been removed entirely. Monthly
// remains salary + savings-percent driven, unchanged.

object BudgetCalculations {

    fun monthlyIncome(monthlySalary: Double): Double = monthlySalary

    fun monthlySavings(monthlySalary: Double, savingsGoalPercent: Float): Double =
        monthlyIncome(monthlySalary) * (savingsGoalPercent / 100.0)

    fun monthlyPool(monthlySalary: Double, savingsGoalPercent: Float): Double =
        monthlyIncome(monthlySalary) - monthlySavings(monthlySalary, savingsGoalPercent)

    /** Weekly pool is simply the user-entered weeklyAllotment directly - no derivation. */
    fun weeklyPool(weeklyAllotment: Double): Double = weeklyAllotment

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

    fun totalAllocated(periodPool: Double, categories: List<Category>): Double =
        categories.sumOf { categoryAllocation(periodPool, it) }

    fun totalAllocatedPercent(categories: List<Category>): Float =
        categories.sumOf { it.percent.toDouble() }.toFloat()

    fun totalSpent(categories: List<Category>): Double =
        categories.sumOf { it.spent }

    fun leftToAllocate(periodPool: Double, categories: List<Category>): Double =
        periodPool - totalAllocated(periodPool, categories)

    fun remainingPool(periodPool: Double, categories: List<Category>): Double =
        periodPool - totalSpent(categories)

    fun progressFraction(spent: Double, allocation: Double): Float {
        if (allocation <= 0.0) return 0f
        val raw = (spent / allocation).toFloat()
        return raw.coerceIn(0f, 1f)
    }

    fun overallProgressFraction(periodPool: Double, categories: List<Category>): Float =
        progressFraction(totalSpent(categories), periodPool)

    fun clampSalary(value: Double): Double = if (value < 0.0) 0.0 else value

    fun clampWeeklyAllotment(value: Double): Double = if (value < 0.0) 0.0 else value

    fun clampSavingsPercent(value: Float): Float = value.coerceIn(0f, 60f)

    fun clampCategoryPercent(value: Float): Float = value.coerceIn(0f, 100f)

    fun clampSpent(value: Double): Double = if (value < 0.0) 0.0 else value

    fun topCategoriesByAllocation(periodPool: Double, categories: List<Category>, n: Int): List<Category> =
        categories.sortedByDescending { categoryAllocation(periodPool, it) }.take(n)
}
