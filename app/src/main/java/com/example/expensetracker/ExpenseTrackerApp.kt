package com.paolorossi.expensetracker

import android.app.Application
import android.os.Process
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.diagnostics.DiagnosticsReporter
import com.paolorossi.expensetracker.data.recurring.RecurringNotifier
import kotlin.system.exitProcess

class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        RecurringNotifier.ensureChannel(this)
        installCrashRecorder()
    }

    /**
     * Persists uncaught exceptions to the diagnostics queue so the next launch
     * can upload them. Without this, a crash on a device that cannot be
     * attached over USB leaves no trace at all.
     *
     * The write is synchronous and coroutine-free on purpose: the process is
     * about to die, so only a plain file write is safe.
     */
    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { DiagnosticsReporter.persistCrash(applicationContext, throwable) }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
}
