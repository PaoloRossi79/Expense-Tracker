package com.paolorossi.expensetracker.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.paolorossi.expensetracker.BuildConfig
import com.paolorossi.expensetracker.data.auth.AuthRepository
import com.paolorossi.expensetracker.data.auth.FirebaseGoogleAuthRepository
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.FirestoreCategoryRepository
import com.paolorossi.expensetracker.data.repository.FirestoreHouseholdRepository
import com.paolorossi.expensetracker.data.repository.FirestoreRecurringRepository
import com.paolorossi.expensetracker.data.repository.FirestoreTransactionRepository
import com.paolorossi.expensetracker.data.repository.FirestoreVendorRepository
import com.paolorossi.expensetracker.data.repository.HouseholdRepository
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository

/**
 * Lightweight dependency locator (no DI framework — keeps the build simple).
 * Repositories are only constructed when Firebase is actually configured, so a
 * fresh checkout without `google-services.json` still launches into a clear
 * "setup required" state instead of crashing.
 */
object ServiceLocator {
    private lateinit var appContext: Context

    /** True once `google-services.json` is present and Firebase initialised. */
    var isConfigured: Boolean = false
        private set

    lateinit var authRepository: AuthRepository
        private set
    lateinit var householdRepository: HouseholdRepository
        private set
    lateinit var transactionRepository: TransactionRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var vendorRepository: VendorRepository
        private set
    lateinit var recurringRepository: RecurringRepository
        private set

    /** Household document id from 00-prerequisites.md §7. */
    val householdId: String get() = BuildConfig.HOUSEHOLD_ID

    fun init(context: Context) {
        appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            isConfigured = false
            return
        }
        isConfigured = true

        val firestore =
            FirebaseFirestore.getInstance().apply {
                firestoreSettings =
                    FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                if (BuildConfig.USE_FIRESTORE_EMULATOR) {
                    useEmulator(BuildConfig.FIRESTORE_EMULATOR_HOST, 8080)
                }
            }

        authRepository =
            FirebaseGoogleAuthRepository(
                auth = FirebaseAuth.getInstance(),
                webClientId = BuildConfig.WEB_CLIENT_ID,
            )
        householdRepository = FirestoreHouseholdRepository(firestore)
        transactionRepository = FirestoreTransactionRepository(firestore)
        categoryRepository = FirestoreCategoryRepository(firestore)
        vendorRepository = FirestoreVendorRepository(firestore)
        recurringRepository = FirestoreRecurringRepository(firestore)
    }
}
