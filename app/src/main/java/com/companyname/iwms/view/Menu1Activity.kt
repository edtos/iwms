package com.companyname.iwms.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companyname.iwms.R
import com.companyname.iwms.adapter.MenuAdapter
import com.companyname.iwms.config.Config
import com.companyname.iwms.config.SSHConnectionManager
import com.companyname.iwms.viewmodel.IWMSApplication
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*

class Menu1Activity : AppCompatActivity() {

    private val sshManager: SSHConnectionManager by lazy {
        (application as IWMSApplication).sshConnectionManager
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu1)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = MenuAdapter(Config.menu1) { menuItem ->
            Toast.makeText(this, "Clicked: $menuItem", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        val backIcon = findViewById<ImageView>(R.id.backIcon)
        val upIcon = findViewById<ImageView>(R.id.upIcon)
        val downIcon = findViewById<ImageView>(R.id.downIcon)
        val moreIcon = findViewById<ImageView>(R.id.moreIcon)

        backIcon.setOnClickListener {
            lifecycleScope.launch{
                sendControlCommand("Ctrl-X")
            }
        }

        upIcon.setOnClickListener {
            lifecycleScope.launch {
                sendControlCommand("Ctrl-U") // Send Ctrl-U and handle response
            }
        }

        downIcon.setOnClickListener {
            lifecycleScope.launch {
                sendControlCommand("Ctrl-D") // Send Ctrl-D and handle response
            }
        }

        moreIcon.setOnClickListener {
            showMenuBottomSheet()
        }
    }
    private suspend fun processSessionCloseResponse(response: String) {
        Log.e("Menu1Activity", "Response: $response")
        // If the response indicates the session is closed or user is logged out
        if (response.contains("Ctrl-X: Exit App", ignoreCase = true)) {
            // Handle session closure
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Menu1Activity, "Session closed", Toast.LENGTH_SHORT).show()
                // Optionally navigate back to HomeActivity or perform cleanup
                navigateToHome()
            }
        } else {
            // Handle any other relevant response, if needed
            Log.e("Menu1Activity", "Response after Ctrl-X: $response")
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish() // Ensure the current activity is removed from the stack
    }


    private fun showMenuBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_menu, null)

        val bottomRecyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.bottomRecyclerView)
        bottomRecyclerView.layoutManager = LinearLayoutManager(this)
        bottomRecyclerView.adapter = MenuAdapter(Config.menucommands) { menuItem ->
            Toast.makeText(this, "Menu item clicked: $menuItem", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss() // Close the bottom sheet after selection
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }


    private suspend fun processMenuResponse(command: String, response: String) {


        Log.e("MENU",response)
        val cleanResponse = removeEscapeSequences(response)

        val newMenuItems = parseMenu(cleanResponse)
        val newControlCommands = parseControlCommands(cleanResponse)

        withContext(Dispatchers.Main) {
            if (newMenuItems.isNotEmpty()) {
                Config.menu1 = newMenuItems.toMutableSet()
                Config.menucommands = newControlCommands.toMutableSet()

                val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
                val adapter = MenuAdapter(Config.menu1) { menuItem ->
                    Toast.makeText(this@Menu1Activity, "Clicked: $menuItem", Toast.LENGTH_SHORT).show()
                }
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(this@Menu1Activity, "No menu items found in response", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-X" -> "\u0018"
            else -> command
        }
        sshManager.sshConnection?.executeCommand(controlCommand)
        delay(1000)
        if(command.equals("Ctrl-X")){
            val intent = Intent(this@Menu1Activity, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish() // Ensure the current activity is removed from the stack
        }
        sshManager.sshConnection?.getResponseFlow()?.collect { response ->
            processMenuResponse(command,response)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish() // Ensure the current activity is removed from the stack
    }
}
