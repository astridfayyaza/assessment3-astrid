package com.astrid0049.myskin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.astrid0049.myskin.model.Skincare

@Database(entities = [Skincare::class], version = 2, exportSchema = false)
abstract class SkincareDatabase : RoomDatabase() {
    abstract fun skincareDao(): SkincareDao

    companion object {
        @Volatile
        private var INSTANCE: SkincareDatabase? = null

        fun getDatabase(context: Context): SkincareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkincareDatabase::class.java,
                    "skincare_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}