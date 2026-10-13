package com.paolorossi.expensetracker.data.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.paolorossi.expensetracker.BuildConfig
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Builds a self-describing diagnostic report for a device, so a build that
 * cannot be attached over USB can still explain a remote failure (e.g. Google
 * sign-in returning "No credentials available").
 *
 * Read the reports in the Firebase console: Firestore -> `diagnostics`. Each
 * document carries the device identity (model, OS, app version, **runtime
 * signing SHA-1/SHA-256**, installer), the auth configuration, the associated
 * Firebase identity, and the exact exception.
 *
 * Deliberately excluded: the device's Google account list and any hardware
 * identifier other than the app-scoped `installId`. Those are PII that cannot be
 * read reliably on modern Android (the `GET_ACCOUNTS` grant is not requested),
 * so the user-entered `note` and the Firebase user are used for identity.
 */
object DiagnosticsCollector {
    /** Document shape marker; also enforced by `firestore.rules`. */
    const val SCHEMA = "et-diag-v1"

    /** Cheap gate so a stray client cannot write an arbitrary payload shape. */
    const val TOKEN = "et-diag-v1"

    private const val PREFS = "diagnostics"
    private const val KEY_INSTALL_ID = "installId"

    /** Stable per-install identifier, shared across reports from one device. */
    fun installId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_INSTALL_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_ID, id).apply()
        return id
    }

    fun collect(
        context: Context,
        throwable: Throwable?,
        trigger: String,
        note: String?,
    ): Map<String, Any?> {
        val report =
            linkedMapOf<String, Any?>(
                "schema" to SCHEMA,
                "token" to TOKEN,
                "trigger" to trigger,
                "installId" to installId(context),
                "createdAtClient" to isoNow(),
                "householdId" to BuildConfig.HOUSEHOLD_ID,
                "app" to appInfo(context),
                "device" to deviceInfo(),
                "google" to googleInfo(context),
                "authConfig" to authConfig(),
                "authState" to authState(),
            )
        if (!note.isNullOrBlank()) report["note"] = note.take(2000)
        if (throwable != null) report["exception"] = exceptionInfo(throwable)
        return report
    }

    private fun appInfo(context: Context): Map<String, Any?> {
        val pm = context.packageManager
        val info = runCatching { pm.getPackageInfo(context.packageName, 0) }.getOrNull()
        val (sha1, sha256) = signingFingerprints(context)
        return linkedMapOf(
            "packageName" to context.packageName,
            "versionName" to info?.versionName,
            "versionCode" to info?.let(::versionCodeOf),
            "debuggable" to
                info?.applicationInfo?.let { (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 },
            "installer" to installerPackage(context),
            "firstInstallTimeMs" to info?.firstInstallTime,
            "lastUpdateTimeMs" to info?.lastUpdateTime,
            "signingSha1" to sha1,
            "signingSha256" to sha256,
        )
    }

    private fun deviceInfo(): Map<String, Any?> =
        linkedMapOf(
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "hardware" to Build.HARDWARE,
            "sdkInt" to Build.VERSION.SDK_INT,
            "release" to Build.VERSION.RELEASE,
            "buildId" to Build.ID,
            "abis" to Build.SUPPORTED_ABIS?.toList(),
            "isEmulator" to isEmulator(),
            "locale" to Locale.getDefault().toString(),
            "timezone" to TimeZone.getDefault().id,
        )

    private fun googleInfo(context: Context): Map<String, Any?> {
        val pm = context.packageManager
        val gms = runCatching { pm.getPackageInfo("com.google.android.gms", 0) }.getOrNull()
        return linkedMapOf(
            "playServicesInstalled" to (gms != null),
            "playServicesVersionName" to gms?.versionName,
            "playServicesVersionCode" to gms?.let(::versionCodeOf),
        )
    }

    private fun authConfig(): Map<String, Any?> {
        val webClientId = BuildConfig.WEB_CLIENT_ID
        return linkedMapOf(
            "webClientId" to webClientId,
            "webClientIdPresent" to webClientId.isNotBlank(),
            "firebaseAppId" to runCatching { FirebaseApp.getInstance().options.applicationId }.getOrNull(),
            "firebaseProjectId" to runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull(),
        )
    }

    private fun authState(): Map<String, Any?> {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        return linkedMapOf(
            "signedIn" to (user != null),
            "uid" to user?.uid,
            "email" to user?.email,
        )
    }

    private fun exceptionInfo(throwable: Throwable): Map<String, Any?> {
        val info =
            linkedMapOf<String, Any?>(
                "class" to throwable.javaClass.name,
                "message" to throwable.message,
                "stackTrace" to throwable.stackTraceToString().take(8000),
            )
        // Credential Manager exceptions carry a type and (for custom subclasses)
        // an error code; read them reflectively so we don't depend on the exact
        // credentials library version.
        reflectString(throwable, "getType")?.let { info["credentialType"] = it }
        reflectString(throwable, "getErrorCode")?.let { info["errorCode"] = it }
        val causes = mutableListOf<String>()
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 8) {
            causes += "${cause.javaClass.name}: ${cause.message}"
            cause = cause.cause
            depth++
        }
        if (causes.isNotEmpty()) info["causeChain"] = causes
        return info
    }

    private fun reflectString(
        target: Any,
        method: String,
    ): String? = runCatching { target.javaClass.getMethod(method).invoke(target) as? String }.getOrNull()

    private fun signingFingerprints(context: Context): Pair<String?, String?> {
        val pm = context.packageManager
        val signatures =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info =
                        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    val signing = info.signingInfo
                    when {
                        signing == null -> null
                        signing.hasMultipleSigners() -> signing.apkContentsSigners
                        else -> signing.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
                }
            }.getOrNull()
        val first: Signature = signatures?.firstOrNull() ?: return null to null
        return digest("SHA-1", first.toByteArray()) to digest("SHA-256", first.toByteArray())
    }

    private fun digest(
        algorithm: String,
        bytes: ByteArray,
    ): String? =
        runCatching {
            MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }
        }.getOrNull()

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }

    private fun installerPackage(context: Context): String? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("vbox") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk")

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            .apply { timeZone = TimeZone.getDefault() }
            .format(Date())
}
