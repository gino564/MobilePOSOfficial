package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ONE-TIME DATABASE CLEANUP AND SETUP SCRIPT
 * Run this once to fix database schema and add missing data
 *
 * HOW TO USE:
 * 1. Call FirestoreSetup.runCompleteSetup() from InventoryListScreen (Build button)
 * 2. Check Logcat for success messages
 * 3. Data will be cleaned and fixed in Firestore
 */
object FirestoreSetup {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Clean up unused fields from recipes table
     * Removes: product_id, product_number (not being used)
     */
    suspend fun cleanupRecipeFields(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "🧹 Cleaning up recipes table...")

            val recipesSnapshot = firestore.collection("recipes").get().await()
            var cleanedCount = 0

            for (recipeDoc in recipesSnapshot.documents) {
                val updates = mutableMapOf<String, Any?>()

                // Remove product_id and product_number if they exist
                if (recipeDoc.contains("product_id")) {
                    updates["product_id"] = com.google.firebase.firestore.FieldValue.delete()
                }
                if (recipeDoc.contains("product_number")) {
                    updates["product_number"] = com.google.firebase.firestore.FieldValue.delete()
                }

                if (updates.isNotEmpty()) {
                    firestore.collection("recipes")
                        .document(recipeDoc.id)
                        .update(updates)
                        .await()
                    cleanedCount++
                }
            }

            Log.d("FirestoreSetup", "✅ Cleaned $cleanedCount recipes")
            Result.success("Cleaned up $cleanedCount recipes")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error cleaning recipes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fix recipe ingredients - add missing ingredientFirebaseId and remove ingredient_product_id
     */
    suspend fun fixRecipeIngredients(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "🔧 Fixing recipe ingredients...")

            val productsSnapshot = firestore.collection("products").get().await()
            val ingredientsSnapshot = firestore.collection("recipe_ingredients").get().await()

            var fixedCount = 0
            var addedFirebaseIdCount = 0

            for (ingredientDoc in ingredientsSnapshot.documents) {
                val updates = mutableMapOf<String, Any?>()

                // Remove ingredient_product_id if it exists
                if (ingredientDoc.contains("ingredient_product_id")) {
                    updates["ingredient_product_id"] = com.google.firebase.firestore.FieldValue.delete()
                }

                // Add ingredientFirebaseId if missing
                val ingredientFirebaseId = ingredientDoc.getString("ingredientFirebaseId")
                val ingredientName = ingredientDoc.getString("ingredientName")

                if (ingredientFirebaseId.isNullOrEmpty() && !ingredientName.isNullOrEmpty()) {
                    // Find the ingredient product by name
                    val ingredientProduct = productsSnapshot.documents.find {
                        it.getString("name")?.equals(ingredientName, ignoreCase = true) == true
                    }

                    if (ingredientProduct != null) {
                        updates["ingredientFirebaseId"] = ingredientProduct.id
                        addedFirebaseIdCount++
                        Log.d("FirestoreSetup", "   ✅ Added firebaseId for: $ingredientName -> ${ingredientProduct.id}")
                    } else {
                        Log.w("FirestoreSetup", "   ⚠️ Could not find product for ingredient: $ingredientName")
                    }
                }

                if (updates.isNotEmpty()) {
                    firestore.collection("recipe_ingredients")
                        .document(ingredientDoc.id)
                        .update(updates)
                        .await()
                    fixedCount++
                }
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ Fixed $fixedCount recipe ingredients")
            Log.d("FirestoreSetup", "   Added firebaseId to $addedFirebaseIdCount ingredients")

            Result.success("Fixed $fixedCount ingredients, added $addedFirebaseIdCount firebaseIds")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error fixing recipe ingredients: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Transfer stock field to quantity field in products table
     */
    suspend fun transferStockToQuantity(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "📦 Transferring stock to quantity...")

            val productsSnapshot = firestore.collection("products").get().await()
            var transferredCount = 0

            for (productDoc in productsSnapshot.documents) {
                val stock = productDoc.getLong("stock")
                val currentQuantity = productDoc.getLong("quantity") ?: 0

                // If stock field exists and quantity is 0 (for beverages/pastries)
                if (stock != null && currentQuantity == 0L) {
                    val updates = mapOf(
                        "quantity" to stock,
                        "stock" to com.google.firebase.firestore.FieldValue.delete()
                    )

                    firestore.collection("products")
                        .document(productDoc.id)
                        .update(updates)
                        .await()

                    transferredCount++
                    val name = productDoc.getString("name") ?: "Unknown"
                    Log.d("FirestoreSetup", "   ✅ $name: transferred stock $stock to quantity")
                }
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ Transferred stock to quantity for $transferredCount products")

            Result.success("Transferred stock for $transferredCount products")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error transferring stock: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fix cost per unit to be realistic per unit prices
     * Uses actual unit prices (e.g., per gram, per ml)
     */
    suspend fun fixCostPerUnit(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "💰 Fixing cost per unit for ingredients...")

            val productsSnapshot = firestore.collection("products").get().await()
            val ingredients = productsSnapshot.documents.filter {
                it.getString("category")?.equals("Ingredients", ignoreCase = true) == true
            }

            Log.d("FirestoreSetup", "📋 Found ${ingredients.size} ingredient products")

            var updateCount = 0
            val costPerUnitMap = mapOf(
                // Format: "ProductName" to cost per unit
                "Flour" to 0.05,           // ₱0.05 per gram (₱50 per 1kg)
                "Sugar" to 0.04,           // ₱0.04 per gram (₱40 per 1kg)
                "Butter" to 0.30,          // ₱0.30 per gram (₱300 per 1kg)
                "Milk" to 0.06,            // ₱0.06 per ml (₱60 per 1L)
                "Egg" to 8.0,              // ₱8.00 per piece
                "Coffee Beans" to 0.50,    // ₱0.50 per gram (₱500 per 1kg)
                "Cocoa Powder" to 0.40,    // ₱0.40 per gram (₱400 per 1kg)
                "Vanilla Extract" to 2.0,  // ₱2.00 per ml
                "Salt" to 0.02,            // ₱0.02 per gram (₱20 per 1kg)
                "Baking Powder" to 0.15,   // ₱0.15 per gram
                "Yeast" to 0.20,           // ₱0.20 per gram
                "Chocolate Chips" to 0.35, // ₱0.35 per gram
                "Cream" to 0.25            // ₱0.25 per ml
            )

            for (ingredientDoc in ingredients) {
                val name = ingredientDoc.getString("name") ?: continue

                // Find matching cost per unit (case-insensitive partial match)
                val costPerUnit = costPerUnitMap.entries.find {
                    name.contains(it.key, ignoreCase = true)
                }?.value ?: run {
                    // Default calculation if not in map
                    val price = ingredientDoc.getDouble("price") ?: 0.0
                    val quantity = ingredientDoc.getLong("quantity")?.toDouble() ?: 1.0
                    if (quantity > 0) price / quantity else 0.05
                }

                // Update document with costPerUnit field
                firestore.collection("products")
                    .document(ingredientDoc.id)
                    .update("costPerUnit", costPerUnit)
                    .await()

                updateCount++
                Log.d("FirestoreSetup", "   ✅ $name: ₱${String.format("%.2f", costPerUnit)} per unit")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ Updated $updateCount ingredients with realistic cost per unit")

            Result.success("Updated cost per unit for $updateCount ingredients!")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error fixing cost per unit: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function to run all setup tasks at once
     */
    suspend fun runCompleteSetup(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "========================================")
            Log.d("FirestoreSetup", "🚀 STARTING COMPLETE FIRESTORE SETUP")
            Log.d("FirestoreSetup", "========================================")
            Log.d("FirestoreSetup", "")

            // Step 1: Clean up recipe fields
            Log.d("FirestoreSetup", "Step 1: Cleaning up recipes...")
            val cleanupResult = cleanupRecipeFields()
            if (cleanupResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Cleanup warning: ${cleanupResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 2: Fix recipe ingredients
            Log.d("FirestoreSetup", "Step 2: Fixing recipe ingredients...")
            val fixIngredientsResult = fixRecipeIngredients()
            if (fixIngredientsResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Fix ingredients warning: ${fixIngredientsResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 3: Transfer stock to quantity
            Log.d("FirestoreSetup", "Step 3: Transferring stock to quantity...")
            val transferResult = transferStockToQuantity()
            if (transferResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Transfer warning: ${transferResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 4: Fix cost per unit to realistic values
            Log.d("FirestoreSetup", "Step 4: Fixing cost per unit...")
            val costResult = fixCostPerUnit()
            if (costResult.isFailure) {
                return Result.failure(costResult.exceptionOrNull()!!)
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "========================================")
            Log.d("FirestoreSetup", "✅ COMPLETE SETUP FINISHED!")
            Log.d("FirestoreSetup", "========================================")

            Result.success("Database cleanup and setup completed successfully!")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Setup failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
