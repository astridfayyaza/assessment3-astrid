package com.astrid0049.myskin.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astrid0049.myskin.model.Skincare

@Dao
interface SkincareDao {
    @Query("SELECT * FROM skincare_table ORDER BY id DESC")
    suspend fun getAllSkincare(): List<Skincare>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(skincareList: List<Skincare>)

    @Query("DELETE FROM skincare_table")
    suspend fun clearAll()
}