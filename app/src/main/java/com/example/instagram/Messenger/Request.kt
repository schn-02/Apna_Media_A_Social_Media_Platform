package com.example.instagram.Messenger

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram.Adapters.RequestMessengerAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.databinding.FragmentRequestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Request : Fragment() {

    private var posts = ArrayList<PostModel>()
    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding!!

    private val list = ArrayList<String>()
    private lateinit var adapter: RequestMessengerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRequestBinding.inflate(inflater, container, false)

        adapter = RequestMessengerAdapter(requireContext(), posts)

        binding.messengerRequestRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.messengerRequestRecycler.adapter = adapter

        showLoading()

        getData()

        return binding.root
    }

    private fun getData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return showEmpty()

        val db = FirebaseDatabase.getInstance()
            .getReference()
            .child("User/UserInfo/$uid/Request")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                posts.clear()
                adapter.notifyDataSetChanged()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    showEmpty()
                    return
                }

                for (req in snapshot.children) {
                    val requestUid = req.child("UID").getValue(String::class.java)
                    if (!requestUid.isNullOrEmpty()) {
                        list.add(requestUid)
                    }
                }

                if (list.isEmpty()) {
                    showEmpty()
                } else {
                    getRequestData(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showEmpty()
            }
        })
    }

    private fun getRequestData(requestUidList: ArrayList<String>) {
        posts.clear()
        adapter.notifyDataSetChanged()

        var loadedCount = 0
        val totalCount = requestUidList.size

        requestUidList.forEach { requestUid ->

            val userDb = FirebaseDatabase.getInstance()
                .getReference()
                .child("User/UserInfo/$requestUid")

            userDb.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val profileImage = userSnapshot.child("ProfileImage")
                        .getValue(String::class.java)
                        ?.toUri()

                    val username = userSnapshot.child("username")
                        .getValue(String::class.java)

                    val adminUid = userSnapshot.child("adminUID")
                        .getValue(String::class.java)

                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                    if (currentUid == null) {
                        loadedCount++
                        checkLoadingComplete(loadedCount, totalCount)
                        return
                    }

                    val requestDb = FirebaseDatabase.getInstance()
                        .getReference()
                        .child("User/UserInfo/$currentUid/Request")

                    requestDb.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(requestSnapshot: DataSnapshot) {
                            var date: String? = null

                            for (req in requestSnapshot.children) {
                                val uid = req.child("UID").getValue(String::class.java)
                                if (uid == requestUid) {
                                    date = req.child("Date").getValue(String::class.java)
                                    break
                                }
                            }

                            val post = PostModel(
                                profileImage = profileImage,
                                userName = username,
                                PostDate = date,
                                AdminUID = adminUid
                            )

                            posts.add(post)
                            adapter.notifyItemInserted(posts.size - 1)

                            loadedCount++
                            checkLoadingComplete(loadedCount, totalCount)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            loadedCount++
                            checkLoadingComplete(loadedCount, totalCount)
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    checkLoadingComplete(loadedCount, totalCount)
                }
            })
        }
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
        binding.requestLoader.visibility = View.VISIBLE
        binding.noRequestText.visibility = View.GONE
        binding.messengerRequestRecycler.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.requestLoader.visibility = View.GONE
        binding.noRequestText.visibility = View.VISIBLE
        binding.messengerRequestRecycler.visibility = View.GONE
    }

    private fun showData() {
        binding.requestLoader.visibility = View.GONE
        binding.noRequestText.visibility = View.GONE
        binding.messengerRequestRecycler.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}