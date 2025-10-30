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
                        productId = (doc.getLong("productId") ?: 0).toInt(),
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
                    // Get the recipeFirebaseId from Firebase
                    val recipeFirebaseId = doc.getString("recipeFirebaseId") ?: ""
                    val ingredientName = doc.getString("ingredientName") ?: ""

                    android.util.Log.d("RecipeRepo", "")
                    android.util.Log.d("RecipeRepo", "🔍 Processing ingredient: $ingredientName")
                    android.util.Log.d("RecipeRepo", "   Document ID: ${doc.id}")
                    android.util.Log.d("RecipeRepo", "   recipeFirebaseId: $recipeFirebaseId")

                    // ✅ FIX: Find recipe by its own firebaseId
                    val recipe = daoRecipe.getRecipeByFirebaseId(recipeFirebaseId)

                    if (recipe == null) {
                        android.util.Log.e("RecipeRepo", "   ❌ Recipe NOT found with firebaseId: $recipeFirebaseId")
                        android.util.Log.e("RecipeRepo", "   Available recipes in Room:")
                        daoRecipe.getAllRecipes().forEach { r ->
                            android.util.Log.e("RecipeRepo", "     - ${r.productName} (firebaseId: '${r.firebaseId}')")
                        }
                        return@mapNotNull null
                    }

                    android.util.Log.d("RecipeRepo", "   ✅ Recipe found: ${recipe.productName} (recipeId: ${recipe.recipeId})")

                    // Map Firebase fields to your local structure
                    val ingredient = Entity_RecipeIngredient(
                        firebaseId = doc.id,
                        recipeId = recipe.recipeId,
                        ingredientProductId = "",  // Will be updated below
                        ingredientName = ingredientName,
                        quantityNeeded = (doc.getLong("quantityNeeded") ?: 0).toDouble(),
                        unit = doc.getString("unit") ?: "g"
                    )

                    android.util.Log.d("RecipeRepo", "   ✅ Ingredient mapped to recipeId: ${recipe.recipeId}")
                    ingredient

                } catch (e: Exception) {
                    android.util.Log.e("RecipeRepo", "❌ Error parsing ingredient: ${e.message}", e)
                    null
                }
            }

            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "📋 Successfully fetched ${ingredientsList.size} ingredients")

// Step 4: Update ingredientProductId by finding products
            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "🔗 Linking ingredients to products...")

            val updatedIngredientsList = ingredientsList.mapNotNull { ingredient ->
                android.util.Log.d("RecipeRepo", "")
                android.util.Log.d("RecipeRepo", "🔍 Looking for product: '${ingredient.ingredientName}'")

                // Find the product by name
                val allProducts = daoProducts.getAllProducts()
                android.util.Log.d("RecipeRepo", "   Searching among ${allProducts.size} products...")

                val product = allProducts.find {
                    it.name.trim().equals(ingredient.ingredientName.trim(), ignoreCase = true)
                }

                if (product != null) {
                    android.util.Log.d("RecipeRepo", "   ✅ Product found: '${product.name}'")
                    android.util.Log.d("RecipeRepo", "      Product Firebase ID: ${product.firebaseId}")  // ✅ Changed
                    android.util.Log.d("RecipeRepo", "      Stock: ${product.quantity}")
                    ingredient.copy(ingredientProductId = product.firebaseId)  // ✅ Use firebaseId instead of id
                } else {
                    android.util.Log.e("RecipeRepo", "   ❌ Product NOT found!")
                    android.util.Log.e("RecipeRepo", "   Available products:")
                    allProducts.forEach { p ->
                        android.util.Log.e("RecipeRepo", "     - '${p.name}' (category: ${p.category})")
                    }
                    null
                }
            }

            android.util.Log.d("RecipeRepo", "")
            android.util.Log.d("RecipeRepo", "📦 Successfully linked ${updatedIngredientsList.size} ingredients")

// Step 5: Insert ingredients
            daoRecipe.clearAllIngredients()
            if (updatedIngredientsList.isNotEmpty()) {
                android.util.Log.d("RecipeRepo", "💾 Inserting ${updatedIngredientsList.size} ingredients into Room...")

                updatedIngredientsList.forEach { ing ->
                    android.util.Log.d("RecipeRepo", "   - ${ing.ingredientName} (recipeId: ${ing.recipeId}, productId: ${ing.ingredientProductId})")
                }

                daoRecipe.insertAllIngredients(updatedIngredientsList)

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
                android.util.Log.d("RecipeRepo", "     ingredientProductId: ${ingredient.ingredientProductId}")

                // ✅ Get the product by firebaseId instead of id
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientProductId)

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

            // Deduct each ingredient
            ingredients.forEach { ingredient ->
                // ✅ Get product by firebaseId
                val product = daoProducts.getProductByFirebaseId(ingredient.ingredientProductId)

                if (product != null) {
                    val amountToDeduct = ingredient.quantityNeeded * quantity
                    val newQuantity = (product.quantity - amountToDeduct.toInt()).coerceAtLeast(0)

                    android.util.Log.d("RecipeRepo", "  📉 ${ingredient.ingredientName}:")
                    android.util.Log.d("RecipeRepo", "     Before: ${product.quantity} ${ingredient.unit}")
                    android.util.Log.d("RecipeRepo", "     Deducting: $amountToDeduct ${ingredient.unit}")
                    android.util.Log.d("RecipeRepo", "     After: $newQuantity ${ingredient.unit}")

                    // Update local Room database
                    val updatedProduct = product.copy(quantity = newQuantity)
                    daoProducts.updateProduct(updatedProduct)

                    // Update Firebase
                    productsCollection.document(product.firebaseId)
                        .update("quantity", newQuantity)
                        .await()

                    android.util.Log.d("RecipeRepo", "     ✅ Updated in Room and Firebase")

                    // ✅ Save ingredient deduction to sales table
                    val ingredientSale = Entity_SalesReport(
                        productName = product.name,
                        category = product.category,  // ✅ This will be "ingredient"
                        quantity = amountToDeduct.toInt(),  // ✅ Actual grams deducted!
                        price = 0.0,  // Ingredients don't have individual sale price (already in beverage price)
                        orderDate = currentDate
                    )

                    // Save to sales via callback
                    saveToSales(ingredientSale)
                    android.util.Log.d("RecipeRepo", "     💰 Ingredient sale recorded: ${amountToDeduct.toInt()}g")

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
}