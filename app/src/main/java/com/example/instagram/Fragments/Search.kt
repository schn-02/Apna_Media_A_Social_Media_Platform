package com.example.instagram.Fragments

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram.Adapters.SearchAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.ViewProfile.ViewProfile
import com.example.instagram.databinding.FragmentAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Search : Fragment() {

    private var posts = ArrayList<PostModel>()
    private var AddUsers = ArrayList<String>()
    private var AddUsers2 = ArrayList<String>()
    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("ViewBinding is not initialized")

    private lateinit var adapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddBinding.inflate(inflater, container, false)

        adapter = SearchAdapter(requireContext(), posts, AddUsers)
        binding.messengerAddRecycler.adapter = adapter
        binding.messengerAddRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Load AddUsers first, then trigger getData() from within AddUser()
        AddUser()
        ReuestData()

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(Text: CharSequence?, p1: Int, p2: Int, p3: Int)
            {

//                Toast.makeText(requireContext() , "XXX:-$Text" , Toast.LENGTH_SHORT).show()
                adapter.filter?.filter(Text.toString().trim())
            }
            override fun afterTextChanged(p0: Editable?) {}
        })

        return binding.root
    }

    private fun getData() {
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val tempList = ArrayList<PostModel>()  // Temporary list taaki async issues na aaye

                for (data in snapshot.children) {
                    val AdminUid = data.child("adminUID").getValue(String::class.java)

                    //checking....  request accept wale show na ho
                    if (AdminUid?.equals(uid) == false && !AddUsers.contains(AdminUid) && !AddUsers2.contains(AdminUid))
                    {
                        val profileImage = data.child("ProfileImage").getValue(String::class.java)?.toUri()
                        val userName = data.child("username").getValue(String::class.java)

                        val post = PostModel(
                            profileImage = profileImage,
                            userName = userName,
                            AdminUID = AdminUid
                        )
                        tempList.add(post)
                    }
                }

                posts.clear()
                posts.addAll(tempList) // ✅ Properly list update karo

                adapter.original = ArrayList(posts)  // ✅ Original list bhi update ho
                adapter.differ.submitList(ArrayList(posts))  // ✅ List ka copy pass karo

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle Error
            }
        })
    }


    private fun AddUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/RequestAccept")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                AddUsers.clear()
                for (userSnapshot in snapshot.children) {
                    userSnapshot.getValue(String::class.java)?.let { AddUsers.add(it) }
                }
                getData() // Trigger data fetch after AddUsers is updated
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private  fun ReuestData()
    {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/Request")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                AddUsers2.clear()
                for (userSnapshot in snapshot.children) {
                    val id = userSnapshot.child("UID").getValue(String::class.java).toString()
                       AddUsers2.add(id)
                }
                getData() // Trigger data fetch after AddUsers is updated
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? ViewProfile)?.showMainUI()
        _binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){

            startActivity(Intent(requireContext() , MainActivity::class.java))
            requireActivity().finish()
        }
    }
}