package com.companyname.iwms.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.companyname.iwms.R
import com.companyname.iwms.config.Config
import com.companyname.iwms.config.SSHConnectionManager
import com.companyname.iwms.model.Environment
import com.companyname.iwms.viewmodel.EnvironmentViewModel
import com.companyname.iwms.viewmodel.EnvironmentViewModelFactory
import com.companyname.iwms.viewmodel.IWMSApplication
import kotlinx.coroutines.*

class HomeActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var showPasswordIcon: ImageView
    private lateinit var environmentSpinner: Spinner
    private lateinit var signInButton: Button
    private lateinit var addEnvironmentButton: TextView
    private lateinit var loadingIndicator: ProgressBar

    private val sshManager: SSHConnectionManager by lazy {
        (application as IWMSApplication).sshConnectionManager
    }
    private val viewModel: EnvironmentViewModel by viewModels {
        EnvironmentViewModelFactory((application as IWMSApplication).repository)
    }

    private var selectedEnvironmentId: Int? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        environmentSpinner = findViewById(R.id.environmentSpinner)
        signInButton = findViewById(R.id.loginButton)
        addEnvironmentButton = findViewById(R.id.addEnvironmentButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        showPasswordIcon = findViewById(R.id.showPasswordIcon)

        // Password visibility toggle
        showPasswordIcon.setOnClickListener {
            togglePasswordVisibility()
        }

        addEnvironmentButton.setOnClickListener {
            val intent = Intent(this, EnvironmentListActivity::class.java)
            startActivity(intent)
        }

        signInButton.isEnabled = false
        observeEnvironments()

        signInButton.setOnClickListener {
            disableButtonWithLoading()
            scope.launch {
                sendCredentialsAndConnect()
            }
        }
    }

    private fun togglePasswordVisibility() {
        if (passwordEditText.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            showPasswordIcon.setImageResource(R.drawable.ic_eye_off)
        } else {
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            showPasswordIcon.setImageResource(R.drawable.ic_eye_icon)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun observeEnvironments() {
        viewModel.allEnvironments.observe(this, Observer { environments ->
            setupEnvironmentSpinner(environments)
        })
    }

    private fun setupEnvironmentSpinner(environments: List<Environment>) {
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
                selectedEnvironmentId = if (environments.isNotEmpty() && position in environments.indices) {
                    signInButton.isEnabled = true
                    environments[position].id
                } else {
                    signInButton.isEnabled = false
                    null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedEnvironmentId = null
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
            sshManager.connect(environment)

            delay(3000)

            val command = "$username\t$password"
            sshManager.sshConnection?.executeCommand(command)

            scope.launch {
                sshManager.sshConnection?.getResponseFlow()?.collect { response ->
                    handleSSHResponse(response)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showAlert("SSH Connection failed: ${e.message}")
            }
        } finally {
            withContext(Dispatchers.Main) {
                enableButtonAndHideLoading()
            }
        }
    }

    private suspend fun handleSSHResponse(response: String) {
        withContext(Dispatchers.Main) {
            when {
                response.contains("Another active session") -> {
                    showSessionExistsDialog()
                }
                response.contains("Invalid Login") -> {
                    showErrorPopup("Incorrect password")
                }

                response.contains("1)") -> {
                    delay(1000)
                    listenForMenuItems()
                    delay(1000)
                }
            }
        }
    }
    private fun showSessionExistsDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Session Conflict")
            setMessage("Another session already exists for this user. That session will end. Proceed?")
            setPositiveButton("Yes") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-A")  // Command to end the existing session
                    delay(1000)
                    listenForMenuItems()  // Resume menu processing if necessary
                }
            }
            setNegativeButton("No") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-W")  // Optional: Command to cancel the login attempt
                    showToast("Session cancelled")
                }
            }
            create()
            show()
        }
    }

    private fun showErrorPopup(message: String) {
        AlertDialog.Builder(this).apply {
            setTitle("Error")
            setMessage(message)
            setPositiveButton("OK") { _, _ ->
                scope.launch {
                    sendControlCommand("Ctrl-X") // Send CTRL-X command
                }


                usernameEditText.setText("")
                passwordEditText.setText("")
            }
            create()
            show()
        }
    }

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-X" -> "\u0018"
            "Ctrl-A" -> "\u0001"
            "Ctrl-W" -> "\u0017"
            else -> ""
        }
        sshManager.sshConnection?.executeCommand(controlCommand)
    }

    private fun listenForMenuItems() {
        if (Config.menu1.isNotEmpty()) {
            val intent = Intent(this@HomeActivity, Menu1Activity::class.java)
            startActivity(intent)
        }
    }

    private fun disableButtonWithLoading() {
        signInButton.isEnabled = false
        loadingIndicator.visibility = View.VISIBLE
    }

    private fun enableButtonAndHideLoading() {
        signInButton.isEnabled = true
        loadingIndicator.visibility = View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this@HomeActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this).apply {
            setTitle("Error")
            setMessage(message)
            setPositiveButton("OK", null)
            create()
            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        scope.launch {
            sshManager.disconnect()
        }
    }
}
