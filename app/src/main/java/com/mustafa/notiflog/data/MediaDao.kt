package com.mustafa.notiflog.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaFileEntity)

    @Query("SELECT * FROM media_files ORDER BY dateAddedMillis DESC")
    fun getAll(): LiveData<List<MediaFileEntity>>

    @Query("SELECT COUNT(*) FROM media_files WHERE originalUri = :uri")
    suspend fun exists(uri: String): Int

    @Query("DELETE FROM media_files")
    suspend fun clearAll()
}
