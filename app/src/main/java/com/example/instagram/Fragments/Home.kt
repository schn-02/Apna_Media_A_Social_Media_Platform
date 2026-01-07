package com.example.instagram.Fragments

import PostAdapter
import android.app.Activity
import android.Manifest


import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.models.SlideModel
import com.example.instagram.Comments.Comment
import com.example.instagram.MainActivity
import com.example.instagram.Messenger.messenger
import com.example.instagram.Models.PostModel
import com.example.instagram.Notification.notification
import com.example.instagram.R
import com.example.instagram.databinding.FragmentHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.InputStream

class Home : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter

    private val posts = ArrayList<PostModel>() // Correct type for posts

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

         binding.shimmerHome.visibility = View.VISIBLE
        binding.PostRecycler.visibility = View.GONE
        // Initialize RecyclerView
        adapter = PostAdapter(requireContext(), posts , binding.PostRecycler )
        binding.PostRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.PostRecycler.adapter = adapter



        binding.notificationhome.setOnClickListener {
            startActivity(Intent(requireContext() , notification::class.java))
        }



       binding.message.setOnClickListener {
           startActivity(Intent(requireContext() , messenger::class.java))
       }

        // Fetch Firebase data
        fetchAllPosts()

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        // Jab fragment leave ho raha ho, reset muted flag
        PostAdapter.isMuted = false
        adapter.resetisMuted()
        adapter.stopMusic()
    }
    private fun fetchAllPosts() {
        val dbRef = FirebaseDatabase.getInstance().getReference("User/UserInfo")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return // Fragment is not attached, exit early

                posts.clear() // Clear previous posts

                for (userSnapshot in snapshot.children) {
                    val profileImage = userSnapshot.child("ProfileImage").getValue(String::class.java)
                    val AdminUID = userSnapshot.child("adminUID").getValue(String::class.java)


                    val userName = userSnapshot.child("username").getValue(String::class.java)

                    val postInfoSnapshot = userSnapshot.child("PostInfo")

                    for (postSnapshot in postInfoSnapshot.children) {
                        val caption = postSnapshot.child("Caption").getValue(String::class.java)
                        val Date = postSnapshot.child("PostDate").getValue(String::class.java)
                        val randomID = postSnapshot.child("RandomID").getValue(String::class.java)
                        val LikesCount = postSnapshot.child("LikeCount").getValue(Int::class.java)?:0

                        val likedBySnapshot = postSnapshot.child("LikedBy")
                        val notiList = mutableListOf<String>()

                        for (likedByChild in likedBySnapshot.children) {
                            val userId = likedByChild.key
                            val isLiked = likedByChild.getValue(Boolean::class.java)
                            if (isLiked == true && userId != null) {
                                notiList.add(userId)
                            }
                        }




                        // Fetch music URI as string
                        val music = postSnapshot.child("Music:-").getValue(String::class.java)
                        val musicName = postSnapshot.child("Music_Name:-").getValue(String::class.java)

                        // Check if Music is valid
                        val musicUri = if (music.isNullOrEmpty()) null else Uri.parse(music)

                        val postPicsSnapshot = postSnapshot.child("PostPics")
                        val imageList = ArrayList<SlideModel>()

                        for (image in postPicsSnapshot.children) {
                            val imageUrl = image.getValue(String::class.java)
                            if (!imageUrl.isNullOrEmpty()) {
                                imageList.add(SlideModel(imageUrl))
                            }
                        }

                        if (imageList.isNotEmpty()) {
                            val post = PostModel(
                                images = imageList,
                                caption = caption,
                                PostDate = Date,
                                userName = userName,
                                profileImage = profileImage?.toUri(),
                                PostID =  randomID,
                                AdminUID = AdminUID,
                                Notification = notiList,
                                musicUri = musicUri // Converted URI
                                , Music_Name = musicName,
                                 LikesCount = LikesCount

                            )
                            posts.add(post)
                        }
                    }
                }
                binding.PostRecycler.visibility = View.VISIBLE
                binding.shimmerHome.visibility = View.GONE

//                if (isAdded) { // Ensure fragment is still attached before updating UI
//                    val fragmentManager = parentFragmentManager
//                    val fragmentTransaction = fragmentManager.beginTransaction()
//                    fragmentTransaction.detach(this).attach(this).commit()
//
//                    adapter.notifyDataSetChanged()
//                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) { // Show error only if fragment is still attached
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottomInset = insets.systemGestureInsets.bottom
            val extraPadding =240

            view.findViewById<RecyclerView>(R.id.PostRecycler).setPadding(0, 0, 0, bottomInset + extraPadding)
            insets
        }
    }

}



