package com.companyname.iwms.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "environments")
data class Environment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val environment: String,
    val host: String,
    val username: String,
    val password: String,
    val port: String
)
