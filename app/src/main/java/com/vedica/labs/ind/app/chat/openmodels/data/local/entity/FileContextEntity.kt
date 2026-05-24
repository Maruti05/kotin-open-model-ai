package com.vedica.labs.ind.app.chat.openmodels.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_contexts")
data class FileContextEntity(
    @PrimaryKey val id: String,
    val filename: String,
    val content: String,
    val addedAt: Long
)
