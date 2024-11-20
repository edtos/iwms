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
import androidx.lifecycle.lifecycleScope
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
    private val responseBuffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeUI()
        observeEnvironments()
        setListeners()
    }

    private fun initializeUI() {
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        environmentSpinner = findViewById(R.id.environmentSpinner)
        signInButton = findViewById(R.id.loginButton)
        addEnvironmentButton = findViewById(R.id.addEnvironmentButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        showPasswordIcon = findViewById(R.id.showPasswordIcon)
        signInButton.isEnabled = false
    }

    private fun setListeners() {
        showPasswordIcon.setOnClickListener { togglePasswordVisibility() }
        addEnvironmentButton.setOnClickListener {
            startActivity(Intent(this, EnvironmentListActivity::class.java))
        }
        signInButton.setOnClickListener {
            disableButtonWithLoading()
            lifecycleScope.launch { sendCredentialsAndConnect() }
        }
    }

    private fun togglePasswordVisibility() {
        val isPasswordVisible =
            passwordEditText.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)

        passwordEditText.inputType = if (isPasswordVisible) {
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        showPasswordIcon.setImageResource(if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye_icon)
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun observeEnvironments() {
        viewModel.allEnvironments.observe(this, Observer { environments ->
            setupEnvironmentSpinner(environments)
        })
    }

    private fun setupEnvironmentSpinner(environments: List<Environment>) {
        val environmentNames = if (environments.isEmpty()) {
            listOf("No Environments Available")
        } else {
            environments.map { it.environment }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, environmentNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        environmentSpinner.adapter = adapter
        environmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEnvironmentId = environments.getOrNull(position)?.id
                signInButton.isEnabled = selectedEnvironmentId != null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedEnvironmentId = null
            }
        }
    }

    private suspend fun sendCredentialsAndConnect() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (username.isBlank() || password.isBlank()) {
            showToast("Please enter both username and password")
            enableButtonAndHideLoading()
            return
        }

        val selectedEnvironment = viewModel.allEnvironments.value?.find { it.id == selectedEnvironmentId }
        if (selectedEnvironment == null) {
            Log.d("HomeActivity", "No selected environment found.")
            enableButtonAndHideLoading()
            return
        }

        connectToSelectedEnvironment(selectedEnvironment, username, password)
    }
    private suspend fun connectToSelectedEnvironment(environment: Environment, username: String, password: String) {
        try {
            Log.d("HomeActivity", "Attempting to connect to the environment: $environment")
            sshManager.connect(environment) // Attempting to connect
            Log.d("HomeActivity", "Successfully connected to SSH.")

            sshManager.sshConnection?.executeCommand("$username\t$password") // Sending credentials
            sshManager.sshConnection?.getResponseFlow()?.collect { response ->
                handleSSHResponse(response)
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "SSH connection failed: ${e.message}")
            showAlert("SSH Connection failed: ${e.message}")
        } finally {
            enableButtonAndHideLoading()
        }
    }


    private suspend fun handleSSHResponse(response: String) {
        responseBuffer.append(response)
        Log.d("HomeActivity", "Response Buffer: $responseBuffer")

        if (response.contains("Invalid Login", ignoreCase = true)) {
            showErrorPopup("Incorrect password")
        } else if (response.contains("Another active session", ignoreCase = true)) {
            showSessionExistsDialog()
        } else if (response.contains("1)") && response.contains("Ctrl-")) {
            parseAndNavigate(response)
        }
    }

    private suspend fun parseAndNavigate(response: String) {
        val cleanResponse = removeEscapeSequences(response)
        val menuItems = parseMenu(cleanResponse)
        val controlCommands = parseControlCommands(cleanResponse)

        Config.menu1 = menuItems.toMutableSet()
        Config.menucommands = controlCommands.toMutableSet()

        Log.d("HomeActivity", Config.menu1.toString())
        Log.d("HomeActivity", Config.menucommands.toString())
        navigateToMenu()
    }

    private fun navigateToMenu() {
        startActivity(Intent(this@HomeActivity, Menu1Activity::class.java))
    }

    private fun showSessionExistsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Session Conflict")
            .setMessage("Another session already exists for this user. That session will end. Proceed?")
            .setPositiveButton("Yes") { _, _ -> lifecycleScope.launch { sendControlCommand("Ctrl-A") } }
            .setNegativeButton("No") { _, _ -> lifecycleScope.launch { sendControlCommand("Ctrl-W") } }
            .create()
            .show()
    }

    private fun showErrorPopup(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> lifecycleScope.launch { sendControlCommand("Ctrl-X") } }
            .create()
            .show()
    }

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-X" -> "\u0018"
            "Ctrl-A" -> "\u0001"
            "Ctrl-W" -> "\u0017"
            else -> ""
        }
        sshManager.sshConnection?.executeCommand(controlCommand)
        enableButtonAndHideLoading()
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
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> enableButtonAndHideLoading() }
            .create()
            .show()
    }
}

fun removeEscapeSequences(input: String): String {
    val regex = Regex("""[\x1B\x08]\[[;?0-9]*[a-zA-Z]""")
    return regex.replace(input, "").replace("\u0008", "")
}

fun parseControlCommands(log: String): List<String> {
    return log.lines()
        .filter { it.contains("Ctrl-") }
        .mapNotNull { line ->
            removeEscapeSequences(line)
                .replace(Regex("Oracle WMS.*"), "").trim()
        }
        .distinct()
}


fun parseMenu(input: String): List<String> {
    val regex = Regex("""\d+\)\s*(.*)""")
    return input.lines().mapNotNull { regex.find(it.trim())?.groups?.get(1)?.value }
}
