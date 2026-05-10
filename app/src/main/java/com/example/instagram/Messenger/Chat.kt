package com.example.instagram.Messenger

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram.Adapters.ChatFragmentAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.ViewProfile.ViewProfile
import com.example.instagram.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Chat : Fragment() {

    private var posts = ArrayList<PostModel>()
    private var list = ArrayList<String>()

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatFragmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        adapter = ChatFragmentAdapter(requireContext(), posts)

        binding.messengerChatRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.messengerChatRecycler.adapter = adapter

        showLoading()
        setupSearch()
        getData()

        return binding.root
    }

    private fun setupSearch() {
        binding.searchChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
                adapter.filter?.filter(text.toString().trim())
            }

            override fun afterTextChanged(editable: Editable?) {
            }
        })
    }

    private fun getData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            showEmpty()
            return
        }

        val db = FirebaseDatabase.getInstance()
            .getReference()
            .child("User/UserInfo/$uid/RequestAccept")

        db.get()
            .addOnSuccessListener { snapshot ->
                list.clear()
                posts.clear()

                adapter.original = ArrayList(posts)
                adapter.differ.submitList(ArrayList(posts))
                adapter.notifyDataSetChanged()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                for (req in snapshot.children) {
                    val friendUid = req.value?.toString()
                    if (!friendUid.isNullOrEmpty()) {
                        list.add(friendUid)
                    }
                }

                if (list.isEmpty()) {
                    showEmpty()
                } else {
                    getListData(list)
                }
            }
            .addOnFailureListener {
                showEmpty()
            }
    }

    private fun getListData(friendUidList: ArrayList<String>) {
        posts.clear()

        var loadedCount = 0
        val totalCount = friendUidList.size

        friendUidList.forEach { friendUid ->

            val db = FirebaseDatabase.getInstance()
                .getReference()
                .child("User/UserInfo/$friendUid")

            db.get()
                .addOnSuccessListener { snapshot ->

                    val profileImage = snapshot.child("ProfileImage")
                        .getValue(String::class.java)
                        ?.toUri()

                    val profileName = snapshot.child("name")
                        .getValue(String::class.java)

                    val adminUid = snapshot.child("adminUID")
                        .getValue(String::class.java)

                    getLastMessage(adminUid) { lastMessage ->

                        val post = PostModel(
                            profileImage = profileImage,
                            userName = profileName,
                            AdminUID = adminUid,
                            LastMessage = lastMessage
                        )

                        posts.add(post)

                        adapter.original = ArrayList(posts)
                        adapter.differ.submitList(ArrayList(posts))
                        adapter.notifyDataSetChanged()

                        loadedCount++
                        checkLoadingComplete(loadedCount, totalCount)
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    checkLoadingComplete(loadedCount, totalCount)
                }
        }
    }

    private fun getLastMessage(adminUID: String?, callback: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null || adminUID == null) {
            callback("No messages yet")
            return
        }

        val chatRoom1 = uid + adminUID
        val chatRoom2 = adminUID + uid

        val db1 = FirebaseDatabase.getInstance()
            .getReference()
            .child("Chats/$chatRoom1")

        db1.get()
            .addOnSuccessListener { snapshot1 ->

                if (snapshot1.exists() && snapshot1.childrenCount > 0L) {
                    val lastMessage = getLastMessageFromSnapshot(snapshot1)
                    callback(lastMessage)
                } else {
                    val db2 = FirebaseDatabase.getInstance()
                        .getReference()
                        .child("Chats/$chatRoom2")

                    db2.get()
                        .addOnSuccessListener { snapshot2 ->
                            val lastMessage = if (snapshot2.exists() && snapshot2.childrenCount > 0L) {
                                getLastMessageFromSnapshot(snapshot2)
                            } else {
                                "No messages yet"
                            }

                            callback(lastMessage)
                        }
                        .addOnFailureListener {
                            callback("No messages yet")
                        }
                }
            }
            .addOnFailureListener {
                callback("No messages yet")
            }
    }

    private fun getLastMessageFromSnapshot(snapshot: com.google.firebase.database.DataSnapshot): String {
        val lastMessageList = mutableListOf<String>()

        for (messageSnapshot in snapshot.children) {
            val message = messageSnapshot.child("chatMessage").getValue(String::class.java)
            if (!message.isNullOrEmpty()) {
                lastMessageList.add(message)
            }
        }

        return lastMessageList.lastOrNull() ?: "No messages yet"
    }

    private fun checkLoadingComplete(loadedCount: Int, totalCount: Int) {
        if (loadedCount >= totalCount) {
            if (posts.isEmpty()) {
                showEmpty()
            } else {
                showData()
            }
        }
    }

    private fun showLoading() {
        binding.chatLoader.visibility = View.VISIBLE
        binding.noChatText.visibility = View.GONE
        binding.messengerChatRecycler.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.chatLoader.visibility = View.GONE
        binding.noChatText.visibility = View.VISIBLE
        binding.messengerChatRecycler.visibility = View.GONE
    }

    private fun showData() {
        binding.chatLoader.visibility = View.GONE
        binding.noChatText.visibility = View.GONE
        binding.messengerChatRecycler.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val activity = activity as? ViewProfile
        activity?.showMainUI()

        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }
}