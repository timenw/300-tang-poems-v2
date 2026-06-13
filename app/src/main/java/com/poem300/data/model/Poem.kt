package com.poem300.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poems")
data class Poem(
    @PrimaryKey val id: Int?,                                          // DB: INTEGER PRIMARY KEY (notNull=false)
    val title: String,
    val titlePinyin: String,
    val titleEn: String,
    val author: String,
    val authorPinyin: String,
    val authorEn: String,
    val dynasty: String,
    val dynastyEn: String,
    val content: String,
    val translation: String,
    val annotation: String,
    val category: String,
    @ColumnInfo(defaultValue = "1") val difficulty: Int = 1            // DB: DEFAULT 1
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val poemId: Int,
    @ColumnInfo(defaultValue = "") val note: String = "",
    @ColumnInfo(defaultValue = "Default") val groupName: String = "Default",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0L
)
