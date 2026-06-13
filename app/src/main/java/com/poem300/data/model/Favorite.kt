package com.poem300.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["poemId"], unique = true)]
)
data class Favorite(
    @PrimaryKey
    @ColumnInfo(name = "poemId")
    val poemId: Int?,

    @ColumnInfo(name = "note", defaultValue = "")
    val note: String = "",

    @ColumnInfo(name = "groupName", defaultValue = "Default")
    val groupName: String = "Default",

    @ColumnInfo(name = "createdAt", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)
