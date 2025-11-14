package com.project.dba_delatorre_dometita_ramirez_tan

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UserRepository(
    private val daoUsers: Dao_Users
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UserRepository"
    }

    // ============ SYNC USERS FROM FIREBASE ============

    suspend fun syncUsersFromFirebase(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d(TAG, "📡 Syncing users from Firestore...")

                val snapshot = usersCollection.get().await()
                android.util.Log.d(TAG, "✅ Firestore returned ${snapshot.documents.size} users")

                val usersList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val joinedDateStr = try {
                            val timestamp = doc.getTimestamp("joinedDate")
                            timestamp?.toDate()?.let {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
                            } ?: ""
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "⚠️ Could not parse joinedDate for ${doc.id}")
                            ""
                        }

                        Entity_Users(
                            Entity_id = 0,
                            Entity_lname = doc.getString("lname") ?: "",
                            Entity_fname = doc.getString("fname") ?: "",
                            Entity_mname = doc.getString("mname") ?: "",
                            Entity_username = doc.getString("username") ?: "",
                            role = doc.getString("role") ?: "Staff",
                            status = doc.getString("status") ?: "active",
                            joinedDate = joinedDateStr
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "❌ Error parsing user ${doc.id}: ${e.message}")
                        null
                    }
                }

                android.util.Log.d(TAG, "✅ Parsed ${usersList.size} users from Firestore")

                if (usersList.isNotEmpty()) {
                    usersList.forEach { user ->
                        val existingUser = daoUsers.getUserByUsername(user.Entity_username)
                        if (existingUser != null) {
                            val updatedUser = user.copy(Entity_id = existingUser.Entity_id)
                            daoUsers.DaoUpdate(updatedUser)
                            android.util.Log.d(TAG, "♻️ Updated user: ${user.Entity_username} (${user.role})")
                        } else {
                            daoUsers.DaoInsert(user)
                            android.util.Log.d(TAG, "➕ Added new user: ${user.Entity_username} (${user.role})")
                        }
                    }
                    android.util.Log.d(TAG, "✅ Synced to Room database")
                }

                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ User sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ============ LOGIN WITH FIREBASE AUTHENTICATION ============

    suspend fun loginUser(username: String, password: String): Entity_Users? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d(TAG, "🔐 Attempting login...")
                android.util.Log.d(TAG, "Username: $username")

                // Step 1: Sync users from Firebase first
                syncUsersFromFirebase()

                // Step 2: Find user in Firestore by username to get authEmail
                android.util.Log.d(TAG, "🔍 Looking up user in Firestore...")
                val querySnapshot = usersCollection
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    android.util.Log.w(TAG, "❌ User not found in Firestore")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext null
                }

                val userDoc = querySnapshot.documents.first()
                val authEmail = userDoc.getString("authEmail")
                val status = userDoc.getString("status")

                if (authEmail == null) {
                    android.util.Log.w(TAG, "❌ User has no authEmail field")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext null
                }

                if (status != "active") {
                    android.util.Log.w(TAG, "❌ User is not active (status: $status)")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext null
                }

                android.util.Log.d(TAG, "✅ Found user in Firestore")
                android.util.Log.d(TAG, "🔑 Auth Email: $authEmail")

                // Step 3: Get user from Room database
                val user = daoUsers.getActiveUserByUsername(username)

                if (user == null) {
                    android.util.Log.w(TAG, "❌ User not found in local database")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext null
                }

                // Step 4: Login with Firebase Authentication using authEmail
                try {
                    android.util.Log.d(TAG, "🔐 Attempting Firebase Auth with: $authEmail")
                    val authResult = auth.signInWithEmailAndPassword(authEmail, password).await()

                    android.util.Log.d(TAG, "✅ Firebase Auth successful")
                    android.util.Log.d(TAG, "   UID: ${authResult.user?.uid}")
                    android.util.Log.d(TAG, "✅ Login successful: ${user.Entity_fname} ${user.Entity_lname}")
                    android.util.Log.d(TAG, "   Role: ${user.role}")
                    android.util.Log.d(TAG, "   Status: ${user.status}")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    user
                } catch (authError: Exception) {
                    android.util.Log.e(TAG, "❌ Firebase Auth failed: ${authError.message}")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    null
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Login failed: ${e.message}", e)
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                null
            }
        }
    }

    // ============ LOGOUT ============

    fun logout() {
        try {
            auth.signOut()
            android.util.Log.d(TAG, "✅ Logged out from Firebase Auth")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Logout error: ${e.message}")
        }
    }
}
