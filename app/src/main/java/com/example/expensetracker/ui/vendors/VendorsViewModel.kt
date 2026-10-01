package com.paolorossi.expensetracker.ui.vendors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VendorsUiState(
    val vendors: List<Vendor> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

class VendorsViewModel(
    private val vendorRepository: VendorRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VendorsUiState())
    val state: StateFlow<VendorsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                vendorRepository.observeVendors(ServiceLocator.householdId),
                categoryRepository.observeCategories(ServiceLocator.householdId),
            ) { vendors, categories ->
                VendorsUiState(vendors = vendors, categories = categories)
            }.collect { _state.value = it }
        }
    }

    fun create(
        name: String,
        defaultCategoryId: String?,
        aliasesCsv: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                vendorRepository.createVendor(
                    ServiceLocator.householdId,
                    Vendor(
                        name = name.trim(),
                        defaultCategoryId = defaultCategoryId,
                        matchAliases = parseAliases(aliasesCsv),
                    ),
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Create failed") }
            }
        }
    }

    fun update(
        vendor: Vendor,
        name: String,
        defaultCategoryId: String?,
        aliasesCsv: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                vendorRepository.updateVendor(
                    ServiceLocator.householdId,
                    vendor.copy(
                        name = name.trim(),
                        defaultCategoryId = defaultCategoryId,
                        matchAliases = parseAliases(aliasesCsv),
                    ),
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Update failed") }
            }
        }
    }

    fun delete(vendor: Vendor) {
        viewModelScope.launch {
            runCatching {
                vendorRepository.deleteVendor(ServiceLocator.householdId, vendor.id)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Delete failed") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun parseAliases(csv: String): List<String> = csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
