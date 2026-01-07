package com.example.instagram.Adapters

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.ImageSlider
import com.example.instagram.Comments.Comment

import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Profile_Post_Adapter(
    val context: Context,
    val list: ArrayList<Uri>,
    val postmodel: ArrayList<PostModel>
) : RecyclerView.Adapter<Profile_Post_Adapter.viewholder>() {

    private var currentPlayingPosition = -1
    private var exoPlayer: ExoPlayer? = null

    private   var  Commentlist =ArrayList<PostModel>()


    companion object {
        var isMuted: Boolean = true // Static variable, accessible globally
        var playM: Boolean = false // Static variable, accessible globally

    }

    class viewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.postProfileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sample_post_profile_layout, parent, false)
        return viewholder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
        val imageUri = list[position]
        val postmodels = if (position < postmodel.size) postmodel[position] else null

//        val ref = Profile()
//
//        ref.refresh()


        Glide.with(context)
            .load(imageUri)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            if (postmodels != null) {
                showCustomAlertDialog(postmodels, position)

            } else {
                Toast.makeText(context, "Post data not available!", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun showCustomAlertDialog(postmodel: PostModel, position: Int) {
//        val ref = Profile()
//
//        ref.refresh()
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.custom_alert_view_profile_post_layout, null)
        builder.setView(dialogView)

        val username = dialogView.findViewById<TextView>(R.id.PostUserName2)
        val profileImage = dialogView.findViewById<ImageView>(R.id.PostProfile2)
        val Likes = dialogView.findViewById<ImageView>(R.id.PostLike2)
        val likeCount = dialogView.findViewById<TextView>(R.id.Likes2)
        val caption = dialogView.findViewById<TextView>(R.id.PostCaption2)
        val Images = dialogView.findViewById<ImageSlider>(R.id.PostImageSlider2)
        val Date = dialogView.findViewById<TextView>(R.id.ShowPostDate3)
        val musicName = dialogView.findViewById<TextView>(R.id.PostSong2)
        val playMusic = dialogView.findViewById<ImageView>(R.id.playMusic2)
        val stopMusic = dialogView.findViewById<ImageView>(R.id.stopMusic2)
        val comment = dialogView.findViewById<ImageView>(R.id.PostComment2)
        val commentCount = dialogView.findViewById<TextView>(R.id.Comments2)



           val uid = FirebaseAuth.getInstance().currentUser?.uid
        ShowComment(uid,postmodel.PostID , commentCount)

            if (postmodel.LikedBy?.contains(uid!!) == true)
            {
                   Likes.setImageResource(R.drawable.redheart)
            }
        else{
                Likes.setImageResource(R.drawable.heartpost)

            }
        comment.setOnClickListener {
            val bottomSheetFragment1 = Comment()


            // Fragment arguments mein AdminUID aur PostID bhejna
            val bundle1 = Bundle().apply {
                putString("AdminID9", uid)

                putString("RandomID9", postmodel.PostID)
//                Toast.makeText(context, "XXX:-${postmodel.PostRandomId}", Toast.LENGTH_SHORT).show()

            }
            bottomSheetFragment1.arguments = bundle1

            // Fragment ko show karna
            if (context is AppCompatActivity) {
                (context as AppCompatActivity).supportFragmentManager?.let {
                    bottomSheetFragment1.show(it, bottomSheetFragment1.tag)
                }
            } else {
                // Error handling in case the context is not an AppCompatActivity
                Toast.makeText(context, "Context is not valid", Toast.LENGTH_SHORT).show()
            }
        }


        if (postmodel.musicUri == null) {
            stopMusic.visibility = View.GONE
            playMusic.visibility = View.GONE
        }

                playMusicAtPosition(postmodel, position, playMusic, stopMusic ,playM)








        if (postmodel.musicUri != null)
        {
            if (ViewProfile_Adapter.playM ==true)
            {
                stopMusic.visibility = View.VISIBLE
                playMusic.visibility = View.GONE
            }

        }

        Glide.with(context)
            .load(postmodel.profileImage ?: R.drawable.user)
            .into(profileImage)
        postmodel.images?.let { Images.setImageList(it) }



        likeCount.text = postmodel.LikesCount?.toString() ?: "0"
        caption.text = postmodel.caption ?: "No Caption"
        username.text = postmodel.userName ?: "Unknown User"
        musicName.text = postmodel.Music_Name
        if (postmodel.PostDate.isNullOrEmpty()) {

            Date.visibility = View.GONE


        } else {
            Date.visibility = View.VISIBLE
            Date.text = postmodel.PostDate.toString()

        }

        if (postmodel.PostDate.isNullOrEmpty()) {
            Date.visibility = View.GONE
        } else {
            Date.visibility = View.VISIBLE
            Date.text = postmodel.PostDate.toString()
        }

        if (postmodel.PostDate.isNullOrEmpty()) {
            Date.visibility = View.GONE
        } else {
            Date.visibility = View.VISIBLE
            Date.text = postmodel.PostDate.toString()
        }


        // music part start....


            playMusic.setOnClickListener {
                if (isMuted) { // If music is paused, resume
                    isMuted = false
                    exoPlayer?.pause()

                    stopMusic.visibility = View.VISIBLE
                    playMusic.visibility = View.GONE
                    playM = true

                }
            }

            stopMusic.setOnClickListener {
                if (!isMuted) { // If music is playing, pause
                    isMuted = true

                    playM = false
                     Toast.makeText(context , "KKK" , Toast.LENGTH_SHORT).show()
                    exoPlayer?.play()

                    stopMusic.visibility = View.GONE
                    playMusic.visibility = View.VISIBLE

                        playMusicAtPosition(postmodel , position , playMusic , stopMusic , playM)




                }
            }


        val dialog = builder.create()

// Stop music completely on dialog dismiss
        dialog.setOnDismissListener {
            stopMusic()
        }

        dialog.show()

    }

    private fun playMusicAtPosition(
        postmodel: PostModel,
        position: Int,
        playMusic: ImageView,
        stopMusic: ImageView,
        playM: Boolean
    ) {
        if (isMuted) {
            if (!playM) {
                if (exoPlayer?.isPlaying != true) { // Sirf tab resume kare jab music pehle se paused ho

                    val context = context ?: return // Ensure context is not null
                    val musicUri = postmodel.musicUri ?: return // Ensure musicUri is not null

                    try {
                        exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                            val mediaItem = MediaItem.fromUri(musicUri)
                            setMediaItem(mediaItem)
                            prepare()
                            playWhenReady = true // Start playing immediately
                            play()

                            currentPlayingPosition = position
                            stopMusic.visibility = View.GONE
                            playMusic.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context , "Error Please try Again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun stopMusic() {
            exoPlayer?.let { player ->
                player.stop()
                player.release()
            }
            exoPlayer = null
            currentPlayingPosition = -1
        }

    private var isFirstLoad = true // To track the first load
    private fun ShowComment(adminUID: String?, RandomId: String?, commentCount: TextView) {
        val db = FirebaseDatabase.getInstance().getReference()
            .child("User/UserInfo/$adminUID/PostInfo/$RandomId/Comments")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isFirstLoad) {
                    Commentlist.clear() // Clear the list for the next load
                }

                // Iterate through the comments
                for (userCommentsSnapshot in snapshot.children) {
                    // Extract the user ID (key)
                    val userId = userCommentsSnapshot.key

                    // Iterate through the comments by this user
                    for (commentSnapshot in userCommentsSnapshot.children) {
                        // Extract the comment text
                        val commentText =
                            commentSnapshot.getValue(String::class.java) ?: "No comment"

                        // Add the comment and userId to the list as CommentData objects
                        if (userId != null) {
                            val post = PostModel(
                                commentText = commentText,
                                userId = userId
                            )

                            Commentlist.add(post)
                            commentCount.text = Commentlist.size.toString()



                        }
                    }
                }




                isFirstLoad = false
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }


}

