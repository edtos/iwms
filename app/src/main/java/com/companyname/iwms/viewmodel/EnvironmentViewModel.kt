package com.companyname.iwms.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companyname.iwms.model.Environment
import com.companyname.iwms.viewmodel.EnvironmentRepository
import kotlinx.coroutines.launch
class EnvironmentViewModel(private val repository: EnvironmentRepository) : ViewModel() {

    val allEnvironments: LiveData<List<Environment>> = repository.getAllEnvironments()

    // Fetch environment by id
    fun getEnvironmentById(id: Int): LiveData<Environment> {
        return repository.getEnvironmentById(id)
    }

    // Insert environment
    fun insertEnvironment(environment: Environment) = viewModelScope.launch {
        repository.insertEnvironment(environment)
    }

    // Update environment
    fun updateEnvironment(environment: Environment) = viewModelScope.launch {
        repository.updateEnvironment(environment)
    }
    // Delete an environment by ID
    fun deleteEnvironment(id: Int) {
        viewModelScope.launch {
            repository.deleteEnvironmentById(id)
        }

    }

     suspend fun getEnvironmentByName(name: String) {
        return repository.environmentByName(name)
    }
    // Other methods as necessary...
}
