package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@Keep
@IgnoreExtraProperties
data class OwnerProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerName: String = "",
    val businessName: String = "",
    val ownerMobile: String = "",
    val gstin: String = "",
    val upiId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSnapshotDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val productCount: Long = 0L,
    val customerCount: Long = 0L,
    val invoiceCount: Long = 0L,
    val totalRevenue: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Owner must be authenticated before syncing to cloud.")
    }

    fun observeCloudSnapshots(userId: String): Flow<List<CloudSnapshotDoc>> = flow {
        val path = "users/$userId/cloud_snapshots"
        emitAll(
            db.collection("users").document(userId).collection("cloud_snapshots")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudSnapshotDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncOwnerProfile(
        ownerName: String,
        businessName: String,
        ownerMobile: String,
        gstin: String,
        upiId: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            val safeOwner = ownerName.trim().take(100).ifBlank { "NASIR ALI" }
            val safeBiz = businessName.trim().take(120).ifBlank { "NAQSH COLLECTION" }
            val safeMobile = ownerMobile.trim().take(30).ifBlank { "+91 9471477887" }
            val safeGstin = gstin.trim().take(32)
            val safeUpi = upiId.trim().take(100)

            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "ownerName" to safeOwner,
                        "businessName" to safeBiz,
                        "ownerMobile" to safeMobile,
                        "gstin" to safeGstin,
                        "upiId" to safeUpi,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "ownerName" to safeOwner,
                        "businessName" to safeBiz,
                        "ownerMobile" to safeMobile,
                        "gstin" to safeGstin,
                        "upiId" to safeUpi,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushCloudSnapshot(
        label: String,
        productCount: Int,
        customerCount: Int,
        invoiceCount: Int,
        totalRevenue: Double
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/cloud_snapshots"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("cloud_snapshots").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "label" to label.trim().take(120).ifBlank { "Boutique Snapshot" },
                "productCount" to productCount.coerceAtLeast(0),
                "customerCount" to customerCount.coerceAtLeast(0),
                "invoiceCount" to invoiceCount.coerceAtLeast(0),
                "totalRevenue" to totalRevenue.coerceAtLeast(0.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
