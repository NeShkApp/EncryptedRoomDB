package com.mun.bonecci.encryptedroomdb.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mun.bonecci.encryptedroomdb.data.models.Session

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM session WHERE userId = :userId")
    suspend fun getSessionsForUser(userId: Long): List<Session>

    @Query("SELECT * FROM session")
    suspend fun getAllSessions(): List<Session>

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteSessionById(id: Long): Int

    @Query("DELETE FROM session")
    suspend fun deleteAllSessions()
}