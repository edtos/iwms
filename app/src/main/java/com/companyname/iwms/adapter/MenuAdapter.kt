package com.companyname.iwms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.companyname.iwms.R
import com.google.android.material.button.MaterialButton

class MenuAdapter(private val menuList: MutableSet<String>, private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: TextView = itemView.findViewById(R.id.cardButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_card, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuList.elementAt(position)
        holder.button.text = menuItem
        holder.button.setOnClickListener {
            onClick(menuItem)
        }
    }

    override fun getItemCount(): Int = menuList.size
}
