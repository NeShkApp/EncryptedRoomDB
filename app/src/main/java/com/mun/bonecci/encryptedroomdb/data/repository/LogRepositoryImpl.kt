package com.mun.bonecci.encryptedroomdb.data.repository

import android.content.Context
import com.mun.bonecci.encryptedroomdb.data.models.Log
import com.mun.bonecci.encryptedroomdb.db.UserDatabase
import com.mun.bonecci.encryptedroomdb.domain.repository.LogRepository

class LogRepositoryImpl(context: Context) : LogRepository {
    private val logDao = UserDatabase.getInstance(context).logDao()

    override suspend fun getAllLogs(): List<Log> {
        return logDao.getAllLogs()
    }

    override suspend fun insertLog(log: Log): Long {
        return logDao.insertLog(log)
    }

    override suspend fun deleteLogById(id: Long): Int {
        return logDao.deleteLogById(id)
    }

    override suspend fun deleteAllLogs() {
        return logDao.deleteAllLogs()
    }
}