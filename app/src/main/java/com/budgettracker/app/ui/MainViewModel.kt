package com.budgettracker.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgettracker.app.data.BudgetDefaults
import com.budgettracker.app.data.BudgetPeriod
import com.budgettracker.app.data.BudgetRepository
import com.budgettracker.app.data.BudgetState
import com.budgettracker.app.data.Category
import com.budgettracker.app.data.PeriodStamps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel backed by the DataStore `Flow<BudgetState>` so rotation and process death don't lose
 * state - DataStore is the source of truth, not in-memory-only state. Also owns the selected
 * period tab (Weekly/Monthly), which can be seeded from the widget tap intent extra.
 */
class MainViewModel(
    private val repository: BudgetRepository,
    initialPeriod: BudgetPeriod = BudgetPeriod.WEEKLY
) : ViewModel() {

    private val seedState = BudgetDefaults.seedBudgetState(
        weeklyStamp = PeriodStamps.currentWeekStamp(),
        monthlyStamp = PeriodStamps.currentMonthStamp()
    )

    val budgetState: StateFlow<BudgetState> = repository.budgetState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = seedState
    )

    val appLockEnabled: StateFlow<Boolean> = repository.appLockEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val maskWidgetAmounts: StateFlow<Boolean> = repository.maskWidgetAmounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _selectedPeriod = MutableStateFlow(initialPeriod)
    val selectedPeriod: StateFlow<BudgetPeriod> = _selectedPeriod

    init {
        // Run the rollover check synchronously on ViewModel init (i.e. on app open), per spec,
        // as a belt-and-suspenders alongside BudgetApplication's own app-open check and the
        // WorkManager worker.
        viewModelScope.launch {
            repository.applyRolloverIfNeeded()
        }
    }

    fun selectPeriod(period: BudgetPeriod) {
        _selectedPeriod.value = period
    }

    fun updateSalary(newSalary: Double) {
        viewModelScope.launch { repository.updateSalary(newSalary) }
    }

    fun updateSavingsGoalPercent(newPercent: Float) {
        viewModelScope.launch { repository.updateSavingsGoalPercent(newPercent) }
    }

    fun updateWeeklyAllotment(newAllotment: Double) {
        viewModelScope.launch { repository.updateWeeklyAllotment(newAllotment) }
    }

    fun updateCategory(period: BudgetPeriod, category: Category) {
        viewModelScope.launch { repository.updateCategory(period, category) }
    }

    fun addCategory(period: BudgetPeriod) {
        viewModelScope.launch { repository.addCategory(period) }
    }

    fun deleteCategory(period: BudgetPeriod, categoryId: String) {
        viewModelScope.launch { repository.deleteCategory(period, categoryId) }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAppLockEnabled(enabled) }
    }

    fun setMaskWidgetAmounts(masked: Boolean) {
        viewModelScope.launch { repository.setMaskWidgetAmounts(masked) }
    }

    class Factory(
        private val repository: BudgetRepository,
        private val initialPeriod: BudgetPeriod
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, initialPeriod) as T
        }
    }
}
