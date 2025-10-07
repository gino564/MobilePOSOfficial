package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [Entity_Users::class], version = 2) // <-- Updated version
abstract class Database_Users : RoomDatabase() {
    abstract fun dao_users(): Dao_Users

    companion object {
        @Volatile
        private var INSTANCE: Database_Users? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tbl_Users ADD COLUMN profileImageUri TEXT")
            }
        }

        fun getDatabase(context: Context): Database_Users {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Database_Users::class.java,
                    "db_users"
                )
                    .addMigrations(MIGRATION_1_2) // <-- Add this
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
