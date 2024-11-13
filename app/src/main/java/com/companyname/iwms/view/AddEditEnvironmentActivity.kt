package com.companyname.iwms.view

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.companyname.iwms.R
import com.companyname.iwms.config.InteractiveSSH
import com.companyname.iwms.model.Environment
import com.companyname.iwms.viewmodel.EnvironmentViewModelFactory
import com.companyname.iwms.viewmodel.IWMSApplication
import com.companyname.iwms.viewmodel.EnvironmentViewModel
import kotlinx.coroutines.*

class AddEditEnvironmentActivity : AppCompatActivity() {

    private lateinit var etEnvironmentName: EditText
    private lateinit var etHost: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPort: EditText
    private lateinit var btnSubmit: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var btnDelete: ImageButton  // Declare the delete button

    private var environmentId: Int? = null // To hold the ID for editing

    private val viewModel: EnvironmentViewModel by viewModels {
        EnvironmentViewModelFactory((application as IWMSApplication).repository)
    }

    private var sshConnection: InteractiveSSH? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etEnvironmentName = findViewById(R.id.etEnvironmentName)
        etHost = findViewById(R.id.etHost)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etPort = findViewById(R.id.etPort)
        btnSubmit = findViewById(R.id.btnSubmit)
        backBtn = findViewById(R.id.btnBack)
        btnDelete = findViewById(R.id.btnDelete)

        backBtn.setOnClickListener { finish() }

        environmentId = intent.getIntExtra("environmentId", -1)
        if (environmentId != -1) {
            viewModel.getEnvironmentById(environmentId!!).observe(this) { environment ->
                environment?.let {
                    etEnvironmentName.setText(it.environment)
                    etHost.setText(it.host)
                    etUsername.setText(it.username)
                    etPassword.setText(it.password)
                    etPort.setText(it.port)

                    etEnvironmentName.isEnabled = false
                    etHost.isEnabled = false
                    etUsername.isEnabled = false
                    etPort.isEnabled = false

                    btnDelete.visibility = ImageButton.VISIBLE
                }
            }
        }

        btnSubmit.setOnClickListener {
            if (environmentId != -1) {
                updateEnvironment()
            } else {
                // Attempt to save only if SSH connection test is successful
                testSSHConnectionAndSave()
            }
        }

        btnDelete.setOnClickListener {
            if (environmentId != null) deleteEnvironment()
        }
    }

    private fun testSSHConnectionAndSave() {
        val host = etHost.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val port = etPort.text.toString().trim().toIntOrNull() ?: 22

        if (host.isEmpty() || username.isEmpty() || password.isEmpty() || port == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                sshConnection = InteractiveSSH(hostname = host, username = username, password = password, port = port)
                sshConnection?.connect()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddEditEnvironmentActivity, "SSH connection successful!", Toast.LENGTH_SHORT).show()
                    saveEnvironmentToDatabase()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddEditEnvironmentActivity, "SSH connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                println("Error connecting via SSH: ${e.message}")
            } finally {
                sshConnection?.disconnect()
            }
        }
    }

    private fun saveEnvironmentToDatabase() {
        val environmentName = etEnvironmentName.text.toString().trim()
        val host = etHost.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val port = etPort.text.toString().trim()

        if (environmentName.isEmpty() || host.isEmpty() || username.isEmpty() || password.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val environment = Environment(
            environment = environmentName,
            host = host,
            username = username,
            password = password,
            port = port
        )

        viewModel.insertEnvironment(environment)
        Toast.makeText(this, "Environment added successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateEnvironment() { /* existing update logic */ }
    private fun deleteEnvironment() { /* existing delete logic */ }
}
