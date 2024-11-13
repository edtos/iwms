package com.companyname.iwms.viewmodel

import androidx.lifecycle.LiveData
import com.companyname.iwms.model.Environment
import com.companyname.iwms.roomdb.EnvironmentDao
import kotlinx.coroutines.flow.Flow

class EnvironmentRepository(private val environmentDao: EnvironmentDao) {

    fun getAllEnvironments(): LiveData<List<Environment>> = environmentDao.getAllEnvironments()

     fun getEnvironmentById(id: Int): LiveData<Environment> {
        return environmentDao.getEnvironmentById(id)
    }

    suspend fun insertEnvironment(environment: Environment) {
        environmentDao.insertEnvironment(environment)
    }

    suspend fun updateEnvironment(environment: Environment) {
        environmentDao.updateEnvironment(environment)
    }
    // Delete environment by ID
    suspend fun deleteEnvironmentById(id: Int) {
        environmentDao.deleteById(id)
    }

    suspend fun environmentByName(name:String){
        environmentDao.getEnvironmentByName(name)
    }

    // Other methods as necessary...
}
