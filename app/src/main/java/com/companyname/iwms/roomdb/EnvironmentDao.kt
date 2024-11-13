package com.companyname.iwms.roomdb

import androidx.lifecycle.LiveData
import androidx.room.*
import com.companyname.iwms.model.Environment
@Dao
interface EnvironmentDao {

    @Query("SELECT * FROM environments")
    fun getAllEnvironments(): LiveData<List<Environment>>

    @Query("SELECT * FROM environments WHERE id = :id")
    fun getEnvironmentById(id: Int): LiveData<Environment>

    @Query("SELECT * FROM environments WHERE environment = :name")
    fun getEnvironmentByName(name: String): LiveData<Environment>

    @Insert
    suspend fun insertEnvironment(environment: Environment)

    @Update
    suspend fun updateEnvironment(environment: Environment)


    // Delete environment by ID
    @Query("DELETE FROM environments WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Other methods as necessary...
}