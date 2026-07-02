import java.time.LocalDate

/**
 * Standalone verification harness for the pure logic in data/BudgetCalculations.kt,
 * data/PeriodStamps.kt, and data/CurrencyFormat.kt. Not part of the Android module - compiled and
 * run directly with kotlinc/kotlin on the JVM. Every case is a hand-computed expected value;
 * failures print full details and cause a non-zero exit code.
 */

private var failures = 0
private var checks = 0

fun check(label: String, actual: Any?, expected: Any?) {
    checks++
    if (actual != expected) {
        failures++
        println("FAIL: $label\n  expected = $expected\n  actual   = $actual")
    } else {
        println("PASS: $label -> $actual")
    }
}

fun checkClose(label: String, actual: Double, expected: Double, epsilon: Double = 1e-6) {
    checks++
    if (Math.abs(actual - expected) > epsilon) {
        failures++
        println("FAIL: $label\n  expected ~= $expected\n  actual   = $actual")
    } else {
        println("PASS: $label -> $actual")
    }
}

fun main() {
    println("=== CurrencyFormat.formatINR ===")
    check("formatINR(1234567.0)", CurrencyFormat.formatINR(1234567.0), "₹12,34,567")
    check("formatINR(1000.0)", CurrencyFormat.formatINR(1000.0), "₹1,000")
    check("formatINR(0.0)", CurrencyFormat.formatINR(0.0), "₹0")
    check("formatINR(-500.0)", CurrencyFormat.formatINR(-500.0), "-₹500")
    check("formatINR(100.0) (<=3 digits, no grouping)", CurrencyFormat.formatINR(100.0), "₹100")
    check("formatINR(99999999.0)", CurrencyFormat.formatINR(99999999.0), "₹9,99,99,999")
    check("formatINR(1234.49) rounds down to nearest rupee", CurrencyFormat.formatINR(1234.49), "₹1,234")
    check("formatINR(1234.5) rounds up (HALF_UP)", CurrencyFormat.formatINR(1234.5), "₹1,235")
    check("formatINR(-1234.5) rounds away from zero on abs then negates", CurrencyFormat.formatINR(-1234.5), "-₹1,235")
    check("formatINR(12.0)", CurrencyFormat.formatINR(12.0), "₹12")

    println("\n=== CurrencyFormat.formatPercent ===")
    check("formatPercent(45f)", CurrencyFormat.formatPercent(45f), "45%")
    check("formatPercent(0f)", CurrencyFormat.formatPercent(0f), "0%")
    check("formatPercent(100f)", CurrencyFormat.formatPercent(100f), "100%")

    println("\n=== BudgetCalculations: income/savings/pool ===")
    // salary 50000, savings 20% (defaults) - monthly math unchanged (salary + savings-percent driven)
    checkClose("monthlyIncome(50000)", BudgetCalculations.monthlyIncome(50000.0), 50000.0)
    checkClose("monthlySavings(50000, 20)", BudgetCalculations.monthlySavings(50000.0, 20f), 10000.0)
    checkClose("monthlyPool(50000, 20)", BudgetCalculations.monthlyPool(50000.0, 20f), 40000.0)

    // weekly pool is now the direct manual allotment - no derivation from salary at all.
    checkClose("weeklyPool(3000) == weeklyAllotment directly", BudgetCalculations.weeklyPool(3000.0), 3000.0)
    checkClose("weeklyPool(0) == 0", BudgetCalculations.weeklyPool(0.0), 0.0)
    checkClose(
        "periodPool(WEEKLY, ...) ignores salary/savings, uses weeklyAllotment",
        BudgetCalculations.periodPool(BudgetPeriod.WEEKLY, monthlySalary = 999999.0, savingsGoalPercent = 55f, weeklyAllotment = 3000.0),
        3000.0
    )

    // salary 0
    checkClose("monthlyPool(0, 0)", BudgetCalculations.monthlyPool(0.0, 0f), 0.0)

    // salary 120000, savings 0%
    checkClose("monthlyPool(120000, 0) == monthlyIncome", BudgetCalculations.monthlyPool(120000.0, 0f), 120000.0)

    // salary 100000, savings 60% (max allowed)
    checkClose("monthlySavings(100000, 60)", BudgetCalculations.monthlySavings(100000.0, 60f), 60000.0)
    checkClose("monthlyPool(100000, 60)", BudgetCalculations.monthlyPool(100000.0, 60f), 40000.0)

    println("\n=== BudgetCalculations: category allocation/remaining ===")
    val monthlyPool40000 = BudgetCalculations.monthlyPool(50000.0, 20f) // 40000.0
    val rent = Category(id = "r", name = "Rent", percent = 45f, spent = 15000.0)
    checkClose("categoryAllocation(40000, 45%)", BudgetCalculations.categoryAllocation(monthlyPool40000, rent), 18000.0)
    checkClose("categoryRemaining(rent, spent 15000)", BudgetCalculations.categoryRemaining(monthlyPool40000, rent), 3000.0)

    val overspentGroceries = Category(id = "g", name = "Groceries", percent = 20f, spent = 10000.0)
    checkClose("categoryAllocation(40000, 20%)", BudgetCalculations.categoryAllocation(monthlyPool40000, overspentGroceries), 8000.0)
    checkClose("categoryRemaining over-spent -> negative", BudgetCalculations.categoryRemaining(monthlyPool40000, overspentGroceries), -2000.0)

    println("\n=== BudgetCalculations: NEW weekly seed (Groceries 50 / Fun 30 / Other 20) against weeklyAllotment 3000 ===")
    val weeklyCategories = BudgetDefaults.seedWeeklyCategories() // 50,30,20 = 100%
    check("weekly seed has 3 categories", weeklyCategories.size, 3)
    check("weekly seed totalAllocatedPercent == 100", BudgetCalculations.totalAllocatedPercent(weeklyCategories), 100f)
    val weeklyPool3000 = BudgetCalculations.weeklyPool(3000.0)
    checkClose(
        "weekly Groceries (50%) allocation == 1500",
        BudgetCalculations.categoryAllocation(weeklyPool3000, weeklyCategories[0]),
        1500.0
    )
    checkClose(
        "weekly Fun/discretionary (30%) allocation == 900",
        BudgetCalculations.categoryAllocation(weeklyPool3000, weeklyCategories[1]),
        900.0
    )
    checkClose(
        "weekly Other (20%) allocation == 600",
        BudgetCalculations.categoryAllocation(weeklyPool3000, weeklyCategories[2]),
        600.0
    )
    val weeklySpentCategories = listOf(
        weeklyCategories[0].copy(spent = 1200.0), // Groceries, allocation 1500, remaining 300
        weeklyCategories[1].copy(spent = 900.0),  // Fun, allocation 900, remaining 0
        weeklyCategories[2].copy(spent = 700.0)   // Other, allocation 600, remaining -100 (over)
    )
    checkClose(
        "weekly Groceries remaining (1500 - 1200) == 300",
        BudgetCalculations.categoryRemaining(weeklyPool3000, weeklySpentCategories[0]),
        300.0
    )
    checkClose(
        "weekly Other remaining over-spent (600 - 700) == -100",
        BudgetCalculations.categoryRemaining(weeklyPool3000, weeklySpentCategories[2]),
        -100.0
    )
    checkClose(
        "totalAllocated(weeklyPool3000, weekly seed) == pool (100% allocated)",
        BudgetCalculations.totalAllocated(weeklyPool3000, weeklyCategories),
        3000.0
    )

    println("\n=== BudgetCalculations: weekly Saved = allotment - spent (incl. over-spent negative case) ===")
    val weeklyTotalSpentNormal = BudgetCalculations.totalSpent(weeklySpentCategories) // 1200+900+700 = 2800
    checkClose("weekly totalSpent == 2800", weeklyTotalSpentNormal, 2800.0)
    val weeklySavedNormal = 3000.0 - weeklyTotalSpentNormal
    checkClose("weekly Saved (3000 - 2800) == 200, non-negative", weeklySavedNormal, 200.0)
    check("weekly Saved >= 0 -> emerald case", weeklySavedNormal >= 0.0, true)

    val weeklyOverspentCategories = listOf(
        weeklyCategories[0].copy(spent = 2000.0),
        weeklyCategories[1].copy(spent = 1200.0),
        weeklyCategories[2].copy(spent = 500.0)
    )
    val weeklyTotalSpentOver = BudgetCalculations.totalSpent(weeklyOverspentCategories) // 2000+1200+500 = 3700
    checkClose("weekly totalSpent (over-spend case) == 3700", weeklyTotalSpentOver, 3700.0)
    val weeklySavedOver = 3000.0 - weeklyTotalSpentOver
    checkClose("weekly Saved (3000 - 3700) == -700, negative (coral case)", weeklySavedOver, -700.0)
    check("weekly Saved < 0 -> coral/over-spent case", weeklySavedOver < 0.0, true)

    println("\n=== BudgetCalculations: NEW monthly seed (8 categories) against monthlyPool 40000 ===")
    val monthlyCategories = BudgetDefaults.seedMonthlyCategories() // 40,15,8,10,5,3,12,7 = 100%
    check("monthly seed has 8 categories", monthlyCategories.size, 8)
    check("monthly seed totalAllocatedPercent == 100", BudgetCalculations.totalAllocatedPercent(monthlyCategories), 100f)
    checkClose(
        "monthly Rent (40%) allocation on pool 40000 == 16000",
        BudgetCalculations.categoryAllocation(monthlyPool40000, monthlyCategories[0]),
        16000.0
    )
    checkClose(
        "monthly Cat supplies (3%) allocation on pool 40000 == 1200",
        BudgetCalculations.categoryAllocation(monthlyPool40000, monthlyCategories[5]),
        1200.0
    )
    checkClose(
        "monthly Credit Card Payments (12%) allocation on pool 40000 == 4800",
        BudgetCalculations.categoryAllocation(monthlyPool40000, monthlyCategories[6]),
        4800.0
    )
    val monthlySpentCategories = monthlyCategories.mapIndexed { idx, c ->
        if (idx == 0) c.copy(spent = 18000.0) else c // Rent over-allocation: 16000 alloc, 18000 spent -> remaining -2000
    }
    checkClose(
        "monthly Rent remaining over-spent (16000 - 18000) == -2000",
        BudgetCalculations.categoryRemaining(monthlyPool40000, monthlySpentCategories[0]),
        -2000.0
    )
    checkClose(
        "totalAllocated(monthlyPool40000, monthly seed) == pool (100% allocated)",
        BudgetCalculations.totalAllocated(monthlyPool40000, monthlyCategories),
        40000.0
    )

    println("\n=== BudgetCalculations: monthly Income/Spent/Savings stat values ===")
    val monthlySalaryForStats = 50000.0
    val monthlySavingsGoalForStats = 20f
    val monthlyIncomeStat = BudgetCalculations.monthlyIncome(monthlySalaryForStats)
    val monthlySpentStat = BudgetCalculations.totalSpent(monthlySpentCategories) // 18000 + 0*7 = 18000
    val monthlySavingsStat = BudgetCalculations.monthlySavings(monthlySalaryForStats, monthlySavingsGoalForStats)
    checkClose("monthly Income stat == monthlySalary == 50000", monthlyIncomeStat, 50000.0)
    checkClose("monthly Spent stat == sum(category.spent) == 18000", monthlySpentStat, 18000.0)
    checkClose("monthly Savings stat == income * savingsPercent/100 == 10000", monthlySavingsStat, 10000.0)

    val defaultCategories = monthlyCategories // kept for topCategoriesByAllocation section below
    checkClose(
        "leftToAllocate(40000, monthly defaults) == 0 (fully allocated)",
        BudgetCalculations.leftToAllocate(monthlyPool40000, defaultCategories),
        0.0
    )
    check(
        "totalAllocatedPercent(monthly defaults) == 100",
        BudgetCalculations.totalAllocatedPercent(defaultCategories),
        100f
    )

    val overAllocated = listOf(
        Category(id = "1", name = "A", percent = 60f, spent = 0.0),
        Category(id = "2", name = "B", percent = 60f, spent = 0.0)
    )
    check("totalAllocatedPercent(over-allocated) == 120", BudgetCalculations.totalAllocatedPercent(overAllocated), 120f)
    checkClose(
        "leftToAllocate negative when over-allocated",
        BudgetCalculations.leftToAllocate(monthlyPool40000, overAllocated),
        40000.0 - (40000.0 * 0.6 + 40000.0 * 0.6)
    )

    println("\n=== BudgetCalculations: progress fraction clamping ===")
    check("progressFraction(0, 100) == 0", BudgetCalculations.progressFraction(0.0, 100.0), 0f)
    check("progressFraction(50, 100) == 0.5", BudgetCalculations.progressFraction(50.0, 100.0), 0.5f)
    check("progressFraction(150, 100) clamps to 1.0 (over-spent)", BudgetCalculations.progressFraction(150.0, 100.0), 1f)
    check("progressFraction(50, 0) == 0 (no allocation, avoid div/0)", BudgetCalculations.progressFraction(50.0, 0.0), 0f)

    println("\n=== BudgetCalculations: input clamps ===")
    checkClose("clampSalary(-100) -> 0", BudgetCalculations.clampSalary(-100.0), 0.0)
    checkClose("clampSalary(500) -> 500", BudgetCalculations.clampSalary(500.0), 500.0)
    check("clampSavingsPercent(-5) -> 0", BudgetCalculations.clampSavingsPercent(-5f), 0f)
    check("clampSavingsPercent(75) -> 60", BudgetCalculations.clampSavingsPercent(75f), 60f)
    check("clampCategoryPercent(-10) -> 0", BudgetCalculations.clampCategoryPercent(-10f), 0f)
    check("clampCategoryPercent(150) -> 100", BudgetCalculations.clampCategoryPercent(150f), 100f)
    checkClose("clampSpent(-50) -> 0", BudgetCalculations.clampSpent(-50.0), 0.0)
    checkClose("clampWeeklyAllotment(-100) -> 0", BudgetCalculations.clampWeeklyAllotment(-100.0), 0.0)
    checkClose("clampWeeklyAllotment(3500) -> 3500", BudgetCalculations.clampWeeklyAllotment(3500.0), 3500.0)

    println("\n=== BudgetCalculations: topCategoriesByAllocation (new 8-category monthly seed) ===")
    val top3 = BudgetCalculations.topCategoriesByAllocation(monthlyPool40000, defaultCategories, 3)
    check("top3 size", top3.size, 3)
    check("top3[0] is Rent (40%, highest allocation)", top3[0].name, "Rent")
    check("top3[1] is Groceries (15%)", top3[1].name, "Groceries")
    check("top3[2] is Credit Card Payments (12%, beats Utility bill at 7%)", top3[2].name, "Credit Card Payments")

    println("\n=== PeriodStamps: ISO week stamps (known dates) ===")
    // 2026-01-01 is a Thursday. ISO 8601: the week containing the first Thursday of the year is
    // week 1. Jan 1 2026 (Thu) is in week 1 of week-based-year 2026.
    check("2026-01-01 (Thu) -> 2026-W01", PeriodStamps.currentWeekStamp(LocalDate.of(2026, 1, 1)), "2026-W01")
    // 2025-12-29 (Mon) is in the same ISO week as Jan 1 2026 (week starts Monday) -> also W01 2026.
    check("2025-12-29 (Mon, ISO week rolls into 2026) -> 2026-W01", PeriodStamps.currentWeekStamp(LocalDate.of(2025, 12, 29)), "2026-W01")
    // Known reference: 2024-01-01 is a Monday -> ISO week 1 of 2024.
    check("2024-01-01 (Mon) -> 2024-W01", PeriodStamps.currentWeekStamp(LocalDate.of(2024, 1, 1)), "2024-W01")
    // Known reference: 2021-01-01 is a Friday -> belongs to ISO week 53 of week-based-year 2020.
    check("2021-01-01 (Fri) -> 2020-W53 (ISO week-based-year rollback)", PeriodStamps.currentWeekStamp(LocalDate.of(2021, 1, 1)), "2020-W53")
    // 2026-07-02 (today, per task context) - Thursday. Let's verify independently below via
    // month stamp and a direct week count sanity check (week 27 expected for early July).
    check("2026-07-02 -> 2026-W27", PeriodStamps.currentWeekStamp(LocalDate.of(2026, 7, 2)), "2026-W27")

    println("\n=== PeriodStamps: month stamps + zero padding ===")
    check("2026-07-02 -> 2026-M07 (zero-padded)", PeriodStamps.currentMonthStamp(LocalDate.of(2026, 7, 2)), "2026-M07")
    check("2026-01-15 -> 2026-M01 (zero-padded)", PeriodStamps.currentMonthStamp(LocalDate.of(2026, 1, 15)), "2026-M01")
    check("2026-12-31 -> 2026-M12 (no padding needed)", PeriodStamps.currentMonthStamp(LocalDate.of(2026, 12, 31)), "2026-M12")
    check("2026-09-09 -> 2026-M09", PeriodStamps.currentMonthStamp(LocalDate.of(2026, 9, 9)), "2026-M09")

    println("\n=== PeriodStamps: monthLabelFromStamp ===")
    check("monthLabelFromStamp(2026-M07)", PeriodStamps.monthLabelFromStamp("2026-M07"), "July 2026")
    check("monthLabelFromStamp(2026-M01)", PeriodStamps.monthLabelFromStamp("2026-M01"), "January 2026")
    check("monthLabelFromStamp(2026-M12)", PeriodStamps.monthLabelFromStamp("2026-M12"), "December 2026")

    println("\n=== Rollover simulation (repository logic, hand-inlined) ===")
    run {
        val staleWeekly = PeriodBudget(
            categories = listOf(Category(id = "a", name = "A", percent = 50f, spent = 999.0)),
            periodStamp = "2020-W01"
        )
        val freshStamp = PeriodStamps.currentWeekStamp(LocalDate.of(2026, 7, 2))
        val needsRollover = staleWeekly.periodStamp != freshStamp
        check("stale weekly stamp triggers rollover", needsRollover, true)
        val rolledOver = if (needsRollover) {
            staleWeekly.copy(
                categories = staleWeekly.categories.map { it.copy(spent = 0.0) },
                periodStamp = freshStamp
            )
        } else staleWeekly
        checkClose("rollover zeroes spent", rolledOver.categories[0].spent, 0.0)
        check("rollover preserves percent", rolledOver.categories[0].percent, 50f)
        check("rollover preserves name", rolledOver.categories[0].name, "A")
        check("rollover updates stamp", rolledOver.periodStamp, freshStamp)
    }

    println("\n=== Summary ===")
    println("$checks checks run, ${checks - failures} passed, $failures failed")
    if (failures > 0) {
        kotlin.system.exitProcess(1)
    }
}
