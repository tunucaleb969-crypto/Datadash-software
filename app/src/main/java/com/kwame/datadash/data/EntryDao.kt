package com.kwame.datadash.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY id DESC")
    fun getAll(): Flow<List<Entry>>

    @Query(
        "SELECT * FROM entries WHERE name LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' " +
            "OR phone LIKE '%' || :query || '%' ORDER BY id DESC"
    )
    fun search(query: String): Flow<List<Entry>>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries ORDER BY id ASC")
    suspend fun getAllOnce(): List<Entry>

    @Insert
    suspend fun insertAll(entries: List<Entry>)
}
