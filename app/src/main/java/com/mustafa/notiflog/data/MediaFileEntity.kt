package com.mustafa.notiflog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * WhatsApp medya klasöründen (Images/Video) yakalanıp app'in kendi private
 * klasörüne kopyalanan bir dosyayı temsil eder. WhatsApp sohbetten dosyayı
 * silse bile buradaki kopya kalıcı olarak durur.
 */
@Entity(tableName = "media_files")
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUri: String,
    val localBackupPath: String,
    val mediaType: String, // "image" | "video"
    val fileName: String,
    val dateAddedMillis: Long,
    val sizeBytes: Long
)
