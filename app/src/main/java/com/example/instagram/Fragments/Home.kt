package com.example.instagram.Fragments

import PostAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.instagram.MainActivity
import com.example.instagram.Messenger.messenger
import com.example.instagram.Models.PostModel
import com.example.instagram.Notification.notification
import com.example.instagram.R
import com.example.instagram.databinding.FragmentHomeBinding
import com.google.firebase.database.*

class Home : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter
    private val posts = ArrayList<PostModel>()

    private var postListener: ValueEventListener? = null
    private var dbRef: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        adapter = PostAdapter(requireContext(), posts, binding.PostRecycler)

        binding.PostRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.PostRecycler.adapter = adapter

        binding.notificationhome.setOnClickListener {
            startActivity(Intent(requireContext(), notification::class.java))
        }

        binding.message.setOnClickListener {
            startActivity(Intent(requireContext(), messenger::class.java))
        }

        showLoading()
        fetchAllPosts()

        return binding.root
    }

    private fun fetchAllPosts() {
        dbRef = FirebaseDatabase.getInstance()
            .getReference("User/UserInfo")

        postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                posts.clear()

                for (userSnapshot in snapshot.children) {
                    val profileImage = userSnapshot.child("ProfileImage").getValue(String::class.java)
                    val adminUID = userSnapshot.child("adminUID").getValue(String::class.java)
                    val userName = userSnapshot.child("username").getValue(String::class.java)

                    val postInfoSnapshot = userSnapshot.child("PostInfo")

                    for (postSnapshot in postInfoSnapshot.children) {
                        val caption = postSnapshot.child("Caption").getValue(String::class.java)
                        val postDate = postSnapshot.child("PostDate").getValue(String::class.java)
                        val randomID = postSnapshot.child("RandomID").getValue(String::class.java)
                        val likesCount = postSnapshot.child("LikeCount").getValue(Int::class.java) ?: 0

                        val likedBySnapshot = postSnapshot.child("LikedBy")
                        val notiList = mutableListOf<String>()

                        for (likedByChild in likedBySnapshot.children) {
                            val userId = likedByChild.key
                            val isLiked = likedByChild.getValue(Boolean::class.java)

                            if (isLiked == true && userId != null) {
                                notiList.add(userId)
                            }
                        }

                        val music = postSnapshot.child("Music:-").getValue(String::class.java)
                        val musicName = postSnapshot.child("Music_Name:-").getValue(String::class.java)
                        val musicUri = if (music.isNullOrEmpty()) null else Uri.parse(music)

                        val postPicsSnapshot = postSnapshot.child("PostPics")
                        val imageList = ArrayList<SlideModel>()

                        for (image in postPicsSnapshot.children) {
                            val imageUrl = image.getValue(String::class.java)

                            if (!imageUrl.isNullOrEmpty()) {
                                imageList.add(
                                    SlideModel(
                                        imageUrl,
                                        ScaleTypes.CENTER_CROP
                                    )
                                )
                            }
                        }

                        if (imageList.isNotEmpty()) {
                            val post = PostModel(
                                images = imageList,
                                caption = caption,
                                PostDate = postDate,
                                userName = userName,
                                profileImage = profileImage?.toUri(),
                                PostID = randomID,
                                AdminUID = adminUID,
                                Notification = notiList,
                                musicUri = musicUri,
                                Music_Name = musicName,
                                LikesCount = likesCount
                            )

                            posts.add(post)
                        }
                    }
                }

                adapter.notifyDataSetChanged()

                if (posts.isEmpty()) {
                    showEmpty()
                } else {
                    showData()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return

                showEmpty()

                Toast.makeText(
                    requireContext(),
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dbRef?.addValueEventListener(postListener!!)
    }

    private fun showLoading() {
        binding.shimmerHome.visibility = View.VISIBLE
        binding.shimmerHome.bringToFront()
        binding.shimmerHome.startShimmer()

        binding.PostRecycler.visibility = View.GONE
        binding.noPostText.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.shimmerHome.stopShimmer()
        binding.shimmerHome.visibility = View.GONE

        binding.PostRecycler.visibility = View.GONE
        binding.noPostText.visibility = View.VISIBLE
        binding.noPostText.bringToFront()
    }

    private fun showData() {
        binding.shimmerHome.stopShimmer()
        binding.shimmerHome.visibility = View.GONE

        binding.noPostText.visibility = View.GONE
        binding.PostRecycler.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()

        if (_binding != null) {
            binding.shimmerHome.stopShimmer()
        }

        if (::adapter.isInitialized) {
            PostAdapter.isMuted = false
            adapter.resetisMuted()
            adapter.stopMusic()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bottomInset = insets.systemGestureInsets.bottom
            val extraPadding = 240

            view.findViewById<RecyclerView>(R.id.PostRecycler)
                .setPadding(0, 0, 0, bottomInset + extraPadding)

            insets
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        postListener?.let { listener ->
            dbRef?.removeEventListener(listener)
        }

        postListener = null
        dbRef = null
        _binding = null
    }
}