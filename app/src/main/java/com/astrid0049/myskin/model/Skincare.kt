package com.astrid0049.myskin.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skincare_table")
data class Skincare(
    @PrimaryKey val id: String,
    val nama: String,
    val brand: String,
    val imageId: String,
    val mine: Int
)
