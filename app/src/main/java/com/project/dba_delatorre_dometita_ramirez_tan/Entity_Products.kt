package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Entity_Products(
    val id: Int = 0,
    @PrimaryKey val firebaseId: String = "",
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val imageUri: String
)