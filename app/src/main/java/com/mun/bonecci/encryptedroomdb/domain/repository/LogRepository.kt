package com.mun.bonecci.encryptedroomdb.domain.repository

import com.mun.bonecci.encryptedroomdb.data.models.Log

interface LogRepository {
    suspend fun getAllLogs(): List<Log>
    suspend fun insertLog(log: Log): Long
    suspend fun deleteLogById(id: Long): Int
    suspend fun deleteAllLogs()
}