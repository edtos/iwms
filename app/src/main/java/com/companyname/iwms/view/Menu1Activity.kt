package com.companyname.iwms.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companyname.iwms.R
import com.companyname.iwms.adapter.MenuAdapter
import com.companyname.iwms.config.Config

class Menu1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu1)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        println("LIST "+Config.menu1.toString())
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = MenuAdapter(Config.menu1) { menuItem ->
            Toast.makeText(this, "Clicked: $menuItem", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
    }
}