// Copy/adapted from app/src/main/java/com/budgettracker/app/data/BudgetModels.kt for standalone
// verification. Identical logic; only the @Serializable annotation is dropped (no kotlinx
// dependency available in this scratch harness) since serialization isn't exercised here.

data class Category(
    val id: String,
    val name: String,
    val percent: Float,
    val spent: Double
)

data class PeriodBudget(
    val categories: List<Category>,
    val periodStamp: String
)

data class BudgetState(
    val monthlySalary: Double,
    val savingsGoalPercent: Float,
    val weekly: PeriodBudget,
    val monthly: PeriodBudget,
    // Direct, manually-entered weekly spending pool (NOT derived from monthlySalary). Default
    // value mirrors the app's default-if-missing fallback for old serialized blobs.
    val weeklyAllotment: Double = 3000.0
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

object BudgetDefaults {
    const val DEFAULT_SALARY = 50000.0
    const val DEFAULT_SAVINGS_PERCENT = 20f
    const val DEFAULT_WEEKLY_ALLOTMENT = 3000.0

    // Weekly seed: Groceries 50%, Fun / discretionary 30%, Other 20%.
    fun seedWeeklyCategories(): List<Category> = listOf(
        Category(id = "id-w1", name = "Groceries", percent = 50f, spent = 0.0),
        Category(id = "id-w2", name = "Fun / discretionary", percent = 30f, spent = 0.0),
        Category(id = "id-w3", name = "Other", percent = 20f, spent = 0.0)
    )

    // Monthly seed: Rent 40%, Groceries 15%, Transport 8%, Fun / discretionary 10%, Other 5%,
    // Cat supplies 3%, Credit Card Payments 12%, Utility bill 7%.
    fun seedMonthlyCategories(): List<Category> = listOf(
        Category(id = "id-m1", name = "Rent", percent = 40f, spent = 0.0),
        Category(id = "id-m2", name = "Groceries", percent = 15f, spent = 0.0),
        Category(id = "id-m3", name = "Transport", percent = 8f, spent = 0.0),
        Category(id = "id-m4", name = "Fun / discretionary", percent = 10f, spent = 0.0),
        Category(id = "id-m5", name = "Other", percent = 5f, spent = 0.0),
        Category(id = "id-m6", name = "Cat supplies", percent = 3f, spent = 0.0),
        Category(id = "id-m7", name = "Credit Card Payments", percent = 12f, spent = 0.0),
        Category(id = "id-m8", name = "Utility bill", percent = 7f, spent = 0.0)
    )

    fun seedBudgetState(weeklyStamp: String, monthlyStamp: String): BudgetState = BudgetState(
        monthlySalary = DEFAULT_SALARY,
        savingsGoalPercent = DEFAULT_SAVINGS_PERCENT,
        weekly = PeriodBudget(categories = seedWeeklyCategories(), periodStamp = weeklyStamp),
        monthly = PeriodBudget(categories = seedMonthlyCategories(), periodStamp = monthlyStamp),
        weeklyAllotment = DEFAULT_WEEKLY_ALLOTMENT
    )
}
