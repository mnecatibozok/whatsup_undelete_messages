package com.mustafa.notiflog.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Ana ekranda gösterilecek kişi/grup özeti.
 * Room bu veri sınıfını, aşağıdaki özel SELECT sorgusunun sonucu olarak
 * otomatik dolduruyor (entity olması gerekmiyor).
 */
data class ConversationSummary(
    val conversationTitle: String,
    val lastTimestamp: Long,
    val deletedCount: Int,
    val totalCount: Int
)

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE conversationTitle LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%'
           OR text LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun search(query: String): LiveData<List<MessageEntity>>

    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE conversationTitle = :conversationTitle 
          AND sender = :sender 
          AND text = :text 
          AND timestamp = :timestamp
    """)
    suspend fun existsExact(conversationTitle: String, sender: String, text: String, timestamp: Long): Int

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    // ---- Kişi/grup listesi (ana ekran) ----

    @Query("""
        SELECT conversationTitle,
               MAX(timestamp) AS lastTimestamp,
               SUM(CASE WHEN isDeleted THEN 1 ELSE 0 END) AS deletedCount,
               COUNT(*) AS totalCount
        FROM messages
        GROUP BY conversationTitle
        ORDER BY lastTimestamp DESC
    """)
    fun getConversations(): LiveData<List<ConversationSummary>>

    @Query("""
        SELECT conversationTitle,
               MAX(timestamp) AS lastTimestamp,
               SUM(CASE WHEN isDeleted THEN 1 ELSE 0 END) AS deletedCount,
               COUNT(*) AS totalCount
        FROM messages
        WHERE conversationTitle LIKE '%' || :query || '%'
        GROUP BY conversationTitle
        ORDER BY lastTimestamp DESC
    """)
    fun searchConversations(query: String): LiveData<List<ConversationSummary>>

    // ---- Bir kişinin/grubun mesajları ----

    @Query("SELECT * FROM messages WHERE conversationTitle = :title AND isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedForConversation(title: String): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationTitle = :title ORDER BY timestamp DESC")
    fun getAllForConversation(title: String): LiveData<List<MessageEntity>>

    // ---- Silinme tespiti ----

    /**
     * WhatsApp'ın "Bu mesaj silindi" güncellemesi, orijinal mesajla aynı
     * conversationTitle+sender+timestamp üçlüsünü koruyor gibi görünüyor.
     * Bu yüzden önce tam eşleşmeyle işaretlemeyi deniyoruz.
     * @return Kaç satırın güncellendiği (0 ise eşleşme bulunamadı demektir)
     */
    @Query("""
        UPDATE messages SET isDeleted = 1
        WHERE conversationTitle = :conversationTitle
          AND sender = :sender
          AND timestamp = :timestamp
          AND isDeleted = 0
    """)
    suspend fun markDeletedExact(conversationTitle: String, sender: String, timestamp: Long): Int

    /**
     * Tam zaman eşleşmesi bulunamazsa yedek yöntem: o kişiden/gruptaki
     * gönderenden gelen, henüz silinmemiş EN SON mesajı silinmiş olarak işaretle.
     * (WhatsApp'ın bildirim güncellemesinde zaman damgasını korumadığı
     * durumlar için en iyi tahmin.)
     */
    @Query("""
        UPDATE messages SET isDeleted = 1
        WHERE id = (
            SELECT id FROM messages
            WHERE conversationTitle = :conversationTitle
              AND sender = :sender
              AND isDeleted = 0
            ORDER BY timestamp DESC
            LIMIT 1
        )
    """)
    suspend fun markLatestDeletedFallback(conversationTitle: String, sender: String): Int
}
