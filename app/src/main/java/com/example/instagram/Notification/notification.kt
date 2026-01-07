package com.example.instagram.Notification

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.animations.Toss
import com.example.instagram.Adapters.NotificationAdapter
import com.example.instagram.Models.PostModel

import com.example.instagram.databinding.ActivityNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class notification : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var adapter: NotificationAdapter

    private  var PostNoti=  ArrayList<PostModel>()
    private  var PostPic=  ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

         adapter = NotificationAdapter(this@notification , PostNoti)
          binding.notificationRecyler.adapter = adapter
         binding.notificationRecyler.layoutManager = LinearLayoutManager(this@notification)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().getReference("User/UserInfo/$uid/PostInfo")

             val PostIdLiked = ArrayList<String>()

        val count = ArrayList<String>()


        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (post in snapshot.children) {
                    val like = post.child("LikedBy")
                        var c =0



                    if (like.exists()) {
                        for (likeBy in like.children) {
                            if (uid ?.equals(likeBy.key) == true)
                            {
                                c++
                               continue
                            }

                            val userId = likeBy.key // Fetch the user ID

                            PostIdLiked.add(userId.toString())

                        }
                    } else {
                        Toast.makeText(this@notification, "No LikedBy data found for post: ${post.key}", Toast.LENGTH_SHORT).show()
                    }

                    count.add((like.childrenCount - c).toString())

                    val firstPic = post.child("PostPics")
                    if (firstPic.exists()) {
                        for (firstPost in firstPic.children) {
                            val firstPostValue = firstPost.value.toString()

                            // Iterate through countList
                            for (count in count) {
                                val repeatCount = count.toInt() // Convert count to integer
                                repeat(repeatCount) {
                                    PostPic.add(firstPostValue) // Add FirstPost value multiple times
                                }
                            }
                            break // Since only the first post value is needed
                        }
                    }
                }

               FetchName(PostIdLiked , PostPic)


            }



            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@notification, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun FetchName(likedPostIds: List<String> , PostPics :ArrayList<String>) {
        for (postId in likedPostIds) {

            val userRef = FirebaseDatabase.getInstance().getReference("User/UserInfo/$postId")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Print the entire snapshot to check its contents

                    val uid = FirebaseAuth.getInstance().currentUser?.uid




                    val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val profilePic = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()

                    val fp = ArrayList<Uri>()
                    // Create a new PostModel instance for each postId
                    val postModel = PostModel()
                    for (FirstPost in PostPics)
                    {
                         fp.add(FirstPost.toUri())

                    }
                       var randomId =""
                    val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/PostInfo")
                     db.addValueEventListener(object :ValueEventListener{
                         override fun onDataChange(snapshot: DataSnapshot) {

                              for (id in snapshot.children)
                              {
                                  randomId = id.child("RandomID").getValue(String::class.java).toString()


                              }


                             Toast.makeText(this@notification, "helll:-$randomId", Toast.LENGTH_SHORT).show()

                             postModel.FirstPic = fp
                             postModel.postName = userName
                             postModel.profileImage = profilePic
                             postModel.PostID = randomId


                             // Add the new PostModel to the list
                             PostNoti.add(postModel)

                             // Optionally, update the adapter here if needed
                             adapter?.notifyDataSetChanged()  // This notifies the adapter about data changes

                         }

                         override fun onCancelled(error: DatabaseError) {
                             TODO("Not yet implemented")
                         }

                     })

                    adapter?.notifyDataSetChanged()  // This notifies the adapter about data changes

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@notification, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


}






