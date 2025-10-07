package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface Dao_Products {
    @Insert
    suspend fun insertProduct(product: Entity_Products)

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Entity_Products>

    @Delete
    suspend fun deleteProduct(product: Entity_Products)

    @Update
    suspend fun updateProduct(product: Entity_Products)

}
