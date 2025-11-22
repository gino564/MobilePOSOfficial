package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WasteLogRepository(private val dao: Dao_WasteLog) {

    private val firestore = FirebaseFirestore.getInstance()
    private val wasteLogsCollection = firestore.collection("waste_logs")

    // ============ INSERT WASTE LOG ============

    suspend fun insertWasteLog(wasteLog: Entity_WasteLog) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WasteLogRepo", "➕ Recording waste: ${wasteLog.productName} - ${wasteLog.quantity} units")

                // Insert to local database first
                dao.insertWasteLog(wasteLog)

                // Sync to Firestore (fire-and-forget for performance)
                syncWasteLogToFirebase(wasteLog)

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ Failed to insert waste log: ${e.message}", e)
                throw e
            }
        }
    }

    // ============ SYNC TO FIRESTORE (OPTIMIZED) ============

    private suspend fun syncWasteLogToFirebase(wasteLog: Entity_WasteLog) {
        try {
            // Create data map for Firestore
            val wasteData = hashMapOf(
                "productFirebaseId" to wasteLog.productFirebaseId,
                "productName" to wasteLog.productName,
                "category" to wasteLog.category,
                "quantity" to wasteLog.quantity,
                "reason" to wasteLog.reason,
                "wasteDate" to wasteLog.wasteDate,
                "recordedBy" to wasteLog.recordedBy
            )

            // Add to Firestore
            val docRef = wasteLogsCollection.add(wasteData).await()
            Log.d("WasteLogRepo", "✅ Waste log synced to Firestore: ${docRef.id}")

            // Update local record with Firebase ID
            dao.markAsSynced(wasteLog.id, docRef.id)

        } catch (e: Exception) {
            Log.e("WasteLogRepo", "⚠️ Firestore sync failed (data saved locally): ${e.message}")
            // Don't throw - waste is already recorded locally
        }
    }

    // ============ SYNC UNSYNCED LOGS ============

    suspend fun syncUnsyncedLogs() {
        withContext(Dispatchers.IO) {
            try {
                val unsyncedLogs = dao.getUnsyncedWasteLogs()
                Log.d("WasteLogRepo", "🔄 Found ${unsyncedLogs.size} unsynced waste logs")

                unsyncedLogs.forEach { log ->
                    syncWasteLogToFirebase(log)
                }

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ Sync failed: ${e.message}")
            }
        }
    }

    // ============ FETCH FROM FIRESTORE (OPTIMIZED) ============

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WasteLogRepo", "📡 Fetching waste logs from Firestore...")

                // Fetch only recent logs (last 30 days) to minimize reads
                val thirtyDaysAgo = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))

                val snapshot = wasteLogsCollection
                    .whereGreaterThanOrEqualTo("wasteDate", thirtyDaysAgo)
                    .get()
                    .await()

                Log.d("WasteLogRepo", "✅ Fetched ${snapshot.documents.size} waste logs")

                val wasteLogs = snapshot.documents.mapNotNull { doc ->
                    try {
                        Entity_WasteLog(
                            firebaseId = doc.id,
                            productFirebaseId = doc.getString("productFirebaseId") ?: "",
                            productName = doc.getString("productName") ?: "",
                            category = doc.getString("category") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            reason = doc.getString("reason") ?: "",
                            wasteDate = doc.getString("wasteDate") ?: "",
                            recordedBy = doc.getString("recordedBy") ?: "",
                            isSyncedToFirebase = true
                        )
                    } catch (e: Exception) {
                        Log.e("WasteLogRepo", "⚠️ Failed to parse waste log: ${e.message}")
                        null
                    }
                }

                // Insert into local database
                if (wasteLogs.isNotEmpty()) {
                    dao.insertWasteLogs(wasteLogs)
                    Log.d("WasteLogRepo", "✅ Synced ${wasteLogs.size} waste logs to local DB")
                }else{}

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ Firebase sync failed: ${e.message}")
                // Continue with local data
            }
        }
    }

    // ============ QUERY METHODS ============

    suspend fun getAllWasteLogs(): List<Entity_WasteLog> = dao.getAllWasteLogs()

    suspend fun getWasteLogsByProduct(productId: String): List<Entity_WasteLog> =
        dao.getWasteLogsByProduct(productId)

    suspend fun getWasteLogsByDateRange(startDate: String, endDate: String): List<Entity_WasteLog> =
        dao.getWasteLogsByDateRange(startDate, endDate)

    suspend fun getWasteLogsByUser(username: String): List<Entity_WasteLog> =
        dao.getWasteLogsByUser(username)

    suspend fun getTotalWasteForProduct(productId: String): Int =
        dao.getTotalWasteForProduct(productId) ?: 0

    suspend fun getTotalWasteByDateRange(startDate: String, endDate: String): Int =
        dao.getTotalWasteByDateRange(startDate, endDate) ?: 0

    suspend fun clearAllWasteLogs() = dao.clearAllWasteLogs()
}
