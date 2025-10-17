package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales_report")
data class Entity_SalesReport(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val orderDate: String // You can store this as ISO-8601 string like "2025-07-09"
)
data class TopSalesItem(
    val productName: String,
    val totalSold: Int,
    val totalRevenue: Double

)
git