package com.example.prueba.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prueba.items.NotificationItem
import com.example.prueba.R


class NotificationAdapter(private val notificationList: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val timeAgoTextView: TextView = itemView.findViewById(R.id.timeAgoTextView)
        val readStatusImageView: ImageView = itemView.findViewById(R.id.unread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_notification, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentItem = notificationList[position]

        // Cambiar la visibilidad de la ImageView según el estado de lectura
        if (currentItem.isRead) {
            holder.readStatusImageView.visibility = View.GONE
        } else {
            holder.readStatusImageView.visibility = View.VISIBLE
        }

        // Resto de la lógica para establecer otros datos en las vistas
        holder.titleTextView.text = currentItem.title
        holder.messageTextView.text = currentItem.message
        holder.timeAgoTextView.text = currentItem.timeAgo
    }

    override fun getItemCount() = notificationList.size
}
