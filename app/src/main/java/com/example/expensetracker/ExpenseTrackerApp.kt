package com.paolorossi.expensetracker

import android.app.Application
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.recurring.RecurringNotifier

class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        RecurringNotifier.ensureChannel(this)
    }
}
