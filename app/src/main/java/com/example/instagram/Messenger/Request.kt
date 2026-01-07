package com.example.instagram.Messenger

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.animations.Toss
import com.example.instagram.Adapters.RequestMessengerAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.databinding.FragmentRequestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Request : Fragment() {
    private   var  posts =ArrayList<PostModel>()
    private var _binding: FragmentRequestBinding? = null
    val list = ArrayList<String>()

    private lateinit var adapter: RequestMessengerAdapter
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRequestBinding.inflate(inflater, container, false)
        adapter = RequestMessengerAdapter(requireContext() , posts)

        binding.messengerRequestRecycler.adapter = adapter
        binding.messengerRequestRecycler.layoutManager = LinearLayoutManager(requireContext())

        getData()


        return  binding.root
    }

    private  fun getData()
    {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/Request")
        db.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (req in snapshot.children)
                {
                     val l = req.child("UID").getValue(String::class.java)
                    list.add(l.toString())
                }


            getRequestData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun getRequestData(list: ArrayList<String>) {
        if (list.isNotEmpty()) {
            posts.clear()
            for (li in list) {
                val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$li")
                db.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val ProfileImage = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()
                        val username = snapshot.child("username").getValue(String::class.java)
                        val Adminuid = snapshot.child("adminUID").getValue(String::class.java)
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        val db2 = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/Request")

                        db2.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var date: String? = null
                                for (req in snapshot.children) {
                                    date = req.child("Date").getValue(String::class.java)
                                }

                                // PostModel ko date set hone ke baad hi add karein
                                val post = PostModel(
                                    profileImage = ProfileImage,
                                    userName = username,
                                    PostDate = date,
                                    AdminUID = Adminuid
                                )
                                posts.add(post)
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle error here
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error here
                    }
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){

            startActivity(Intent(requireContext() , MainActivity::class.java))
            requireActivity().finish()
        }
    }


}