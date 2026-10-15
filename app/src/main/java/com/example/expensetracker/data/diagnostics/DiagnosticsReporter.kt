package com.paolorossi.expensetracker.data.diagnostics

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores a diagnostic report locally and uploads it to the Firestore
 * `diagnostics` collection (free Spark tier). Reports that fail to upload are
 * retried on the next app start via [flushPending].
 *
 * Two entry points feed the same queue:
 *  - [send] for sign-in failures and manual reports;
 *  - [persistCrash] for uncaught exceptions, called from the app's default
 *    uncaught-exception handler. That path is intentionally synchronous and
 *    coroutine-free, because the process is about to die — only a plain file
 *    write is safe there. The next launch uploads it.
 *
 * The collection is create-only for clients and requires an authenticated
 * identity. The app prefers an existing Google session and otherwise signs in
 * anonymously ([ensureSignedIn]); anonymous sign-in is a separate path from
 * Google, so a device that cannot complete Google sign-in can still report why.
 *
 * All file work runs on [Dispatchers.IO], and the pending queue is pruned by
 * count and age so it can never grow without bound.
 */
class DiagnosticsReporter(
    private val context: Context,
    private val firestore: FirebaseFirestore,
) {
    /** Builds, persists, and uploads a report. Returns the report id. */
    suspend fun send(
        throwable: Throwable?,
        trigger: String,
        note: String? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val report = DiagnosticsCollector.collect(context, throwable, trigger, note)
                val id = newReportId(report)
                val dir = pendingDir().apply { mkdirs() }
                prunePending(dir)
                writePending(dir, id, report)
                upload(id, report)
                id
            }.onFailure { Log.w(TAG, "Diagnostics send failed", it) }
        }

    /** Re-uploads reports left behind by earlier failed attempts. */
    suspend fun flushPending(): Int =
        withContext(Dispatchers.IO) {
            val dir = pendingDir()
            prunePending(dir)
            var sent = 0
            for (file in dir.listFiles()?.toList().orEmpty()) {
                val id = file.nameWithoutExtension
                val outcome =
                    runCatching {
                        upload(id, jsonToMap(JSONObject(file.readText())))
                    }
                if (outcome.isSuccess) {
                    sent++
                } else {
                    // Without this the queue can silently never drain.
                    Log.w(TAG, "Pending diagnostics report $id not uploaded", outcome.exceptionOrNull())
                }
            }
            sent
        }

    private suspend fun upload(
        id: String,
        report: Map<String, Any?>,
    ) {
        ensureSignedIn()
        val payload = withoutNulls(report).toMutableMap()
        payload["createdAt"] = FieldValue.serverTimestamp()
        try {
            firestore.collection(COLLECTION).document(id).set(payload).await()
        } catch (failure: Throwable) {
            if (isPermanent(failure)) {
                // The rule rejected the shape, or this document id already
                // exists (a create-only rule denies the retry as an update).
                // Either way it can never upload, so drop the local copy
                // instead of retrying it on every launch.
                pendingDir().resolve("$id.json").delete()
                Log.w(TAG, "Dropped undeliverable diagnostics report $id", failure)
            }
            throw failure
        }
        pendingDir().resolve("$id.json").delete()
    }

    /**
     * Anonymous sign-in must be enabled in the Firebase console. If it is not,
     * the report stays in the pending queue and is retried on a later launch.
     */
    private suspend fun ensureSignedIn() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    /** True when retrying can never succeed for this request. */
    private fun isPermanent(failure: Throwable): Boolean {
        var cause: Throwable? = failure
        while (cause != null) {
            if (cause is FirebaseFirestoreException && cause.code in PERMANENT_CODES) return true
            cause = cause.cause
        }
        return false
    }

    /** Keeps at most [MAX_PENDING] of the newest reports, none older than [MAX_AGE_MS]. */
    private fun prunePending(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        val now = System.currentTimeMillis()
        files.forEachIndexed { index, file ->
            if (index >= MAX_PENDING || now - file.lastModified() > MAX_AGE_MS) {
                file.delete()
            }
        }
    }

    private fun writePending(
        dir: File,
        id: String,
        report: Map<String, Any?>,
    ) {
        runCatching { dir.resolve("$id.json").writeText(toJson(report).toString()) }
            .onFailure { Log.w(TAG, "Could not persist diagnostics locally", it) }
    }

    private fun pendingDir(): File = File(context.filesDir, "diagnostics")

    private fun newReportId(report: Map<String, Any?>): String = "${report["installId"]}-${System.currentTimeMillis()}"

    companion object {
        private const val TAG = "Diagnostics"
        private const val COLLECTION = "diagnostics"
        private const val MAX_PENDING = 20
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        private val PERMANENT_CODES =
            setOf(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.INVALID_ARGUMENT,
                FirebaseFirestoreException.Code.ALREADY_EXISTS,
                FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            )

        /**
         * Records an uncaught exception into the pending queue. Safe to call
         * from the default uncaught-exception handler: it performs only a
         * bounded, synchronous file write and never throws.
         *
         * The next app start uploads it via [flushPending], which is the only
         * way to see a crash on a device that cannot be attached over USB.
         */
        fun persistCrash(
            context: Context,
            throwable: Throwable,
        ): String? =
            runCatching {
                val report =
                    DiagnosticsCollector.collect(
                        context = context,
                        throwable = throwable,
                        trigger = "uncaught_exception",
                        note = crashNote(),
                    )
                val id = "${report["installId"]}-${System.currentTimeMillis()}"
                val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
                dir.resolve("$id.json").writeText(toJson(report).toString())
                id
            }.onFailure { Log.w(TAG, "Could not persist crash report", it) }
                .getOrNull()

        private fun crashNote(): String {
            val thread = Thread.currentThread()
            return "thread=${thread.name} main=${thread === Looper.getMainLooper().thread}"
        }

        /** Firestore rejects nulls, so drop them (and empty containers) recursively. */
        private fun withoutNulls(map: Map<String, Any?>): Map<String, Any> {
            val out = LinkedHashMap<String, Any>()
            for ((key, value) in map) {
                val cleaned = cleanValue(value) ?: continue
                out[key] = cleaned
            }
            return out
        }

        private fun cleanValue(value: Any?): Any? =
            when (value) {
                null -> null
                is Map<*, *> -> {
                    val nested = LinkedHashMap<String, Any>()
                    for ((key, item) in value) {
                        val cleaned = cleanValue(item) ?: continue
                        nested[key.toString()] = cleaned
                    }
                    nested.ifEmpty { null }
                }

                is List<*> -> value.mapNotNull { cleanValue(it) }.ifEmpty { null }
                is Array<*> -> cleanValue(value.toList())
                is Boolean, is Number, is String -> value
                else -> value.toString()
            }

        private fun toJson(value: Any?): Any =
            when (value) {
                null -> JSONObject.NULL
                is Map<*, *> -> {
                    val obj = JSONObject()
                    for ((key, item) in value) obj.put(key.toString(), toJson(item))
                    obj
                }

                is List<*> -> {
                    val arr = JSONArray()
                    for (item in value) arr.put(toJson(item))
                    arr
                }

                is Array<*> -> toJson(value.toList())
                else -> value
            }

        private fun jsonToMap(json: JSONObject): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            for (key in json.keys()) out[key] = jsonToValue(json.get(key))
            return out
        }

        private fun jsonToValue(value: Any?): Any? =
            when (value) {
                JSONObject.NULL -> null
                is JSONObject -> jsonToMap(value)
                is JSONArray -> (0 until value.length()).map { jsonToValue(value.get(it)) }
                else -> value
            }
    }
}
