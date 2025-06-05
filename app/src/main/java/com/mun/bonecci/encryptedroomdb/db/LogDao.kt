package com.mun.bonecci.encryptedroomdb.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mun.bonecci.encryptedroomdb.data.models.Log

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: Log): Long

    @Query("SELECT * FROM log ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<Log>

    @Query("SELECT * FROM log WHERE id = :id")
    suspend fun getLogById(id: Long): Log

    @Query("DELETE FROM log WHERE id = :id")
    suspend fun deleteLogById(id: Long): Int

    @Query("DELETE FROM log")
    suspend fun deleteAllLogs()
}