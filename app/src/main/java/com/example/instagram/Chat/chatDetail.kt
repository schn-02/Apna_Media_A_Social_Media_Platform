package com.example.instagram.Chat

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.instagram.Adapters.ChatAdapter
import com.example.instagram.Models.ChatModel
import com.example.instagram.R
import com.example.instagram.databinding.ActivityChatDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class chatDetail : AppCompatActivity() {
    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var adapter: ChatAdapter
    private var list = ArrayList<ChatModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(this, list)
        binding.chatRecycler1.adapter = adapter
        binding.chatRecycler1.layoutManager = LinearLayoutManager(this@chatDetail)

        val adminUID = intent.getStringExtra("ChatDetail")
        getData(adminUID)

        val senderID = FirebaseAuth.getInstance().currentUser?.uid
        val senderRoom = senderID + adminUID
        val recieverRoom = adminUID + senderID

        MessaageData(senderRoom)

        binding.send.setOnClickListener {
            send(senderID, senderRoom, recieverRoom)
        }
    }

    private fun getData(adminUID: String?) {
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$adminUID")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ProfileImage = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()
                val username = snapshot.child("name").getValue(String::class.java)

                Glide.with(this@chatDetail)
                    .load(ProfileImage)
                    .into(binding.chatprofilePic)

                binding.Chatusername.text = username
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun send(senderID: String?, senderRoom: String, recieverRoom: String) {
        val Message = binding.messageChats.text

        val post = ChatModel(
            senderMessageUId = senderID,
            ChatMessage = Message.toString(),
            TimeStampe = Date().time
        )

        FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).push()
            .setValue(post)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    send2(post, recieverRoom)
                }
            }
    }

    private fun send2(post: ChatModel, recieverRoom: String) {
        FirebaseDatabase.getInstance().getReference().child("Chats").child(recieverRoom).push()
            .setValue(post).addOnCompleteListener { taskId ->
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
            }
    }

    private fun MessaageData(senderRoom: String) {
        val db = FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom)
        db.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                list.clear()

                for (l in snapshot.children) {

                     val mess = l.child("chatMessage").getValue(String::class.java)
                     val senderMessageUId = l.child("senderMessageUId").getValue(String::class.java)
                     val timeStamp = l.child("timeStampe").getValue(Long::class.java)

                    val chat = ChatModel(ChatMessage = mess
                        , senderMessageUId = senderMessageUId,
                        TimeStampe = timeStamp
                    )
                    list.add(chat)
                    binding.messageChats.text = null



                }

                adapter.notifyDataSetChanged() // Update RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })

    }
}
