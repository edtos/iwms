package com.companyname.iwms.config

import android.util.Log
import com.companyname.iwms.model.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SSHConnectionManager {

    var sshConnection: InteractiveSSH? = null
    private var isConnected = false

    // Connect to the environment
    suspend fun connect(environment: Environment) {
        try {
            // Ensure we disconnect any existing session before creating a new one
            if (isConnected) {
                disconnect()
            }

            sshConnection = InteractiveSSH(
                hostname = environment.host,
                username = environment.username,
                password = environment.password,
                port = environment.port.toInt()
            )

            // Perform connection operation asynchronously
            withContext(Dispatchers.IO) {
                sshConnection?.connect()
            }

            // After connection attempt, check if it was successful
            if (sshConnection?.isConnected() == true) {
                isConnected = true
                Log.d("SSHConnectionManager", "Successfully connected to ${environment.host}")
            } else {
                throw IOException("Failed to establish connection to ${environment.host}")
            }
        } catch (e: Exception) {
            Log.e("SSHConnectionManager", "Error during SSH connection: ${e.message}")
            isConnected = false
            throw e // Re-throw to handle in calling code if necessary
        }
    }

    // Disconnect the current SSH session
    suspend fun disconnect() {
        try {
            if (isConnected) {
                withContext(Dispatchers.IO) {
                    sshConnection?.disconnect()
                }
                isConnected = false
                Log.d("SSHConnectionManager", "Disconnected from SSH session")
            }
        } catch (e: Exception) {
            Log.e("SSHConnectionManager", "Error during SSH disconnection: ${e.message}")
        } finally {
            sshConnection = null
        }
    }

    // Check if the connection is active
    fun isConnected(): Boolean {
        return isConnected && sshConnection?.isConnected() == true
    }
}
