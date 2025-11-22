package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeRepository(
    private val database: Database_Products
) {
    private val daoRecipe = database.daoRecipe()
    private val daoProducts = database.dao_products()
    private val firestore = FirebaseFirestore.getInstance()
    private val recipesCollection = firestore.collection("recipes")
    private val ingredientsCollection = firestore.collection("recipe_ingredients")
    private val productsCollection = firestore.collection("products")

    companion object {
        private const val TAG = "RecipeRepository"
    }

    // ============ SYNC FROM FIREBASE ============

    suspend fun syncRecipesFromFirebase(): Result<Unit> {
        return try {
            android.util.Log.d("RecipeRepo", "🔄 Starting recipe sync from Firebase...")

            // Step 1: Fetch recipes
            val recipesSnapshot = recipesCollection.get().await()
            val recipesList = recipesSnapshot.documents.mapNotNull { doc ->
                try {
                    Entity_Recipe(
                        firebaseId = doc.id,
                        productFirebaseId = doc.getString("productFirebaseId") ?: "",
                        productName = doc.getString("productName") ?: ""
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RecipeRepo", "❌ Error parsing recipe: ${e.message}")
                    null
                }
            }

            android.util.Log.d("RecipeRepo", "📋 Fetched ${recipesList.size} recipes")

            // Step 2: Insert recipes first to get their IDs
            daoRecipe.clearAllRecipes()
            if (recipesList.isNotEmpty()) {
                daoRecipe.insertAllRecipes(recipesList)

                // ✅ ADD THIS DEBUG BLOCK
                android.util.Log.d("RecipeRepo", "")
                android.util.Log.d("RecipeRepo", "✅ Inserted recipes into Room. Verifying...")
                val verifyRecipes = daoRecipe.getAllRecipes()
                android.util.Log.d("RecipeRepo", "📋 Recipes in Room after insert: ${verifyRecipes.size}")
                verifyRecipes.forEach { r ->
                    android.util.Log.d("RecipeRepo", "  - ${r.productName}")
                    android.util.Log.d("RecipeRepo", "    recipeId: ${r.recipeId}")
                    android.util.Log.d("RecipeRepo", "    firebaseId: ${r.firebaseId}")
                    android.util.Log.d("RecipeRepo", "    productFirebaseId: ${r.productFirebaseId}")
                }
            }


            // Step 3: Fetch recipe ingredients from Firebase
            val ingredientsSnapshot = ingredientsCollection.get().await()
            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "📦 Found ${ingredientsSnapshot.documents.size} ingredient documents in Firebase")

            val ingredientsList = ingredientsSnapshot.documents.mapNotNull { doc ->
                try {
                    val recipeFirebaseId = doc.getString("recipeFirebaseId") ?: ""
                    val ingredientFirebaseId = doc.getString("ingredientFirebaseId") ?: ""
                    val ingredientName = doc.getString("ingredientName") ?: ""

                    android.util.Log.d("RecipeRepo", "🔍 Processing ingredient: $ingredientName")
                    android.util.Log.d("RecipeRepo", "   ingredientFirebaseId: $ingredientFirebaseId")

                    val recipe = daoRecipe.getRecipeByFirebaseId(recipeFirebaseId)
                    if (recipe == null) {
                        android.util.Log.e("RecipeRepo", "   ❌ Recipe NOT found")
                        return@mapNotNull null
                    }

                    Entity_RecipeIngredient(
                        firebaseId = doc.id,
                        recipeId = recipe.recipeId,
                        ingredientFirebaseId = ingredientFirebaseId,
                        ingredientName = ingredientName,
                        quantityNeeded = (doc.getLong("quantityNeeded") ?: 0).toDouble(),
                        unit = doc.getString("unit") ?: "g"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RecipeRepo", "❌ Error parsing ingredient: ${e.message}", e)
                    null
                }
            }

            android.util.Log.d("RecipeRepo", "📋 Fetched ${ingredientsList.size} ingredients")

            // Insert ingredients
            daoRecipe.clearAllIngredients()
            if (ingredientsList.isNotEmpty()) {
                daoRecipe.insertAllIngredients(ingredientsList)

                // Verify insertion
                android.util.Log.d("RecipeRepo", "")
                android.util.Log.d("RecipeRepo", "✅ Verifying ingredient insertion...")
                val allIngredientsInRoom = daoRecipe.getAllRecipes().flatMap { recipe ->
                    val ings = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)
                    android.util.Log.d("RecipeRepo", "   Recipe '${recipe.productName}' has ${ings.size} ingredients")
                    ings
                }
                android.util.Log.d("RecipeRepo", "   Total ingredients in Room: ${allIngredientsInRoom.size}")
            } else {
                android.util.Log.w("RecipeRepo", "⚠️ No ingredients to insert!")
            }


            android.util.Log.d("RecipeRepo", "✅ Recipe sync completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e("RecipeRepo", "❌ Recipe sync failed: ${e.message}", e)
            Result.failure(e)
        }

    }

    // ============ READ OPERATIONS ============

    suspend fun getAllRecipesWithIngredients(): List<RecipeWithIngredients> {
        return try {
            daoRecipe.getAllRecipesWithIngredients()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRecipeForProduct(productId: Int): RecipeWithIngredients? {
        return try {
            daoRecipe.getRecipeWithIngredients(productId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe for product $productId: ${e.message}", e)
            null
        }
    }

    // ============ CALCULATE AVAILABLE QUANTITY ============

    suspend fun calculateMaxServings(productFirebaseId: String): Int {
        return try {
            android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("RecipeRepo", "🧮 Calculating max servings...")
            android.util.Log.d("RecipeRepo", "Product Firebase ID: $productFirebaseId")

            // DEBUG: Check all recipes in database
            val allRecipes = daoRecipe.getAllRecipes()
            android.util.Log.d("RecipeRepo", "📋 Total recipes in Room: ${allRecipes.size}")
            allRecipes.forEach { r ->
                android.util.Log.d("RecipeRepo", "  Recipe: ${r.productName}")
                android.util.Log.d("RecipeRepo", "    - recipeId: ${r.recipeId}")
                android.util.Log.d("RecipeRepo", "    - firebaseId: ${r.firebaseId}")
                android.util.Log.d("RecipeRepo", "    - productFirebaseId: ${r.productFirebaseId}")
            }

            // Step 1: Find the recipe for this product
            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "🔍 Looking for recipe with productFirebaseId = $productFirebaseId")
            val recipe = daoRecipe.getRecipeByProductFirebaseId(productFirebaseId)

            if (recipe == null) {
                android.util.Log.w("RecipeRepo", "❌ NO RECIPE FOUND!")
                android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            android.util.Log.d("RecipeRepo", "✅ Recipe found: ${recipe.productName}")
            android.util.Log.d("RecipeRepo", "   Recipe ID: ${recipe.recipeId}")

            // Step 2: Get all ingredients for this recipe
            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)

            if (ingredients.isEmpty()) {
                android.util.Log.w("RecipeRepo", "❌ No ingredients found for this recipe!")
                android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            android.util.Log.d("RecipeRepo", "📦 Found ${ingredients.size} ingredients:")

            // Step 3: Calculate max servings for each ingredient
            val maxServingsPerIngredient = ingredients.map { ingredient ->
                android.util.Log.d("RecipeRepo", "")
                android.util.Log.d("RecipeRepo", "  🔍 Checking ingredient: ${ingredient.ingredientName}")
                android.util.Log.d("RecipeRepo", "     ingredientProductId: ${ingredient.ingredientFirebaseId}")

                // ✅ Get the product by firebaseId instead of id
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (ingredientProduct == null) {
                    android.util.Log.e("RecipeRepo", "     ❌ Product not found!")
                    return@map 0
                }

                val available = ingredientProduct.quantity.toDouble()
                val needed = ingredient.quantityNeeded
                val maxServings = if (needed > 0) (available / needed).toInt() else 0

                android.util.Log.d("RecipeRepo", "     ✅ Product found: ${ingredientProduct.name}")
                android.util.Log.d("RecipeRepo", "     Available: $available ${ingredient.unit}")
                android.util.Log.d("RecipeRepo", "     Needed per serving: $needed ${ingredient.unit}")
                android.util.Log.d("RecipeRepo", "     Max servings: $maxServings")

                maxServings
            }

            // Step 4: Return the minimum (bottleneck ingredient)
            val result = maxServingsPerIngredient.minOrNull() ?: 0

            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "🎯 RESULT: Can make $result servings")
            android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

            result

        } catch (e: Exception) {
            android.util.Log.e("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.e("RecipeRepo", "❌ Error calculating servings!")
            android.util.Log.e("RecipeRepo", "Error: ${e.message}", e)
            android.util.Log.e("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            0
        }
    }


    // ============ DEDUCT INGREDIENTS ON ORDER ============

    suspend fun deductIngredients(productFirebaseId: String, quantity: Int, saveToSales: (Entity_SalesReport) -> Unit) {
        try {
            android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("RecipeRepo", "🔻 Deducting ingredients for $quantity servings")
            android.util.Log.d("RecipeRepo", "Product Firebase ID: $productFirebaseId")

            // Get the recipe for this product
            val recipe = daoRecipe.getRecipeByProductFirebaseId(productFirebaseId)

            if (recipe == null) {
                android.util.Log.w("RecipeRepo", "⚠️ No recipe found, cannot deduct ingredients")
                android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            }

            android.util.Log.d("RecipeRepo", "📋 Recipe found: ${recipe.productName}")

            // Get all ingredients for this recipe
            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)

            if (ingredients.isEmpty()) {
                android.util.Log.w("RecipeRepo", "⚠️ No ingredients found for this recipe")
                android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            }

            android.util.Log.d("RecipeRepo", "📦 Deducting ${ingredients.size} ingredients:")

            // Get current timestamp for all ingredient sales
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            // Deduct each ingredient using dual inventory (B first, then A)
            ingredients.forEach { ingredient ->
                val product = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (product != null) {
                    val amountToDeduct = (ingredient.quantityNeeded * quantity).toInt()

                    android.util.Log.d("RecipeRepo", "  📉 ${ingredient.ingredientName}:")
                    android.util.Log.d("RecipeRepo", "     Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")
                    android.util.Log.d("RecipeRepo", "     Deducting: $amountToDeduct ${ingredient.unit}")

                    var remainingToDeduct = amountToDeduct
                    var newInventoryA = product.inventoryA
                    var newInventoryB = product.inventoryB

                    // Step 1: Deduct from Inventory B first
                    if (newInventoryB > 0) {
                        val deductFromB = minOf(remainingToDeduct, newInventoryB)
                        newInventoryB -= deductFromB
                        remainingToDeduct -= deductFromB
                        android.util.Log.d("RecipeRepo", "     Deducted $deductFromB from Inventory B")
                    }

                    // Step 2: If still need more, deduct from Inventory A
                    if (remainingToDeduct > 0 && newInventoryA > 0) {
                        val deductFromA = minOf(remainingToDeduct, newInventoryA)
                        newInventoryA -= deductFromA
                        remainingToDeduct -= deductFromA
                        android.util.Log.d("RecipeRepo", "     Deducted $deductFromA from Inventory A")
                    }

                    val newQuantity = newInventoryA + newInventoryB

                    android.util.Log.d("RecipeRepo", "     After - Inventory A: $newInventoryA, Inventory B: $newInventoryB, Total: $newQuantity")

                    if (remainingToDeduct > 0) {
                        android.util.Log.w("RecipeRepo", "     ⚠️ Warning: Could not deduct full amount. Remaining: $remainingToDeduct")
                    }

                    // Update local Room database
                    val updatedProduct = product.copy(
                        quantity = newQuantity,
                        inventoryA = newInventoryA,
                        inventoryB = newInventoryB
                    )
                    daoProducts.updateProduct(updatedProduct)

                    // Update Firebase (batch update to minimize writes)
                    productsCollection.document(product.firebaseId)
                        .update(
                            mapOf(
                                "quantity" to newQuantity,
                                "inventoryA" to newInventoryA,
                                "inventoryB" to newInventoryB
                            )
                        )
                        .await()

                    android.util.Log.d("RecipeRepo", "     ✅ Updated in Room and Firebase")

                    // ✅ Save ingredient deduction to sales table
                    val ingredientSale = Entity_SalesReport(
                        productName = product.name,
                        category = product.category,  // ✅ This will be "ingredient"
                        quantity = amountToDeduct,  // ✅ Actual amount deducted
                        price = 0.0,  // Ingredients don't have individual sale price (already in beverage price)
                        orderDate = currentDate,
                        productFirebaseId = product.firebaseId
                    )

                    // Save to sales via callback
                    saveToSales(ingredientSale)
                    android.util.Log.d("RecipeRepo", "     💰 Ingredient sale recorded: $amountToDeduct ${ingredient.unit}")

                } else {
                    android.util.Log.w("RecipeRepo", "  ⚠️ Product not found for ingredient: ${ingredient.ingredientName}")
                }
            }

            android.util.Log.d("RecipeRepo", "✅ All ingredients deducted successfully")
            android.util.Log.d("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            android.util.Log.e("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.e("RecipeRepo", "❌ Error deducting ingredients!")
            android.util.Log.e("RecipeRepo", "Error: ${e.message}", e)
            android.util.Log.e("RecipeRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    // ============ CALCULATE RECIPE COST ============

    data class IngredientCostInfo(
        val ingredientName: String,
        val quantityNeeded: Double,
        val unit: String,
        val costPerUnit: Double,  // Price per unit (e.g., ₱0.50/g)
        val totalCost: Double     // Cost for this ingredient in recipe
    )

    data class RecipeCostSummary(
        val ingredientCosts: List<IngredientCostInfo>,
        val totalCost: Double,
        val sellingPrice: Double,
        val profitMargin: Double,
        val profitPercentage: Double
    )

    suspend fun calculateRecipeCost(productFirebaseId: String): RecipeCostSummary? {
        return try {
            android.util.Log.d("RecipeRepo", "💰 Calculating recipe cost for: $productFirebaseId")

            // Get the recipe
            val recipe = daoRecipe.getRecipeByProductFirebaseId(productFirebaseId)
            if (recipe == null) {
                android.util.Log.w("RecipeRepo", "⚠️ No recipe found")
                return null
            }

            // Get product selling price
            val product = daoProducts.getProductByFirebaseId(productFirebaseId)
            val sellingPrice = product?.price ?: 0.0

            // Get all ingredients
            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)
            if (ingredients.isEmpty()) {
                android.util.Log.w("RecipeRepo", "⚠️ No ingredients found")
                return null
            }

            // Calculate cost for each ingredient
            val ingredientCosts = ingredients.mapNotNull { ingredient ->
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (ingredientProduct != null) {
                    // Assume ingredient price is cost per unit in stock
                    // E.g., if 1kg (1000g) costs ₱500, then price = 500, quantity = 1000g
                    // Cost per gram = ₱500 / 1000g = ₱0.50/g
                    val totalStockQuantity = ingredientProduct.quantity.toDouble()
                    val costPerUnit = if (totalStockQuantity > 0) {
                        ingredientProduct.price / totalStockQuantity
                    } else {
                        0.0
                    }

                    val totalCost = ingredient.quantityNeeded * costPerUnit

                    IngredientCostInfo(
                        ingredientName = ingredient.ingredientName,
                        quantityNeeded = ingredient.quantityNeeded,
                        unit = ingredient.unit,
                        costPerUnit = costPerUnit,
                        totalCost = totalCost
                    )
                } else {
                    null
                }
            }

            val totalCost = ingredientCosts.sumOf { it.totalCost }
            val profitMargin = sellingPrice - totalCost
            val profitPercentage = if (totalCost > 0) {
                (profitMargin / totalCost) * 100
            } else {
                0.0
            }

            android.util.Log.d("RecipeRepo", "✅ Total Cost: ₱${"%.2f".format(totalCost)}")
            android.util.Log.d("RecipeRepo", "   Selling Price: ₱${"%.2f".format(sellingPrice)}")
            android.util.Log.d("RecipeRepo", "   Profit: ₱${"%.2f".format(profitMargin)} (${"%.1f".format(profitPercentage)}%)")

            RecipeCostSummary(
                ingredientCosts = ingredientCosts,
                totalCost = totalCost,
                sellingPrice = sellingPrice,
                profitMargin = profitMargin,
                profitPercentage = profitPercentage
            )

        } catch (e: Exception) {
            android.util.Log.e("RecipeRepo", "❌ Error calculating recipe cost: ${e.message}", e)
            null
        }
    }
}