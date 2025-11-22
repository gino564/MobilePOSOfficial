package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ONE-TIME DATABASE CLEANUP AND SETUP SCRIPT
 * Combines database cleanup, recipe setup, and ingredient cost calculations
 *
 * HOW TO USE:
 * 1. Call FirestoreSetup.runCompleteSetup() from InventoryListScreen (Build button)
 * 2. Check Logcat for success messages
 * 3. Data will be cleaned, recipes added, and costs calculated in Firestore
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
     * Add missing ingredient products to Firestore
     * Adds: Butter, Flour, Egg with proper structure
     */
    suspend fun addMissingIngredients(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "🥘 Adding missing ingredient products...")

            val productsSnapshot = firestore.collection("products").get().await()
            val existingProducts = productsSnapshot.documents

            // Define missing ingredients with their properties
            val missingIngredients = listOf(
                MissingIngredient(
                    name = "Flour",
                    price = 50.0,        // ₱50 per 1kg
                    quantity = 1000,     // 1000g = 1kg
                    costPerUnit = 0.05   // ₱0.05 per gram
                ),
                MissingIngredient(
                    name = "Butter",
                    price = 300.0,       // ₱300 per 1kg
                    quantity = 1000,     // 1000g = 1kg
                    costPerUnit = 0.30   // ₱0.30 per gram
                ),
                MissingIngredient(
                    name = "Egg",
                    price = 96.0,        // ₱96 per dozen (12 pcs)
                    quantity = 12,       // 12 pieces
                    costPerUnit = 8.0    // ₱8.00 per piece
                )
            )

            var addedCount = 0

            for (ingredient in missingIngredients) {
                // Check if ingredient already exists
                val exists = existingProducts.any {
                    it.getString("name")?.equals(ingredient.name, ignoreCase = true) == true
                }

                if (!exists) {
                    // Create ingredient product data
                    val productData = hashMapOf(
                        "name" to ingredient.name,
                        "category" to "Ingredients",
                        "price" to ingredient.price,
                        "quantity" to ingredient.quantity,
                        "inventoryA" to ingredient.quantity,
                        "inventoryB" to 0,
                        "costPerUnit" to ingredient.costPerUnit,
                        "imageUri" to ""
                    )

                    // Add to Firestore
                    val docRef = firestore.collection("products").add(productData).await()
                    addedCount++
                    Log.d("FirestoreSetup", "   ✅ Added ${ingredient.name}: ₱${String.format("%.2f", ingredient.price)} (${ingredient.quantity} units, ₱${String.format("%.2f", ingredient.costPerUnit)}/unit)")
                    Log.d("FirestoreSetup", "      Firebase ID: ${docRef.id}")
                } else {
                    Log.d("FirestoreSetup", "   ⏭️ ${ingredient.name} already exists, skipping...")
                }
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ Added $addedCount missing ingredient products")

            Result.success("Added $addedCount missing ingredients")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error adding missing ingredients: ${e.message}", e)
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
     * Main function to add all pastry recipes to Firestore
     * IMPORTANT: Run this ONCE only!
     */
    suspend fun addRecipesForPastries(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "🚀 Starting Firestore recipe setup...")

            // Get all products from Firestore
            val productsSnapshot = firestore.collection("products").get().await()
            val products = productsSnapshot.documents

            // Find pastry products
            val pastries = products.filter {
                it.getString("category")?.equals("Pastries", ignoreCase = true) == true
            }

            Log.d("FirestoreSetup", "📋 Found ${pastries.size} pastry products")

            if (pastries.isEmpty()) {
                return Result.failure(Exception("No pastries found in Firestore. Add pastry products first!"))
            }

            var recipeCount = 0
            var ingredientCount = 0

            // Add recipes for each pastry
            for (pastryDoc in pastries) {
                val pastryName = pastryDoc.getString("name") ?: continue
                val pastryId = pastryDoc.id

                Log.d("FirestoreSetup", "🍰 Adding recipe for: $pastryName")

                // Check if recipe already exists
                val existingRecipe = firestore.collection("recipes")
                    .whereEqualTo("productFirebaseId", pastryId)
                    .get()
                    .await()

                if (!existingRecipe.isEmpty) {
                    Log.d("FirestoreSetup", "   ⏭️ Recipe already exists, skipping...")
                    continue
                }

                // Create recipe document (NO productId!)
                val recipeData = hashMapOf(
                    "productFirebaseId" to pastryId,
                    "productName" to pastryName
                )

                val recipeRef = firestore.collection("recipes").add(recipeData).await()
                val recipeFirebaseId = recipeRef.id
                recipeCount++

                Log.d("FirestoreSetup", "   ✅ Recipe created with ID: $recipeFirebaseId")

                // Add ingredients based on pastry type
                val ingredients = getIngredientsForPastry(pastryName)

                for (ingredient in ingredients) {
                    // Find the ingredient product by name to get its firebaseId
                    val ingredientProduct = products.find {
                        it.getString("name")?.equals(ingredient.name, ignoreCase = true) == true
                    }

                    if (ingredientProduct == null) {
                        Log.w("FirestoreSetup", "      ⚠️ Ingredient product not found: ${ingredient.name}, skipping")
                        continue
                    }

                    val ingredientData = hashMapOf(
                        "recipeFirebaseId" to recipeFirebaseId,
                        "ingredientFirebaseId" to ingredientProduct.id,
                        "ingredientName" to ingredient.name,
                        "quantityNeeded" to ingredient.quantity,
                        "unit" to ingredient.unit
                    )

                    firestore.collection("recipe_ingredients").add(ingredientData).await()
                    ingredientCount++

                    Log.d("FirestoreSetup", "      ➕ Added: ${ingredient.quantity}${ingredient.unit} ${ingredient.name}")
                }
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ SETUP COMPLETE!")
            Log.d("FirestoreSetup", "   📋 Recipes added: $recipeCount")
            Log.d("FirestoreSetup", "   🥖 Ingredients added: $ingredientCount")

            Result.success("Successfully added $recipeCount recipes with $ingredientCount ingredients!")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error setting up recipes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Define ingredients for each pastry
     * CUSTOMIZE THIS based on your actual pastry products and available ingredients!
     */
    private fun getIngredientsForPastry(pastryName: String): List<IngredientData> {
        return when {
            pastryName.contains("Croissant", ignoreCase = true) -> listOf(
                IngredientData("Flour", 50.0, "g"),
                IngredientData("Butter", 25.0, "g"),
                IngredientData("Milk", 30.0, "ml"),
                IngredientData("Sugar", 10.0, "g")
            )

            pastryName.contains("Donut", ignoreCase = true) -> listOf(
                IngredientData("Flour", 60.0, "g"),
                IngredientData("Sugar", 20.0, "g"),
                IngredientData("Milk", 40.0, "ml"),
                IngredientData("Egg", 1.0, "pcs")
            )

            pastryName.contains("Muffin", ignoreCase = true) -> listOf(
                IngredientData("Flour", 70.0, "g"),
                IngredientData("Sugar", 30.0, "g"),
                IngredientData("Butter", 20.0, "g"),
                IngredientData("Milk", 50.0, "ml"),
                IngredientData("Egg", 1.0, "pcs")
            )

            pastryName.contains("Bread", ignoreCase = true) -> listOf(
                IngredientData("Flour", 80.0, "g"),
                IngredientData("Butter", 10.0, "g"),
                IngredientData("Sugar", 5.0, "g"),
                IngredientData("Milk", 30.0, "ml")
            )

            pastryName.contains("Cookie", ignoreCase = true) -> listOf(
                IngredientData("Flour", 40.0, "g"),
                IngredientData("Butter", 20.0, "g"),
                IngredientData("Sugar", 15.0, "g"),
                IngredientData("Egg", 0.5, "pcs")
            )

            pastryName.contains("Cake", ignoreCase = true) -> listOf(
                IngredientData("Flour", 100.0, "g"),
                IngredientData("Sugar", 40.0, "g"),
                IngredientData("Butter", 30.0, "g"),
                IngredientData("Milk", 60.0, "ml"),
                IngredientData("Egg", 2.0, "pcs")
            )

            // Default recipe if pastry name doesn't match
            else -> listOf(
                IngredientData("Flour", 50.0, "g"),
                IngredientData("Sugar", 20.0, "g"),
                IngredientData("Butter", 15.0, "g"),
                IngredientData("Milk", 30.0, "ml")
            )
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

            // Step 2: Add missing ingredient products
            Log.d("FirestoreSetup", "Step 2: Adding missing ingredient products...")
            val addIngredientsResult = addMissingIngredients()
            if (addIngredientsResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Add ingredients warning: ${addIngredientsResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 3: Fix recipe ingredients
            Log.d("FirestoreSetup", "Step 3: Fixing recipe ingredients...")
            val fixIngredientsResult = fixRecipeIngredients()
            if (fixIngredientsResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Fix ingredients warning: ${fixIngredientsResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 4: Transfer stock to quantity
            Log.d("FirestoreSetup", "Step 4: Transferring stock to quantity...")
            val transferResult = transferStockToQuantity()
            if (transferResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Transfer warning: ${transferResult.exceptionOrNull()?.message}")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 5: Fix cost per unit to realistic values
            Log.d("FirestoreSetup", "Step 5: Fixing cost per unit...")
            val costResult = fixCostPerUnit()
            if (costResult.isFailure) {
                return Result.failure(costResult.exceptionOrNull()!!)
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 6: Add recipes for pastries
            Log.d("FirestoreSetup", "Step 6: Adding recipes for pastries...")
            val recipeResult = addRecipesForPastries()
            if (recipeResult.isFailure) {
                Log.w("FirestoreSetup", "⚠️ Recipe setup warning: ${recipeResult.exceptionOrNull()?.message}")
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

    private data class IngredientData(
        val name: String,
        val quantity: Double,
        val unit: String
    )

    private data class MissingIngredient(
        val name: String,
        val price: Double,
        val quantity: Int,
        val costPerUnit: Double
    )
}
