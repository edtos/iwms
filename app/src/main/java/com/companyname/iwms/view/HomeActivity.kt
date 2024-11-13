package com.companyname.iwms.view

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.companyname.iwms.R
import com.companyname.iwms.config.Config
import com.companyname.iwms.config.InteractiveSSH
import com.companyname.iwms.model.Environment
import com.companyname.iwms.viewmodel.EnvironmentViewModel
import com.companyname.iwms.viewmodel.EnvironmentViewModelFactory
import com.companyname.iwms.viewmodel.IWMSApplication
import kotlinx.coroutines.*

class HomeActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var environmentSpinner: Spinner
    private lateinit var signInButton: Button
    private lateinit var addEnvironmentButton: TextView

    private val viewModel: EnvironmentViewModel by viewModels {
        EnvironmentViewModelFactory((application as IWMSApplication).repository)
    }

    private var selectedEnvironmentId: Int? = null
    private var sshConnection: InteractiveSSH? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        environmentSpinner = findViewById(R.id.environmentSpinner)
        signInButton = findViewById(R.id.signInButton)
        addEnvironmentButton = findViewById(R.id.addEnvironmentButton)

        addEnvironmentButton.setOnClickListener {
            val intent = Intent(this, EnvironmentListActivity::class.java)
            startActivity(intent)
        }
        signInButton.isEnabled = false

        // Observing environments from ViewModel
        viewModel.allEnvironments.observe(this, Observer { environments ->
            val environmentNames = if (environments.isEmpty()) {
                mutableListOf("No Environments Available")
            } else {
                environments.map { it.environment }.toMutableList()
            }

            // Set up the Spinner adapter
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, environmentNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            environmentSpinner.adapter = adapter

            environmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (environments.isNotEmpty() && position >= 0 && position < environments.size) {
                        selectedEnvironmentId = environments[position].id
                        signInButton.isEnabled = true
                    } else {
                        selectedEnvironmentId = null
                        sshConnection = null
                        signInButton.isEnabled = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedEnvironmentId = null
                    sshConnection = null
                }
            }
        })
        signInButton.setOnClickListener {
            scope.launch {
                sendCredentialsAndConnect()
            }
        }
    }

    private suspend fun sendCredentialsAndConnect() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        withContext(Dispatchers.Main) {
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this@HomeActivity, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@withContext
            }
        }

        val selectedEnvironment = viewModel.allEnvironments.value?.find { it.id == selectedEnvironmentId }
        if (selectedEnvironment != null) {
            connectToSelectedEnvironment(selectedEnvironment, username, password)
        }
    }

    private suspend fun connectToSelectedEnvironment(environment: Environment, username: String, password: String) {
        try {
            sshConnection?.disconnect()
            sshConnection = InteractiveSSH(
                hostname = environment.host,
                username = environment.username,
                password = environment.password,
                port = environment.port.toInt()
            )
            sshConnection?.connect()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeActivity, "Connected to ${environment.environment}", Toast.LENGTH_SHORT).show()
            }

            // Send credentials
            val command = "$username\t$password"
            sshConnection?.executeCommand(command)

            // Start listening for menu items after sending credentials
            listenForMenuItems()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeActivity, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSessionExistsDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Session Conflict")
            setMessage("Another session already exists for this user. That session will end. Proceed?")
            setPositiveButton("Yes") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-A")
                }
            }
            setNegativeButton("No") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-W")
                }
            }
            create()
            show()
        }
    }

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-A" -> "\u0001" // ASCII for Ctrl-A
            "Ctrl-W" -> "\u0017" // ASCII for Ctrl-W
            else -> ""
        }
        sshConnection?.executeCommand(controlCommand)
    }

    private fun listenForMenuItems() {
        scope.launch {
            sshConnection?.getResponseFlow()?.collect { response ->
                if (Config.menu1.size>0) { // Replace "Menu1" with the actual keyword for the menu item
                    withContext(Dispatchers.Main) {
                        println(Config.menu1.toString())
                        val i = Intent(this@HomeActivity, Menu1Activity::class.java)
                        startActivity(i)
                    }
                }

                // Debug log to check received responses
                if (response.contains("Another active session")) {
                    withContext(Dispatchers.Main) {
                        showSessionExistsDialog()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        scope.launch {
            sshConnection?.disconnect()
        }
    }
}
