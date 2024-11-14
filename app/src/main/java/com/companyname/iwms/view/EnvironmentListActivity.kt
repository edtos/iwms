package com.companyname.iwms.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companyname.iwms.R
import com.companyname.iwms.adapter.EnvironmentAdapter
import com.companyname.iwms.viewmodel.EnvironmentViewModelFactory
import com.companyname.iwms.viewmodel.IWMSApplication
import com.companyname.iwms.viewmodel.EnvironmentViewModel

class EnvironmentListActivity : AppCompatActivity() {

    private lateinit var environmentRecyclerView: RecyclerView
    private lateinit var environmentAdapter: EnvironmentAdapter
    private lateinit var addBtn: Button
    private lateinit var backBtn: ImageButton
    private lateinit var noDataTextView: TextView // Declare the TextView for "No environments to display"

    private val viewModel: EnvironmentViewModel by viewModels {
        EnvironmentViewModelFactory((application as IWMSApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_environment_list)

        environmentRecyclerView = findViewById(R.id.environmentRecyclerView)
        environmentRecyclerView.layoutManager = LinearLayoutManager(this)

        environmentAdapter = EnvironmentAdapter(this, emptyList())
        environmentRecyclerView.adapter = environmentAdapter

        addBtn = findViewById(R.id.addEnvironmentButton)
        backBtn = findViewById(R.id.btnBack)
        noDataTextView = findViewById(R.id.noDataTextView) // Initialize the TextView

        backBtn.setOnClickListener {
            finish()
        }

        addBtn.setOnClickListener {
            val intent = Intent(this, AddEditEnvironmentActivity::class.java)
            startActivity(intent)
        }

        viewModel.allEnvironments.observe(this, Observer { environmentList ->
            environmentAdapter.updateData(environmentList)
            // Show/hide the "No Data" message based on the list size
            if (environmentList.isEmpty()) {
                noDataTextView.visibility = View.VISIBLE
                environmentRecyclerView.visibility = View.GONE
            } else {
                noDataTextView.visibility = View.GONE
                environmentRecyclerView.visibility = View.VISIBLE
            }
        })
    }
}