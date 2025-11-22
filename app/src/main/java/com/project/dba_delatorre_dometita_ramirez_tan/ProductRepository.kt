package com.project.dba_delatorre_dometita_ramirez_tan

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID


class ProductRepository(
    private val daoProducts: Dao_Products,
    private val daoSalesReport: Dao_SalesReport
) {
    // ✅ Firebase Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")
    private val storage = FirebaseStorage.getInstance()

    // ============ IMAGE UPLOAD ============

    private suspend fun uploadImageToCloudinary(imageUri: String): String {
        return try {
            if (imageUri.isEmpty()) {
                android.util.Log.w("ProductRepo", "⚠️ No image provided")
                return ""
            }

            android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("ProductRepo", "📤 Uploading to Cloudinary...")
            android.util.Log.d("ProductRepo", "Input URI: $imageUri")

            val uri = Uri.parse(imageUri)
            val downloadUrl = CloudinaryHelper.uploadImage(uri)

            android.util.Log.d("ProductRepo", "✅ Upload successful!")
            android.util.Log.d("ProductRepo", "🔗 Cloudinary URL: $downloadUrl")
            android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

            downloadUrl
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.e("ProductRepo", "❌ Cloudinary upload failed!")
            android.util.Log.e("ProductRepo", "Error: ${e.message}", e)
            android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            ""
        }
    }
    private suspend fun deleteImageFromCloudinary(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.isEmpty()) {
                    android.util.Log.d("ProductRepo", "⚠️ No image to delete")
                    return@withContext false
                }

                if (!imageUrl.contains("cloudinary.com")) {
                    android.util.Log.d("ProductRepo", "⚠️ Not a Cloudinary URL, skipping delete")
                    return@withContext false
                }

                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "🗑️ Deleting image from Cloudinary...")
                android.util.Log.d("ProductRepo", "Image URL: $imageUrl")

                val success = CloudinaryHelper.deleteImage(imageUrl)

                if (success) {
                    android.util.Log.d("ProductRepo", "✅ Image deleted successfully!")
                } else {
                    android.util.Log.w("ProductRepo", "⚠️ Image deletion failed or not found")
                }

                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

                success
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e("ProductRepo", "❌ Cloudinary delete failed!")
                android.util.Log.e("ProductRepo", "Error: ${e.message}", e)
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                false
            }
        }
    }

    // ============ INSERT WITH IMAGE UPLOAD ============

    suspend fun insert(product: Entity_Products) {
        try {  // ✅ Just 'try' - no 'return'
            android.util.Log.d("ProductRepo", "➕ Inserting product: ${product.name}")

            // Step 1: Upload image if exists
            val cloudinaryImageUrl = if (product.imageUri.isNotEmpty()) {
                uploadImageToCloudinary(product.imageUri)  // ✅ NEW
            } else {
                ""
            }

            // Step 2: Create product data with Firebase image URL (including dual inventory)
            val productData = hashMapOf(
                "name" to product.name,
                "category" to product.category,
                "price" to product.price,
                "quantity" to product.quantity,
                "inventoryA" to product.inventoryA,
                "inventoryB" to product.inventoryB,
                "imageUri" to cloudinaryImageUrl
            )

            // Step 3: Add to Firestore
            val docRef = productsCollection.add(productData).await()
            android.util.Log.d("ProductRepo", "✅ Product added to Firestore with ID: ${docRef.id}")

            // Step 4: Save to Room with Firebase ID
            val productWithFirebaseId = product.copy(
                firebaseId = docRef.id,
                imageUri = cloudinaryImageUrl
            )
            daoProducts.insertProduct(productWithFirebaseId)

            android.util.Log.d("ProductRepo", "✅ Product synced to Room")
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Insert failed: ${e.message}", e)
            throw e
        }
    }

    // ============ FETCH FROM FIREBASE ============

    suspend fun getAll(): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "📡 Fetching products from Firestore...")

                val snapshot = productsCollection.get().await()
                android.util.Log.d("ProductRepo", "✅ Firestore returned ${snapshot.documents.size} documents")

                android.util.Log.d("ProductRepo", "📋 Document IDs in Firestore:")
                snapshot.documents.forEachIndexed { index, doc ->
                    android.util.Log.d("ProductRepo", "   ${index + 1}. ${doc.id}")
                }

                val firebaseProducts = snapshot.documents.mapNotNull { doc ->
                    try {
                        // ✅ FIX 1: Handle all possible field variations
                        val name = doc.getString("name") ?: doc.getString("productName") ?: ""

                        // ✅ FIX 2: Normalize category names
                        val rawCategory = doc.getString("category") ?: ""
                        val category = when (rawCategory.lowercase()) {
                            "hot drinks", "cold drinks", "drink", "drinks" -> "Beverages"
                            "snacks", "pastry" -> "Pastries"
                            "ingredient" -> "Ingredients"
                            else -> rawCategory.replaceFirstChar { it.uppercase() }
                        }

                        val price = doc.getDouble("price") ?: 0.0

                        // ✅ FIX 3: Handle quantity (even if it's 0)
                        val quantity = doc.getLong("quantity")?.toInt() ?: 0

                        // ✅ NEW: Handle inventoryA and inventoryB
                        val inventoryA = doc.getLong("inventoryA")?.toInt() ?: quantity  // Default to quantity for backward compatibility
                        val inventoryB = doc.getLong("inventoryB")?.toInt() ?: 0

                        // ✅ FIX 4: Handle NaN and empty imageUri - THE KEY FIX!
                        val imageUri = try {
                            val rawImageUri = doc.getString("imageUri") ?: ""
                            when {
                                rawImageUri.isEmpty() -> ""
                                rawImageUri.equals("NaN", ignoreCase = true) -> ""
                                rawImageUri.equals("nan", ignoreCase = true) -> ""
                                else -> rawImageUri
                            }
                        } catch (e: Exception) {
                            // ✅ If getString fails (because it's actually NaN type, not string), return empty
                            android.util.Log.w("ProductRepo", "⚠️ imageUri is not a string for ${doc.id}, using empty string")
                            ""
                        }

                        // Get cost per unit (for ingredients)
                        val costPerUnit = doc.getDouble("costPerUnit") ?: 0.0

                        android.util.Log.d("ProductRepo", "  📦 ${doc.id}: $name")
                        android.util.Log.d("ProductRepo", "     - category: $rawCategory → $category")
                        android.util.Log.d("ProductRepo", "     - quantity: $quantity")
                        android.util.Log.d("ProductRepo", "     - costPerUnit: $costPerUnit")
                        android.util.Log.d("ProductRepo", "     - imageUri: $imageUri")

                        Entity_Products(
                            id = 0,
                            firebaseId = doc.id,
                            name = name,
                            category = category,
                            price = price,
                            quantity = quantity,
                            inventoryA = inventoryA,
                            inventoryB = inventoryB,
                            costPerUnit = costPerUnit,
                            imageUri = imageUri
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepo", "❌ Error parsing document ${doc.id}: ${e.message}")
                        android.util.Log.e("ProductRepo", "   Stack trace:", e)
                        null
                    }
                }

                android.util.Log.d("ProductRepo", "✅ Parsed ${firebaseProducts.size} products from Firestore")

                val categoryBreakdown = firebaseProducts.groupingBy { it.category }.eachCount()
                android.util.Log.d("ProductRepo", "📊 Category Breakdown:")
                categoryBreakdown.forEach { (category, count) ->
                    android.util.Log.d("ProductRepo", "   $category: $count products")
                }

                if (firebaseProducts.isNotEmpty()) {
                    daoProducts.insertProducts(firebaseProducts)
                    android.util.Log.d("ProductRepo", "✅ Synced to Room database")
                }

                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                firebaseProducts
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ getAll() failed: ${e.message}", e)

                android.util.Log.d("ProductRepo", "⚠️ Falling back to Room database...")
                val roomProducts = daoProducts.getAllProducts()
                android.util.Log.d("ProductRepo", "✅ Room returned ${roomProducts.size} products")
                roomProducts
            }
        }
    }

    // ============ UPDATE WITH IMAGE UPLOAD ============

    suspend fun update(product: Entity_Products) {
        try {
            android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("ProductRepo", "📝 Updating product: ${product.name}")
            android.util.Log.d("ProductRepo", "Input imageUri: ${product.imageUri}")

            // Check if this is a new local image that needs to be uploaded

            // Get the OLD image URL from Firestore before updating
            val oldProduct = try {
                val doc = productsCollection.document(product.firebaseId).get().await()
                doc.getString("imageUri") ?: ""
            } catch (e: Exception) {
                ""
            }

// Check if this is a new local image that needs to be uploaded
            val cloudinaryImageUrl = when {
                product.imageUri.isEmpty() -> {
                    android.util.Log.d("ProductRepo", "⚠️ No image provided")
                    ""
                }
                product.imageUri.startsWith("https://res.cloudinary.com") -> {
                    android.util.Log.d("ProductRepo", "✅ Already a Cloudinary URL, keeping it")
                    product.imageUri
                }
                product.imageUri.startsWith("content://") -> {
                    android.util.Log.d("ProductRepo", "🆕 content:// URI detected - UPLOADING TO CLOUDINARY...")

                    // Delete old image if exists
                    if (oldProduct.isNotEmpty() && oldProduct != product.imageUri) {
                        android.util.Log.d("ProductRepo", "🗑️ Deleting old image first...")
                        deleteImageFromCloudinary(oldProduct)
                    }

                    val uploadedUrl = uploadImageToCloudinary(product.imageUri)
                    android.util.Log.d("ProductRepo", "✅ Uploaded! New URL: $uploadedUrl")
                    uploadedUrl
                }
                product.imageUri.startsWith("file://") ||
                        product.imageUri.contains("/data/user/") -> {
                    android.util.Log.d("ProductRepo", "🆕 file:// URI detected - UPLOADING TO CLOUDINARY...")

                    // Delete old image if exists
                    if (oldProduct.isNotEmpty() && oldProduct != product.imageUri) {
                        android.util.Log.d("ProductRepo", "🗑️ Deleting old image first...")
                        deleteImageFromCloudinary(oldProduct)
                    }

                    val uploadedUrl = uploadImageToCloudinary(product.imageUri)
                    android.util.Log.d("ProductRepo", "✅ Uploaded! New URL: $uploadedUrl")
                    uploadedUrl
                }
                else -> {
                    android.util.Log.w("ProductRepo", "⚠️ Unknown format: ${product.imageUri}")
                    product.imageUri
                }
            }

            android.util.Log.d("ProductRepo", "Final imageUri to save: $cloudinaryImageUrl")

            // Update Firestore with Firebase Storage URL (including dual inventory)
            if (product.firebaseId.isNotEmpty()) {
                val productData = hashMapOf(
                    "name" to product.name,
                    "category" to product.category,
                    "price" to product.price,
                    "quantity" to product.quantity,
                    "inventoryA" to product.inventoryA,
                    "inventoryB" to product.inventoryB,
                    "imageUri" to cloudinaryImageUrl
                )
                productsCollection.document(product.firebaseId).set(productData).await()
                android.util.Log.d("ProductRepo", "✅ Firestore updated with: $cloudinaryImageUrl")
            }

            // Update Room with Cloudinary URL
            val updatedProduct = product.copy(imageUri = cloudinaryImageUrl)
            daoProducts.updateProduct(updatedProduct)

            android.util.Log.d("ProductRepo", "✅ Room updated")
            android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Update failed: ${e.message}", e)
            throw e
        }
    }

    // ============ DELETE ============

    suspend fun delete(product: Entity_Products) {
        try {
            android.util.Log.d("ProductRepo", "🗑️ Deleting product: ${product.name}")

            // Step 1: Delete image from Cloudinary first
            if (product.imageUri.isNotEmpty()) {
                android.util.Log.d("ProductRepo", "🖼️ Deleting product image...")
                deleteImageFromCloudinary(product.imageUri)
            }

            // Step 2: Delete from Firestore
            productsCollection.document(product.firebaseId).delete().await()
            android.util.Log.d("ProductRepo", "✅ Deleted from Firestore")

            // Step 3: Delete from Room
            daoProducts.deleteProduct(product)
            android.util.Log.d("ProductRepo", "✅ Deleted from Room")

            android.util.Log.d("ProductRepo", "✅ Product and image deleted successfully!")

        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Delete failed: ${e.message}", e)
            throw e
        }
    }

    // ============ DEDUCT PRODUCT STOCK (DUAL INVENTORY - B FIRST, THEN A) ============

    suspend fun deductProductStock(productFirebaseId: String, quantity: Int) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "📉 Deducting product stock (Dual Inventory)...")
                android.util.Log.d("ProductRepo", "Product Firebase ID: $productFirebaseId")
                android.util.Log.d("ProductRepo", "Quantity to deduct: $quantity")

                // Get the product
                val product = daoProducts.getProductByFirebaseId(productFirebaseId)

                if (product == null) {
                    android.util.Log.w("ProductRepo", "⚠️ Product not found!")
                    android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext
                }

                android.util.Log.d("ProductRepo", "📦 Product found: ${product.name}")
                android.util.Log.d("ProductRepo", "   Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")

                var remainingToDeduct = quantity
                var newInventoryA = product.inventoryA
                var newInventoryB = product.inventoryB

                // Step 1: Deduct from Inventory B first
                if (newInventoryB > 0) {
                    val deductFromB = minOf(remainingToDeduct, newInventoryB)
                    newInventoryB -= deductFromB
                    remainingToDeduct -= deductFromB
                    android.util.Log.d("ProductRepo", "   Deducted $deductFromB from Inventory B")
                }

                // Step 2: If still need more, deduct from Inventory A
                if (remainingToDeduct > 0 && newInventoryA > 0) {
                    val deductFromA = minOf(remainingToDeduct, newInventoryA)
                    newInventoryA -= deductFromA
                    remainingToDeduct -= deductFromA
                    android.util.Log.d("ProductRepo", "   Deducted $deductFromA from Inventory A")
                }

                // Calculate new total quantity
                val newQuantity = newInventoryA + newInventoryB

                android.util.Log.d("ProductRepo", "   After - Inventory A: $newInventoryA, Inventory B: $newInventoryB, Total: $newQuantity")

                if (remainingToDeduct > 0) {
                    android.util.Log.w("ProductRepo", "⚠️ Warning: Could not deduct full amount. Remaining: $remainingToDeduct")
                }

                // Update Room
                val updatedProduct = product.copy(
                    quantity = newQuantity,
                    inventoryA = newInventoryA,
                    inventoryB = newInventoryB
                )
                daoProducts.updateProduct(updatedProduct)

                // Update Firebase (batch update to minimize writes)
                productsCollection.document(productFirebaseId)
                    .update(
                        mapOf(
                            "quantity" to newQuantity,
                            "inventoryA" to newInventoryA,
                            "inventoryB" to newInventoryB
                        )
                    )
                    .await()

                android.util.Log.d("ProductRepo", "✅ Stock deducted successfully")
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e("ProductRepo", "❌ Failed to deduct stock!")
                android.util.Log.e("ProductRepo", "Error: ${e.message}", e)
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }

    // ============ TRANSFER INVENTORY (A → B) ============

    suspend fun transferInventory(productFirebaseId: String, quantity: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "🔄 Transferring inventory A → B...")
                android.util.Log.d("ProductRepo", "Product Firebase ID: $productFirebaseId")
                android.util.Log.d("ProductRepo", "Quantity to transfer: $quantity")

                // Get the product
                val product = daoProducts.getProductByFirebaseId(productFirebaseId)

                if (product == null) {
                    android.util.Log.w("ProductRepo", "⚠️ Product not found!")
                    android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext Result.failure(Exception("Product not found"))
                }

                // Check if enough stock in Inventory A
                if (product.inventoryA < quantity) {
                    android.util.Log.w("ProductRepo", "⚠️ Insufficient stock in Inventory A")
                    android.util.Log.d("ProductRepo", "   Available: ${product.inventoryA}, Requested: $quantity")
                    android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext Result.failure(Exception("Insufficient stock in Inventory A"))
                }

                android.util.Log.d("ProductRepo", "📦 Product: ${product.name}")
                android.util.Log.d("ProductRepo", "   Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")

                // Transfer from A to B
                val newInventoryA = product.inventoryA - quantity
                val newInventoryB = product.inventoryB + quantity
                val newQuantity = product.quantity // Total remains the same

                android.util.Log.d("ProductRepo", "   After - Inventory A: $newInventoryA, Inventory B: $newInventoryB")

                // Update Room
                val updatedProduct = product.copy(
                    inventoryA = newInventoryA,
                    inventoryB = newInventoryB
                )
                daoProducts.updateProduct(updatedProduct)

                // Update Firebase
                productsCollection.document(productFirebaseId)
                    .update(
                        mapOf(
                            "inventoryA" to newInventoryA,
                            "inventoryB" to newInventoryB
                        )
                    )
                    .await()

                android.util.Log.d("ProductRepo", "✅ Inventory transferred successfully")
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

                Result.success(Unit)

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e("ProductRepo", "❌ Failed to transfer inventory!")
                android.util.Log.e("ProductRepo", "Error: ${e.message}", e)
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(e)
            }
        }
    }

    // ============ SALES OPERATIONS ============


    suspend fun getAllSales(): List<Entity_SalesReport> {
        return daoSalesReport.getAllSales()
    }

    suspend fun clearSales() {
        daoSalesReport.clearSalesReport()
    }

    suspend fun insertSalesReport(sale: Entity_SalesReport) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "💰 Saving sale to Firebase...")
                android.util.Log.d("ProductRepo", "Product: ${sale.productName}")
                android.util.Log.d("ProductRepo", "Product Firebase ID: ${sale.productFirebaseId}")
                android.util.Log.d("ProductRepo", "Category: ${sale.category}")
                android.util.Log.d("ProductRepo", "Quantity: ${sale.quantity}")
                android.util.Log.d("ProductRepo", "Price: ₱${sale.price}")
                android.util.Log.d("ProductRepo", "Date: ${sale.orderDate}")

                // Step 1: Create sale data for Firebase
                val saleData = hashMapOf(
                    "productName" to sale.productName,
                    "category" to sale.category,
                    "quantity" to sale.quantity,
                    "price" to sale.price,
                    "orderDate" to sale.orderDate,
                    "productFirebaseId" to sale.productFirebaseId
                )

                // Step 2: Add to Firestore sales collection
                val salesCollection = firestore.collection("sales")
                val docRef = salesCollection.add(saleData).await()
                android.util.Log.d("ProductRepo", "✅ Sale added to Firestore with ID: ${docRef.id}")

                // Step 3: Save to Room
                daoSalesReport.insertSale(sale)
                android.util.Log.d("ProductRepo", "✅ Sale synced to Room")
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e("ProductRepo", "❌ Failed to save sale!")
                android.util.Log.e("ProductRepo", "Error: ${e.message}", e)
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }
    // ============ SYNC SALES FROM FIREBASE ============

    suspend fun syncSalesFromFirebase(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "📡 Syncing sales from Firestore...")

                val salesCollection = firestore.collection("sales")
                val snapshot = salesCollection.get().await()
                android.util.Log.d("ProductRepo", "✅ Firestore returned ${snapshot.documents.size} sales")

                val salesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Entity_SalesReport(
                            orderId = 0,  // Room will auto-generate

                            productName = doc.getString("productName") ?: "",
                            category = doc.getString("category") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            price = doc.getDouble("price") ?: 0.0,
                            orderDate = doc.getString("orderDate") ?: "",
                            productFirebaseId = doc.getString("productFirebaseId") ?: ""
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepo", "❌ Error parsing sale ${doc.id}: ${e.message}")
                        null
                    }
                }

                android.util.Log.d("ProductRepo", "✅ Parsed ${salesList.size} sales from Firestore")

                // Note: We don't clear sales here, just sync new ones
                // If you want to fully sync, you can clear first: daoSalesReport.clearSalesReport()

                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ Sales sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testFirebaseConnection(): String {
        return try {
            val snapshot = productsCollection.get().await()
            "✅ Firebase connected! Found ${snapshot.documents.size} documents"
        } catch (e: Exception) {
            "❌ Firebase error: ${e.message}"
        }
    }
    suspend fun testStorageConnection(): String {
        return try {
            android.util.Log.d("ProductRepo", "🧪 Testing Cloudinary connection...")
            // Cloudinary is initialized in MainActivity, so just check if MediaManager is ready
            "✅ Cloudinary ready (test upload on first product add)"
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "❌ Cloudinary check failed: ${e.message}", e)
            "❌ Cloudinary error: ${e.message}"
        }
    }
    // ============ SYNC ALL SALES FROM FIREBASE (OPTIMIZED FOR FREE TIER) ============
    suspend fun syncAllSalesFromFirebase(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductRepo", "📡 Syncing recent sales from Firestore (last 30 days only)...")

                // Calculate 30 days ago to minimize Firestore reads
                val thirtyDaysAgo = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))

                val salesCollection = firestore.collection("sales")

                // ✅ OPTIMIZED: Only fetch last 30 days, limit to 500 docs max
                val snapshot = salesCollection
                    .whereGreaterThanOrEqualTo("orderDate", thirtyDaysAgo)
                    .limit(500)  // Extra safety: max 500 documents
                    .get()
                    .await()

                android.util.Log.d("ProductRepo", "✅ Firestore returned ${snapshot.documents.size} sales (last 30 days)")
                android.util.Log.d("ProductRepo", "   💰 Saved ~${700 - snapshot.documents.size} reads by filtering!")

                if (snapshot.documents.isEmpty()) {
                    android.util.Log.d("ProductRepo", "⚠️ No sales found in Firebase")
                    android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext Result.success(Unit)
                }

                val salesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Entity_SalesReport(
                            orderId = 0,  // Room will auto-generate
                            productName = doc.getString("productName") ?: "",
                            category = doc.getString("category") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            price = doc.getDouble("price") ?: 0.0,
                            orderDate = doc.getString("orderDate") ?: "",
                            productFirebaseId = doc.getString("productFirebaseId") ?: ""
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepo", "❌ Error parsing sale ${doc.id}: ${e.message}")
                        null
                    }
                }

                android.util.Log.d("ProductRepo", "✅ Parsed ${salesList.size} sales from Firestore")

                // Clear old sales and insert new ones
                daoSalesReport.clearSalesReport()
                android.util.Log.d("ProductRepo", "🗑️ Cleared old sales from Room")

                salesList.forEach { sale ->
                    daoSalesReport.insertSale(sale)
                }
                android.util.Log.d("ProductRepo", "✅ Synced ${salesList.size} sales to Room")

                android.util.Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.e("ProductRepo", "❌ Sales sync failed: ${e.message}", e)
                android.util.Log.e("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(e)
            }
        }
    }
}