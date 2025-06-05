package com.mun.bonecci.encryptedroomdb.domain.repository

import com.mun.bonecci.encryptedroomdb.data.models.Session

interface SessionRepository {
    suspend fun getAllSessions(): List<Session>
    suspend fun insertSession(session: Session): Long
    suspend fun deleteSessionById(id: Long): Int
    suspend fun deleteAllSessions()
}