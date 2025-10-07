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

    // ✅ Load products on ViewModel init
    init {
        getAllProducts()
    }

    fun getAllProducts() {
        viewModelScope.launch {
            productList = repository.getAll()
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

}

// ✅ Factory
class ProductViewModelFactory(private val repository: ProductRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository) as T
    }
}
