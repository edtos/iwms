package com.companyname.iwms.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.companyname.iwms.R
import com.companyname.iwms.model.Environment
import com.companyname.iwms.view.AddEditEnvironmentActivity

class EnvironmentAdapter(
    private val context: Context,
    private var environmentList: List<Environment>
) : RecyclerView.Adapter<EnvironmentAdapter.EnvironmentViewHolder>() {

    // Update the data in the adapter
    fun updateData(newList: List<Environment>) {
        environmentList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnvironmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_environment, parent, false)
        return EnvironmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnvironmentViewHolder, position: Int) {
        val environment = environmentList[position]
        holder.name.text = environment.host
        holder.description.text = environment.username

        // Navigate to AddEditEnvironmentActivity with the clicked environment data
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AddEditEnvironmentActivity::class.java).apply {
                putExtra("environmentId", environment.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = environmentList.size

    class EnvironmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.environmentName)
        val description: TextView = itemView.findViewById(R.id.environmentDescription)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)
    }
}
