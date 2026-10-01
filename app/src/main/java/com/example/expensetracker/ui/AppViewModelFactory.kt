package com.paolorossi.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.ui.addtransaction.AddTransactionViewModel
import com.paolorossi.expensetracker.ui.auth.RootViewModel
import com.paolorossi.expensetracker.ui.categories.CategoriesViewModel
import com.paolorossi.expensetracker.ui.home.HomeViewModel
import com.paolorossi.expensetracker.ui.members.AddMemberViewModel
import com.paolorossi.expensetracker.ui.recurring.RecurringRulesViewModel
import com.paolorossi.expensetracker.ui.transactions.TransactionsViewModel
import com.paolorossi.expensetracker.ui.vendors.VendorsViewModel

/** Single factory that builds every ViewModel from [ServiceLocator] repositories. */
object AppViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel =
            when {
                modelClass.isAssignableFrom(RootViewModel::class.java) ->
                    RootViewModel()

                modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                    HomeViewModel(
                        ServiceLocator.transactionRepository,
                        ServiceLocator.recurringRepository,
                    )

                modelClass.isAssignableFrom(AddTransactionViewModel::class.java) ->
                    AddTransactionViewModel(
                        ServiceLocator.transactionRepository,
                        ServiceLocator.categoryRepository,
                        ServiceLocator.vendorRepository,
                        ServiceLocator.recurringRepository,
                    )

                modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                    TransactionsViewModel(
                        ServiceLocator.transactionRepository,
                        ServiceLocator.categoryRepository,
                    )

                modelClass.isAssignableFrom(CategoriesViewModel::class.java) ->
                    CategoriesViewModel(
                        ServiceLocator.categoryRepository,
                        ServiceLocator.transactionRepository,
                    )

                modelClass.isAssignableFrom(VendorsViewModel::class.java) ->
                    VendorsViewModel(
                        ServiceLocator.vendorRepository,
                        ServiceLocator.categoryRepository,
                    )

                modelClass.isAssignableFrom(RecurringRulesViewModel::class.java) ->
                    RecurringRulesViewModel(
                        ServiceLocator.recurringRepository,
                        ServiceLocator.categoryRepository,
                        ServiceLocator.vendorRepository,
                    )

                modelClass.isAssignableFrom(AddMemberViewModel::class.java) ->
                    AddMemberViewModel(ServiceLocator.householdRepository)

                else -> error("Unknown ViewModel: ${modelClass.name}")
            }
        return viewModel as T
    }
}
