package com.ukp.axrcockpit.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands")
    fun observeAll(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands")
    fun getAll(): List<CustomCommandEntity>

    @Query("SELECT * FROM custom_commands WHERE id = :id LIMIT 1")
    fun getById(id: String): CustomCommandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CustomCommandEntity)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_commands WHERE id LIKE :prefix")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM custom_commands")
    suspend fun deleteAll()
}
