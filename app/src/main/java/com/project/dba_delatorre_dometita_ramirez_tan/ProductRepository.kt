package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Delete
import androidx.room.Update

class ProductRepository(private val daoProducts: Dao_Products,
                        private val daoSalesReport: Dao_SalesReport) {

    suspend fun insert(product: Entity_Products) {
        daoProducts.insertProduct(product)
    }

    suspend fun getAll(): List<Entity_Products> {
        return daoProducts.getAllProducts()
    }

    suspend fun delete(product: Entity_Products) {
        daoProducts.deleteProduct(product)
    }

    suspend fun update(product: Entity_Products) {
        daoProducts.updateProduct(product)
    }
    suspend fun getAllSales(): List<Entity_SalesReport> {
        return daoSalesReport.getAllSales()
    }

    suspend fun clearSales() {
        daoSalesReport.clearSalesReport()
    }
    suspend fun insertSalesReport(sale: Entity_SalesReport) {
        daoSalesReport.insertSale(sale)
    }

}
