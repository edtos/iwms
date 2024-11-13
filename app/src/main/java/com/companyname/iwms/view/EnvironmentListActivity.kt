package com.companyname.iwms.view
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
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
    private lateinit var addBtn: ImageButton  // Declare the add button
    private lateinit var backBtn: ImageButton  // Declare the back button

    // Initialize the ViewModel using the factory
    private val viewModel: EnvironmentViewModel by viewModels {
        EnvironmentViewModelFactory((application as IWMSApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_environment_list)

        // Initialize RecyclerView
        environmentRecyclerView = findViewById(R.id.environmentRecyclerView)
        environmentRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        environmentAdapter = EnvironmentAdapter(this, emptyList())
        environmentRecyclerView.adapter = environmentAdapter

        // Initialize the Add button
        addBtn = findViewById(R.id.addBtn)
        backBtn = findViewById(R.id.btnBack)

        backBtn.setOnClickListener {
            finish()
        }
        // Set up the OnClickListener for the Add button
        addBtn.setOnClickListener {
            val intent = Intent(this, AddEditEnvironmentActivity::class.java)
            startActivity(intent)
        }

        // Observe LiveData from ViewModel
        viewModel.allEnvironments.observe(this, Observer { environmentList ->
            // Update the adapter when data changes
            environmentAdapter.updateData(environmentList)
        })
    }
}

