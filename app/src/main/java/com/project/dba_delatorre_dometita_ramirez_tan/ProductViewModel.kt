package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    // ✅ Expose product list for UI
    var productList by mutableStateOf<List<Entity_Products>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // ✅ Load products on ViewModel init
    init {
        viewModelScope.launch {
            android.util.Log.d("ProductViewModel", "🚀 ViewModel initialized")

            // Test connections
            val firebaseTest = repository.testFirebaseConnection()
            android.util.Log.d("ProductViewModel", "🔥 Firestore Test: $firebaseTest")

            val storageTest = repository.testStorageConnection()
            android.util.Log.d("ProductViewModel", "📦 Storage Test: $storageTest")

            // Sync sales from Firebase
            repository.syncSalesFromFirebase()

            // ✅ Then load products and WAIT for completion
            getAllProducts()

            android.util.Log.d("ProductViewModel", "✅ Initial sync complete")
        }
    }

    fun getAllProducts() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                android.util.Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("ProductViewModel", "📲 ViewModel requesting products...")

                val fetchedProducts = repository.getAll()

                android.util.Log.d("ProductViewModel", "✅ Repository returned ${fetchedProducts.size} products")

                productList = fetchedProducts

                if (productList.isEmpty()) {
                    errorMessage = "No products found. Please add products."
                    android.util.Log.w("ProductViewModel", "⚠️ Product list is EMPTY!")
                } else {
                    android.util.Log.d("ProductViewModel", "✅ Product list updated with ${productList.size} items:")
                    productList.forEachIndexed { index, product ->
                        android.util.Log.d("ProductViewModel", "  ${index + 1}. ${product.name}")
                        android.util.Log.d("ProductViewModel", "     - Price: ₱${product.price}")
                        android.util.Log.d("ProductViewModel", "     - Quantity: ${product.quantity}")
                        android.util.Log.d("ProductViewModel", "     - Firebase ID: ${product.firebaseId}")
                        android.util.Log.d("ProductViewModel", "     - Image URI: ${product.imageUri}")
                    }
                }

                android.util.Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                errorMessage = "Failed to load products: ${e.message}"
                android.util.Log.e("ProductViewModel", "❌ ViewModel error: ${e.message}", e)
                android.util.Log.e("ProductViewModel", "Stack trace:", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun insertProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.insert(product)
            getAllProducts() // refresh after insert
        }
    }

    fun deleteProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.delete(product)
            getAllProducts() // refresh after delete
        }
    }
    fun deductProductStock(productFirebaseId: String, quantity: Int) {
        viewModelScope.launch {
            repository.deductProductStock(productFirebaseId, quantity)
        }
    }

    fun updateProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.update(product)
            getAllProducts() // Refresh list after update
        }
    }

    fun insertSalesReport(sale: Entity_SalesReport) {
        viewModelScope.launch {
            repository.insertSalesReport(sale)
        }
    }

    // ============ DUAL INVENTORY METHODS ============

    fun transferInventory(productFirebaseId: String, quantity: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.transferInventory(productFirebaseId, quantity)
            onResult(result)
            if (result.isSuccess) {
                getAllProducts() // Refresh list after transfer
            }
        }
    }
}

// ✅ Factory
class ProductViewModelFactory(private val repository: ProductRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository) as T
    }
}