package com.mun.bonecci.encryptedroomdb.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val startedAt: Long,
    val endedAt: Long?
)