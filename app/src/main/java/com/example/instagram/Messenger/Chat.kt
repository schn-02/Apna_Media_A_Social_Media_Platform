package com.example.instagram.Messenger

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram.Adapters.ChatFragmentAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.ViewProfile.ViewProfile
import com.example.instagram.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Chat : Fragment() {

    private   var  posts =ArrayList<PostModel>()
    private   var  list =ArrayList<String>()

    private var _binding: FragmentChatBinding? = null
    private lateinit var   adapter: ChatFragmentAdapter
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChatBinding.inflate(inflater, container, false)
         adapter = ChatFragmentAdapter(requireContext() , posts)
        binding.messengerChatRecycler.adapter = adapter
        binding.messengerChatRecycler.layoutManager = LinearLayoutManager(requireContext())

        getData()

        binding.searchChat.addTextChangedListener( object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(Text: CharSequence?, p1: Int, p2: Int, p3: Int) {

                adapter.filter?.filter(Text.toString().trim())

            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        getListData(list)
        return binding.root
    }

    private fun getData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/RequestAccept")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (req in snapshot.children) {
                    list.add(req.value.toString())
                }
                // Data fetch ho gaya, ab list ko process karo
                getListData(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation error here
            }
        })
    }
    private fun getListData(list: ArrayList<String>) {
        posts.clear()

        for (li in list) {
            val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$li")
            db.get().addOnSuccessListener { snapshot ->
                val ProfileImage = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()
                val ProfileName = snapshot.child("name").getValue(String::class.java)
                val Adminuid = snapshot.child("adminUID").getValue(String::class.java)

                // Callback function use kar rahe hain
                getLastMessage(Adminuid) { lastMessage ->
                    val post = PostModel(
                        profileImage = ProfileImage,
                        userName = ProfileName,
                        AdminUID = Adminuid,
                        LastMessage = lastMessage
                    )

                    posts.add(post)

                    adapter.original = ArrayList(posts)  // ✅ Original list bhi update ho
                    adapter.differ.submitList(ArrayList(posts))  // ✅ List ka copy pass karo

                    adapter.notifyDataSetChanged()
                }
            }.addOnFailureListener {
                // Handle error here
            }
        }
    }

    private fun getLastMessage(adminUID: String?, callback: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ChatRoom = uid + adminUID

        val db3 = FirebaseDatabase.getInstance().getReference().child("Chats/$ChatRoom")
        db3.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessageList = mutableListOf<String>()
                for (l in snapshot.children) {
                    val mess = l.child("chatMessage").getValue(String::class.java)
                    mess?.let { lastMessageList.add(it) }
                }

                val lastM = lastMessageList.lastOrNull() ?: " "
                callback(lastM) // Yeh ensure karega ki value milne ke baad hi callback chale
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Error fetching message")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = activity as? ViewProfile
        activity?.showMainUI() // Activity me UI restore karne ke liye method call kar rahe hain
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){

            startActivity(Intent(requireContext() , MainActivity::class.java))
            requireActivity().finish()
        }
    }

}