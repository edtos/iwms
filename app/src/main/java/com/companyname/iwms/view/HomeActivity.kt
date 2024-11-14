package com.companyname.iwms.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
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
        signInButton = findViewById(R.id.loginButton)
        addEnvironmentButton = findViewById(R.id.addEnvironmentButton)
        val showPasswordIcon = findViewById<ImageView>(R.id.showPasswordIcon)

        // Password visibility toggle
        showPasswordIcon.setOnClickListener {
            if (passwordEditText.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showPasswordIcon.setImageResource(R.drawable.ic_eye_off)
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                showPasswordIcon.setImageResource(R.drawable.ic_eye_icon)
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

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
                Toast.makeText(this@HomeActivity, "SSH connection established", Toast.LENGTH_SHORT).show()
            }

            val command = "$username\t$password"
            sshConnection?.executeCommand(command)

            scope.launch {
                sshConnection?.getResponseFlow()?.collect { response ->
                    if (response.contains("Another active session")) {
                        withContext(Dispatchers.Main) {
                            showSessionExistsDialog()
                        }
                    }

                    else if (response.contains("Invalid Login")) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Invalid login credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else if(response.contains("1)")){
                        delay(1000)
                        listenForMenuItems()
                        delay(1000)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeActivity, "SSH Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    listenForMenuItems()
                    delay(1000)
                }
            }
            setNegativeButton("No") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-W")
                    Toast.makeText(this@HomeActivity, "Session cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            create()
            show()
        }
    }

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-A" -> "\u0001"
            "Ctrl-W" -> "\u0017"
            else -> ""
        }
        sshConnection?.executeCommand(controlCommand)
    }

    private fun listenForMenuItems() {
            if (Config.menu1.size>0) {
                    val intent = Intent(this@HomeActivity, Menu1Activity::class.java)
                    startActivity(intent)
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
