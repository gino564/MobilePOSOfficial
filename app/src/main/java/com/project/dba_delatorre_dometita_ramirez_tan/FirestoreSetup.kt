package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ONE-TIME SETUP SCRIPT
 * Run this once to add recipes for pastries to Firestore
 *
 * HOW TO USE:
 * 1. Call FirestoreSetup.addRecipesForPastries() from MainActivity or any screen
 * 2. Check Logcat for success messages
 * 3. Recipes will be added to Firestore and synced to local database
 */
object FirestoreSetup {

    private val firestore = FirebaseFirestore.getInstance()

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
     * Add cost per unit to all ingredient products
     * This allows proper waste cost calculation
     */
    suspend fun addCostPerUnitToIngredients(): Result<String> {
        return try {
            Log.d("FirestoreSetup", "💰 Adding cost per unit to ingredients...")

            val productsSnapshot = firestore.collection("products").get().await()
            val ingredients = productsSnapshot.documents.filter {
                it.getString("category")?.equals("Ingredients", ignoreCase = true) == true
            }

            Log.d("FirestoreSetup", "📋 Found ${ingredients.size} ingredient products")

            var updateCount = 0

            for (ingredientDoc in ingredients) {
                val name = ingredientDoc.getString("name") ?: continue
                val price = ingredientDoc.getDouble("price") ?: 0.0
                val quantity = ingredientDoc.getLong("quantity")?.toDouble() ?: 1.0

                // Calculate cost per unit (e.g., if 1kg flour costs ₱200, cost per gram = 0.20)
                val costPerUnit = if (quantity > 0) price / quantity else price

                // Update document with costPerUnit field
                firestore.collection("products")
                    .document(ingredientDoc.id)
                    .update("costPerUnit", costPerUnit)
                    .await()

                updateCount++
                Log.d("FirestoreSetup", "   ✅ $name: ₱$price / $quantity = ₱${String.format("%.2f", costPerUnit)} per unit")
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "✅ Updated $updateCount ingredients with cost per unit")

            Result.success("Added cost per unit to $updateCount ingredients!")

        } catch (e: Exception) {
            Log.e("FirestoreSetup", "❌ Error adding cost per unit: ${e.message}", e)
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

            // Step 1: Add cost per unit to ingredients
            val costResult = addCostPerUnitToIngredients()
            if (costResult.isFailure) {
                return Result.failure(costResult.exceptionOrNull()!!)
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "----------------------------------------")
            Log.d("FirestoreSetup", "")

            // Step 2: Add recipes for pastries
            val recipeResult = addRecipesForPastries()
            if (recipeResult.isFailure) {
                return Result.failure(recipeResult.exceptionOrNull()!!)
            }

            Log.d("FirestoreSetup", "")
            Log.d("FirestoreSetup", "========================================")
            Log.d("FirestoreSetup", "✅ COMPLETE SETUP FINISHED!")
            Log.d("FirestoreSetup", "========================================")

            Result.success("Firestore setup completed successfully!")

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
}
