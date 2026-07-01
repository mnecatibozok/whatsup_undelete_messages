package com.mustafa.notiflog.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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
}
