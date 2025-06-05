package com.mun.bonecci.encryptedroomdb.data.repository

import android.content.Context
import com.mun.bonecci.encryptedroomdb.data.models.Session
import com.mun.bonecci.encryptedroomdb.db.UserDatabase
import com.mun.bonecci.encryptedroomdb.domain.repository.SessionRepository

class SessionRepositoryImpl(context: Context) : SessionRepository {
    private val sessionDao = UserDatabase.getInstance(context).sessionDao()

    override suspend fun getAllSessions(): List<Session> {
        return sessionDao.getAllSessions()
    }

    override suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session)
    }

    override suspend fun deleteSessionById(id: Long): Int {
        return sessionDao.deleteSessionById(id)
    }

    override suspend fun deleteAllSessions() {
        return sessionDao.deleteAllSessions()
    }
}