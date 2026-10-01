package com.paolorossi.expensetracker.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository
import com.paolorossi.expensetracker.domain.CategoryRules
import com.paolorossi.expensetracker.domain.Money
import com.paolorossi.expensetracker.domain.VendorMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val vendor: String = "",
    val date: LocalDate = LocalDate.now(),
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val vendors: List<Vendor> = emptyList(),
    val editingId: String? = null,
    val recurringRuleId: String? = null,
    val dueDate: LocalDate? = null,
    val loading: Boolean = true,
    val finished: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = editingId != null
    val isRecurringConfirm: Boolean get() = recurringRuleId != null

    val availableCategories: List<Category>
        get() = CategoryRules.categoriesFor(type, categories)
}

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val vendorRepository: VendorRepository,
    private val recurringRepository: RecurringRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()

    private var loaded = false

    fun load(
        type: TransactionType,
        transactionId: String?,
        recurringRuleId: String?,
        dueDate: LocalDate?,
    ) {
        if (loaded) return
        loaded = true
        _state.update {
            it.copy(type = type, editingId = transactionId, recurringRuleId = recurringRuleId, dueDate = dueDate)
        }
        viewModelScope.launch {
            val householdId = ServiceLocator.householdId
            val categories =
                runCatching {
                    categoryRepository.observeCategories(householdId).first()
                }.getOrDefault(emptyList())
            val vendors =
                runCatching {
                    vendorRepository.observeVendors(householdId).first()
                }.getOrDefault(emptyList())
            _state.update { it.copy(categories = categories, vendors = vendors, loading = false) }

            when {
                transactionId != null -> prefillFromTransaction(householdId, transactionId)
                recurringRuleId != null -> prefillFromRule(householdId, recurringRuleId, dueDate)
            }
        }
    }

    private suspend fun prefillFromTransaction(
        householdId: String,
        transactionId: String,
    ) {
        val transaction =
            runCatching {
                transactionRepository.getTransaction(householdId, transactionId)
            }.getOrNull() ?: return
        _state.update {
            it.copy(
                type = transaction.type,
                amount = Money.toPlainString(transaction.amountPence),
                vendor = transaction.vendor,
                date = transaction.date,
                categoryId = transaction.categoryId,
                editingId = transaction.id,
            )
        }
    }

    private suspend fun prefillFromRule(
        householdId: String,
        ruleId: String,
        dueDate: LocalDate?,
    ) {
        val rule =
            runCatching {
                recurringRepository.observeRules(householdId).first().firstOrNull { it.id == ruleId }
            }.getOrNull() ?: return
        _state.update {
            it.copy(
                type = rule.type,
                amount = Money.toPlainString(rule.amountPence),
                vendor = rule.reason,
                date = dueDate ?: LocalDate.now(),
                categoryId = rule.categoryId,
            )
        }
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amount = value, error = null) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun onCategorySelect(category: Category) = _state.update { it.copy(categoryId = category.id) }

    /** Vendor auto-allocation: match, then pre-fill the category (Design/01 §4.8). */
    fun onVendorChange(value: String) {
        val match = VendorMatcher.bestMatch(value, _state.value.vendors)
        _state.update { current ->
            val autoCategory =
                match?.vendor?.defaultCategoryId
                    ?.takeIf { id -> current.categories.any { it.id == id } }
            current.copy(vendor = value, categoryId = autoCategory ?: current.categoryId)
        }
    }

    fun createCategory(name: String) {
        val type = _state.value.type
        val categoryType =
            when (type) {
                TransactionType.EXPENSE -> CategoryType.EXPENSE
                TransactionType.INCOME -> CategoryType.INCOME
            }
        viewModelScope.launch {
            val category = Category(name = name, type = categoryType)
            val id =
                runCatching {
                    categoryRepository.createCategory(ServiceLocator.householdId, category)
                }.getOrNull() ?: return@launch
            _state.update {
                it.copy(
                    categories = (it.categories + category.copy(id = id)).sortedBy { c -> c.name },
                    categoryId = id,
                )
            }
        }
    }

    fun save() {
        val snapshot = _state.value
        val pence = Money.parsePoundsToPence(snapshot.amount)
        if (pence == null || pence <= 0L) {
            _state.update { it.copy(error = "Enter a valid amount") }
            return
        }
        val categoryId = snapshot.categoryId
        if (categoryId.isNullOrBlank()) {
            _state.update { it.copy(error = "Choose a category") }
            return
        }
        val user = ServiceLocator.authRepository.currentUser
        if (user == null) {
            _state.update { it.copy(error = "Not signed in") }
            return
        }

        viewModelScope.launch {
            val result =
                runCatching {
                    when {
                        snapshot.recurringRuleId != null && snapshot.dueDate != null -> {
                            val rule =
                                recurringRepository.observeRules(ServiceLocator.householdId)
                                    .first()
                                    .first { it.id == snapshot.recurringRuleId }
                            recurringRepository.confirmOccurrence(
                                householdId = ServiceLocator.householdId,
                                rule = rule,
                                date = snapshot.dueDate,
                                user = user,
                                amountPence = pence,
                                categoryId = categoryId,
                                vendor = snapshot.vendor,
                            )
                        }

                        snapshot.editingId != null ->
                            transactionRepository.updateTransaction(
                                ServiceLocator.householdId,
                                Transaction(
                                    id = snapshot.editingId,
                                    type = snapshot.type,
                                    amountPence = pence,
                                    date = snapshot.date,
                                    vendor = snapshot.vendor,
                                    categoryId = categoryId,
                                    createdByUid = user.uid,
                                    createdByName = user.displayName,
                                ),
                            )

                        else ->
                            transactionRepository.addTransaction(
                                ServiceLocator.householdId,
                                Transaction(
                                    type = snapshot.type,
                                    amountPence = pence,
                                    date = snapshot.date,
                                    vendor = snapshot.vendor,
                                    categoryId = categoryId,
                                    createdByUid = user.uid,
                                    createdByName = user.displayName,
                                ),
                            )
                    }
                }
            result
                .onSuccess { _state.update { it.copy(finished = true) } }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Save failed") }
                }
        }
    }
}
