package com.paolorossi.expensetracker.integration

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Integration tests run against the local Firestore emulator (Design/03).
 * A named FirebaseApp is created with dummy options so these tests don't need
 * `google-services.json`; `useEmulator` redirects all traffic to the emulator.
 *
 * On a standard Android emulator the host machine is reachable at 10.0.2.2.
 * Start it first: `firebase emulators:start --only firestore`.
 */
object FirestoreEmulator {
    private const val APP_NAME = "integration-test"
    private const val HOST = "10.0.2.2"
    private const val PORT = 8080
    private const val PROJECT_ID = "demo-expense-tracker"

    fun firestore(context: Context): FirebaseFirestore {
        val app =
            FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
                ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApplicationId("1:000000000000:android:0000000000000000")
                        .setProjectId(PROJECT_ID)
                        .setApiKey("fake-api-key")
                        .build(),
                    APP_NAME,
                )!!

        return FirebaseFirestore.getInstance(app).apply {
            useEmulator(HOST, PORT)
        }
    }
}
