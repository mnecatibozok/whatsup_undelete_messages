package com.mustafa.notiflog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * WhatsApp bildiriminden yakalanan tek bir mesajı temsil eder.
 *
 * conversationTitle: Grup adı ya da kişi adı (bildirimin başlığı)
 * sender: Grup mesajlarında gönderen kişinin adı (özel sohbette conversationTitle ile aynı olabilir)
 * text: Mesaj içeriği
 * isGroup: Grup sohbeti mi yoksa özel mi olduğunun tahmini
 * timestamp: Mesajın yakalandığı zaman (epoch millis)
 * isDeleted: WhatsApp'ın "Bu mesaj silindi" bildirim güncellemesiyle
 *            karşı tarafın bu mesajı sildiği tespit edildiyse true olur.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val conversationTitle: String,
    val sender: String,
    val text: String,
    val isGroup: Boolean,
    val timestamp: Long,
    val notificationKey: String,
    val isDeleted: Boolean = false
)
