package com.example.instagram.Comments

import PostAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instagram.Adapters.CommentAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.PostModel
import com.example.instagram.databinding.FragmentCommentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.merge


class Comment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentBinding? = null
    private val binding get() = _binding!!
    private   var  Commentlist =ArrayList<PostModel>()
    private   var  CommentSize:String = ""

    private lateinit var  adapter :CommentAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentCommentBinding.inflate(inflater, container, false)

        val adminUID = arguments?.getString("AdminID")
         val RandomId = arguments?.getString("RandomID")

        val adminUIDFromViewProfile = arguments?.getString("AdminID1")
         val RandomIdFromViewProfile  = arguments?.getString("RandomID1")

         val AdminIdIdFromNotification  = arguments?.getString("AdminID5")
         val RandomIdFromVNotification  = arguments?.getString("RandomID1")


         val RandomIdFromProfile = arguments?.getString("RandomID9")
         val AdminIDFromProfile  = arguments?.getString("AdminID9")



        if (adminUID !=null&& RandomId!=null)
        {
            ShowComment(adminUID , RandomId)
        }
        if (AdminIdIdFromNotification !=null&& RandomIdFromVNotification!=null)
        {
            ShowComment(AdminIdIdFromNotification , RandomIdFromVNotification)
        }

        if (adminUIDFromViewProfile !=null && RandomIdFromViewProfile!=null)
        {
            ShowComment(adminUIDFromViewProfile , RandomIdFromViewProfile)

        }

        if (AdminIDFromProfile !=null && RandomIdFromProfile!=null)
        {
            ShowComment(AdminIDFromProfile , RandomIdFromProfile)

        }




           adapter = CommentAdapter(requireContext() , Commentlist)
        binding.commentsRecycler.adapter = adapter
        binding.commentsRecycler.layoutManager = LinearLayoutManager(requireContext())


        binding.sendButton.setOnClickListener {
            val Mess:String = binding.commentInput.text.toString()

            if (adminUID !=null&& RandomId!=null)
            {
                SaveComment(adminUID , RandomId , Mess)
            }

            if (AdminIdIdFromNotification !=null&& RandomIdFromVNotification!=null)
            {
                SaveComment(AdminIdIdFromNotification , RandomIdFromVNotification , Mess)
            }

            if (adminUIDFromViewProfile !=null && RandomIdFromViewProfile!=null)
            {
                SaveComment(adminUIDFromViewProfile , RandomIdFromViewProfile , Mess)

            }

            if (AdminIDFromProfile !=null && RandomIdFromProfile!=null)
            {
                SaveComment(AdminIDFromProfile , RandomIdFromProfile , Mess)

            }


        }



        return binding.root


    }

    private  fun  SaveComment(adminUID: String?, RandomId: String?, Mess: String)
    {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$adminUID/PostInfo/$RandomId/Comments")
            .child(uid!!).push().setValue(Mess).addOnCompleteListener { task->
                if (task.isSuccessful)
                {
                    Toast.makeText(requireContext()  , "Successfully Addedd Comment" , Toast.LENGTH_SHORT).show()
                     binding.commentInput.text = null
                }
                else{
                    Toast.makeText(requireContext()  , "Failed ! Please Try Again" , Toast.LENGTH_SHORT).show()

                }
            }
        adapter.notifyDataSetChanged()

    }

    private var isFirstLoad = true // To track the first load

    private fun ShowComment(adminUID: String?, RandomId: String?)
    {


        val db = FirebaseDatabase.getInstance().getReference()
            .child("User/UserInfo/$adminUID/PostInfo/$RandomId/Comments")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFirstLoad) {
                    Commentlist.clear() // Clear the list only on the first load
                }

                // Iterate through the comments
                for (userCommentsSnapshot in snapshot.children) {
                    // Extract the user ID (key)
                    val userId = userCommentsSnapshot.key

                    // Iterate through the comments by this user
                    for (commentSnapshot in userCommentsSnapshot.children) {


                        // Extract the comment text
                        val commentText = commentSnapshot.getValue(String::class.java) ?: "No comment"

                        // Fetch the user data (name and profile image) and add the data to the list
                        if (userId != null) {
                            getUserData(userId, commentText) // Fetch user data separately
                        }
                    }
                }
                isFirstLoad = false
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUserData(userId: String, commentText: String) {
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$userId")
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Fetch user data
                val userName = snapshot.child("username").getValue(String::class.java)
                val profileImage = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()
                val adminUID = snapshot.child("adminUID").getValue(String::class.java)

                // Create a CommentWithUserData object with both comment and user data
                val commentData = PostModel(
                    userId = userId,
                    commentText = commentText,
                    userName = userName,
                    profileImage = profileImage,
                    AdminUID = adminUID

                )

                // Add the comment data to the list
                Commentlist.add(commentData)
                 CommentSize = Commentlist.size.toString()



                // Notify the adapter that the data has been updated
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
            }
        })
    }




}