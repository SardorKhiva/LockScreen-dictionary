package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val english: String,
    val russian: String,
    val uzbek: String,
    val exampleEnglish: String = "",
    val exampleRussian: String = "",
    val exampleUzbek: String = "",
    val shownCount: Int = 0,
    val isFavorite: Boolean = false
)
