package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Entity_Products::class, Entity_SalesReport::class], // Include both
    version = 2 // ⚠️ Change to 2 if you've already released version 1
)
abstract class Database_Products: RoomDatabase() {
    abstract fun dao_products(): Dao_Products
    abstract fun dao_salesReport(): Dao_SalesReport

    companion object {
        @Volatile private var INSTANCE: Database_Products? = null

        fun getDatabase(context: Context): Database_Products{
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Database_Products::class.java,
                    "product_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
