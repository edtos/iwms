package com.companyname.iwms.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            scope.launch {
                sendControlCommand("Ctrl-X")
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }

        upIcon.setOnClickListener {
            scope.launch {
                sendControlCommand("Ctrl-U")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Menu1Activity, "Up command sent (Ctrl+U)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        downIcon.setOnClickListener {
            scope.launch {
                sendControlCommand("Ctrl-D")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Menu1Activity, "Down command sent (Ctrl+D)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        moreIcon.setOnClickListener {
            showMenuBottomSheet()
        }
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

    private suspend fun sendControlCommand(command: String) {
        val controlCommand = when (command) {
            "Ctrl-X" -> "\u0018" // ASCII code for Ctrl+X
            "Ctrl-U" -> "\u0015" // ASCII code for Ctrl+U
            "Ctrl-D" -> "\u0004" // ASCII code for Ctrl+D
            else -> ""
        }
        sshManager.sshConnection?.executeCommand(controlCommand)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
