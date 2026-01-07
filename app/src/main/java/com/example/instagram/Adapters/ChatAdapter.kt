package com.example.instagram.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.instagram.Chat.chatDetail
import com.example.instagram.Models.ChatModel
import com.example.instagram.R
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val context: Context, private val list: ArrayList<ChatModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val Sender_Type = 1
        const val Reciver_Type = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == Sender_Type) {
            val view = LayoutInflater.from(context).inflate(R.layout.sender, parent, false)
            SenderViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.reciver, parent, false)
            RecieverViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatModel  = list[position]

        val messageText = chatModel.ChatMessage
        val timestamp = chatModel.TimeStampe
        val date = timestamp?.let { Date(it) }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Format: 12-hour format with AM/PM
        val formattedTime = sdf.format(date)

        if (holder is SenderViewHolder) {
            holder.message.text = messageText
            holder.date.text = formattedTime
        } else if (holder is RecieverViewHolder) {
            holder.message.text = messageText
            holder.date1.text = formattedTime
        }



    }
    override fun getItemViewType(position: Int): Int {
        return if (list[position].senderMessageUId == FirebaseAuth.getInstance().currentUser?.uid) {
            Sender_Type
        } else {
            Reciver_Type
        }
    }

    override fun getItemCount(): Int = list.size

    class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.SenderMessage)
        val date: TextView = itemView.findViewById(R.id.sender_message_Time)
    }

    class RecieverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.ReciverrMessage)
        val date1: TextView = itemView.findViewById(R.id.reciver_message_TIme)
    }
}
