package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val id: String,
    val patientId: String,
    val fileName: String,
    val filePath: String, // Local storage path or URI
    val fileType: String, // image/jpeg, application/pdf
    val uploadedAt: Long,
    val status: String // pending, processed, error
)
