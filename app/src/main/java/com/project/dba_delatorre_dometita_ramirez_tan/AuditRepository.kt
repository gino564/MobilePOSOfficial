package com.project.dba_delatorre_dometita_ramirez_tan

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AuditRepository(
    private val daoAuditLog: Dao_AuditLog
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auditCollection = firestore.collection("audit_trail")

    companion object {
        private const val TAG = "AuditRepository"
    }

    // ============ LOG USER ACTION ============

    suspend fun logAction(
        action: String,
        description: String,
        status: String = "Success",
        usernameParam: String? = null // ✅ Add this parameter
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = UserSession.currentUser

                val username = usernameParam ?: currentUser?.Entity_username ?: "Unknown"
                val fullName = UserSession.getUserFullName()

                // Determine online status based on action
                val isOnline = when (action) {
                    AuditActions.LOGIN -> true
                    AuditActions.LOGOUT -> false
                    else -> currentUser != null
                }

                val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d(TAG, "📝 Logging audit action...")
                android.util.Log.d(TAG, "Username: $username")
                android.util.Log.d(TAG, "Full Name: $fullName")
                android.util.Log.d(TAG, "Action: $action")
                android.util.Log.d(TAG, "Description: $description")
                android.util.Log.d(TAG, "Online Status: $isOnline")

                // Create audit log
                val auditLog = Entity_AuditLog(
                    username = username,
                    action = action,
                    description = description,
//                    timestamp = System.currentTimeMillis(),
                    dateTime = dateTime,
                    status = status,
                    isOnline = isOnline
                )

                // Save to Firebase
                val auditData = hashMapOf(
                    "username" to auditLog.username,
                    "action" to auditLog.action,
                    "description" to auditLog.description,
//                    "timestamp" to auditLog.timestamp,
                    "dateTime" to auditLog.dateTime,
                    "status" to auditLog.status,
                    "isOnline" to auditLog.isOnline
                )

                val docRef = auditCollection.add(auditData).await()
                android.util.Log.d(TAG, "✅ Audit log saved to Firebase: ${docRef.id}")

                // Save to Room
                daoAuditLog.insertAuditLog(auditLog)
                android.util.Log.d(TAG, "✅ Audit log saved to Room")
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                android.util.Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e(TAG, "❌ Failed to log audit action!")
                android.util.Log.e(TAG, "Error: ${e.message}", e)
                android.util.Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }
    // ============ GET ALL AUDIT LOGS ============

    suspend fun getAllAuditLogs(): List<Entity_AuditLog> {
        return withContext(Dispatchers.IO) {
            try {
                daoAuditLog.getAllAuditLogs()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getting audit logs: ${e.message}", e)
                emptyList()
            }
        }
    }
}