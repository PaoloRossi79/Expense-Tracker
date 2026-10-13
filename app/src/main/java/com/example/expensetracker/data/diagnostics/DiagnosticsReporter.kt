package com.paolorossi.expensetracker.data.diagnostics

import android.content.Context
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
 * `diagnostics` collection (free Spark tier). Uploads that fail are retried on
 * the next app start via [flushPending].
 *
 * The collection is create-only for clients and requires an authenticated
 * identity. The app prefers an existing Google session and otherwise signs in
 * anonymously ([ensureSignedIn]); anonymous sign-in is a separate path from
 * Google, so a device that cannot complete Google sign-in can still report why.
 * Reads are limited to household members in `firestore.rules`.
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
                val id = "${report["installId"]}-${System.currentTimeMillis()}"
                val dir = pendingDir().apply { mkdirs() }
                prunePending(dir)
                writePending(dir, id, report)
                upload(id, report)
                id
            }
        }

    /** Re-uploads reports left behind by earlier failed attempts. */
    suspend fun flushPending(): Int =
        withContext(Dispatchers.IO) {
            val dir = pendingDir()
            prunePending(dir)
            var sent = 0
            for (file in dir.listFiles()?.toList().orEmpty()) {
                val id = file.nameWithoutExtension
                val ok =
                    runCatching {
                        upload(id, jsonToMap(JSONObject(file.readText())))
                    }.isSuccess
                if (ok) sent++
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

    private companion object {
        const val TAG = "Diagnostics"
        const val COLLECTION = "diagnostics"
        const val MAX_PENDING = 20
        const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        val PERMANENT_CODES =
            setOf(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.INVALID_ARGUMENT,
                FirebaseFirestoreException.Code.ALREADY_EXISTS,
                FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            )
    }
}
