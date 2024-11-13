package com.companyname.iwms.view

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.companyname.iwms.config.InteractiveSSH
import com.companyname.iwms.R
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private lateinit var loginUsername: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var responseTextView: TextView

    private var ssh: InteractiveSSH? = null
    private var responseJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        loginUsername = findViewById(R.id.loginUsername)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        responseTextView = findViewById(R.id.outputTextView)

        // Retrieve connection parameters from Intent
        val hostname = intent.getStringExtra("hostname") ?: ""
        val username = intent.getStringExtra("username") ?: ""
        val password = intent.getStringExtra("password") ?: ""
        val port = intent.getIntExtra("port", 22)

        // Initialize SSH instance with retrieved parameters
        ssh = InteractiveSSH(hostname, username, password, port)

        // Connect SSH (assuming you want to connect immediately on opening LoginActivity)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ssh?.connect()
                withContext(Dispatchers.Main) {
                    responseTextView.append("Connected successfully!\n")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SSH Connection Error", "Connection failed", e)
                }
            }
        }
        loginUsername.setText("JP")
        loginPassword.setText("Welcome16")

        loginButton.setOnClickListener {
            val usernameInput = loginUsername.text.toString()
            val passwordInput = loginPassword.text.toString()

            if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(usernameInput, passwordInput)
            }
        }

        // Start collecting responses
        startCollectingResponse()
    }

    private fun performLogin(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ssh?.executeCommand("$username\t$password\n")
                Log.e("Login Success", "send login credentials"+"$username\t$password")

                delay(1000) // Allow some time for the command to execute
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Login Error", "Failed to send login credentials", e)
                }
            }
        }
    }
    private fun cleanOutput(output: String): String {
        return output.replace(Regex("\\x1B\\[[0-9;]*[a-zA-Z]"), "")
    }
    private fun startCollectingResponse() {
        responseJob?.cancel()

        responseJob = CoroutineScope(Dispatchers.Main).launch {
            ssh?.getResponseFlow()?.collect { response ->
                val cleanedResponse = cleanOutput(response)
                responseTextView.append("$cleanedResponse\n")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        responseJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            ssh?.disconnect()
        }
    }
}
