package com.example.instagram.ViewProfile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.models.SlideModel
import com.example.instagram.Adapters.ViewProfile_Adapter
import com.example.instagram.Fragments.Search
import com.example.instagram.Messenger.Chat
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.databinding.ActivityViewProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ViewProfile : AppCompatActivity() {
    lateinit var  binding :ActivityViewProfileBinding


  lateinit  var username:String
  lateinit  var adapter : ViewProfile_Adapter
  lateinit  var  name:String
  lateinit  var  adminuid:String
    private var FriendsCount:String = "0"

    private var  postmodel = ArrayList<PostModel>()

    private lateinit var yourValueEventListener: ValueEventListener
    private  val Postt = ArrayList<Uri>()
  lateinit  var  Bio:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)

        binding = ActivityViewProfileBinding.inflate(layoutInflater)
       setContentView(binding.root)





        binding.MessageViewProfile.setOnClickListener{
            val add = Search()
            binding.toolbar3.visibility = View.GONE
            binding.profileViewprofile.visibility = View.GONE
            binding.Post.visibility = View.GONE
            binding.ower.visibility = View.GONE
            binding.textView10.visibility = View.GONE
            binding.textView9.visibility = View.GONE
            binding.ProfileName.visibility = View.GONE
            binding.BioProfile.visibility = View.GONE
            binding.Follow.visibility = View.GONE
            binding.MessageViewProfile.visibility = View.GONE
            binding.textView12.visibility = View.GONE
            binding.recyclerProfileView.visibility = View.GONE
            binding.Follow.visibility = View.GONE

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_ViewProfile, add) // `fragment_container` activity XML me define hona chahiye
                .addToBackStack(null) // Back button se wapas aane ke liye
                .commit()


        }


        binding.shimmerViewProfile.visibility = View.VISIBLE
        binding.recyclerProfileView.visibility = View.GONE

        adapter = ViewProfile_Adapter(this , Postt , postmodel)

        binding.recyclerProfileView.adapter = adapter
        binding.recyclerProfileView.layoutManager = GridLayoutManager(this , 3)

         val recieveData = intent.getStringExtra("UID")
         val recieveDataFromAdd = intent.getStringExtra("UIDadd")
         val recieveDataFromRequest = intent.getStringExtra("UIRequest")
         val recieveDataFromChat = intent.getStringExtra("UIDChat")
         val recieveDataFromCommment = intent.getStringExtra("commentUID")
//        Toast.makeText(this , "pii:- $Postt", Toast.LENGTH_SHORT).show()
          var a= 0
          var b= 0
          var c= 0
          var d= 0
          var e= 0

        if (recieveData != null) {
            a=1
            fetchData(recieveData)
            Friends(recieveData)

        }
         if (recieveDataFromAdd != null) {
             b=1
            fetchData(recieveDataFromAdd)
             Friends(recieveDataFromAdd)

         }
        if (recieveDataFromRequest != null) {
            c=1
            fetchData(recieveDataFromRequest)
            Friends(recieveDataFromRequest)

        }
        if (recieveDataFromChat != null) {
            d=1
            fetchData(recieveDataFromChat)
            Friends(recieveDataFromChat)

        }
        if (recieveDataFromCommment != null) {
            e=1
            fetchData(recieveDataFromCommment)
            Friends(recieveDataFromCommment)

        }





    }

    private fun fetchData(Data: String) {

        val db = FirebaseDatabase.getInstance().getReference().child("User").child("UserInfo").child(Data)
        yourValueEventListener = object : ValueEventListener {
            override fun onDataChange(value: DataSnapshot) {
                postmodel.clear() // Prevent duplicate entries
                Postt.clear()

                 val profileImage = value.child("ProfileImage").getValue(String::class.java)?.toUri()
                   adminuid = value.child("adminUID").getValue(String::class.java)?.toUri().toString()
                username = value.child("username").getValue(String::class.java).toString()?:""
                name = value.child("name").getValue(String::class.java)?: ""
                Bio = value.child("bio").getValue(String::class.java)?:""




                val postInfo = value.child("PostInfo")
                val postCount = postInfo.childrenCount

                for (pi in postInfo.children) {

                    val postss = PostModel()  // Create a new instance for each post
                    val likeList = ArrayList<String>()

                    val caption = pi.child("Caption").getValue(String::class.java)
                    val likeCount = pi.child("LikeCount").getValue(Int::class.java)
                    val Date = pi.child("PostDate").getValue(String::class.java)
                    val MusicName = pi.child("Music_Name:-").getValue(String::class.java)
                    val music = pi.child("Music:-").getValue(String::class.java)

                    val  randomID = pi.child("RandomID").getValue(String::class.java).toString()



                    val musicUri = if (music.isNullOrEmpty()) null else Uri.parse(music)

                    // Get the PostPics node for each post
                    val postPics = pi.child("PostPics")
                    val imageList = ArrayList<SlideModel>()

                    val LikedBy = pi.child("LikedBy")

                    if (LikedBy.exists())
                    {
                        for (l in LikedBy.children)
                        {
                            val LikeBYY = l.key
                            LikeBYY?.let { likeList.add(it) }
                        }

                    }

                    if (postPics.exists())
                    {
                        for (fp in postPics.children) {
                            val postPicUrl = fp.getValue(String::class.java)
                            if (postPicUrl != null) {
                                val uri = Uri.parse(postPicUrl)  // Convert the URL to Uri
                                Postt.add(uri)  // Add to Postt list
                                break
                            }
                        }
                    }
                    for (image in postPics.children) {
                        val imageUrl = image.getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            imageList.add(SlideModel(imageUrl))
                        }
                    }

                    postss.profileImage = profileImage
                    postss.images = imageList
                    postss.userName = username
                    postss.PostCount = postCount.toInt()
                    postss.PostDate = Date
                    postss.AdminUID = adminuid
                    postss.PostID = randomID
                    postss.caption = caption
                    postss.Music_Name = MusicName
                    postss.LikedBy =likeList
                    postss.musicUri = musicUri
                    postss.LikesCount = likeCount

                    binding.Post.text = postss.PostCount.toString()


                    postmodel.add(postss)
                }
                binding.shimmerViewProfile.visibility = View.GONE
                binding.recyclerProfileView.visibility = View.VISIBLE


                 // Add each post to the list

                // Update UI elements
                binding.ProfileName.text = name
                binding.ProfileUsername.text = username

                if (Bio!="Bio")
                {
                    binding.BioProfile.text = Bio
                }


                if (!isFinishing && !isDestroyed)
                {
                    Glide.with(this@ViewProfile)
                        .load(profileImage)
                        .placeholder(R.drawable.userstory)
                        .into(binding.profileViewprofile)

                }

                // Notify adapter about data change
                adapter.notifyDataSetChanged()  // Refresh the RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
            }
        }
        db.addValueEventListener(yourValueEventListener)
    }
    override fun onDestroy() {
        super.onDestroy()
        // Remove Firebase listeners
        val db = FirebaseDatabase.getInstance().getReference().child("User").child("UserInfo")
        db.removeEventListener(yourValueEventListener)

    }
    private fun openFragment() {
        val ChatFragement = Chat()

        // Sab views hide kar rahe hain
        hideMainUI()

        // Fragment ko replace kar rahe hain
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_ViewProfile, ChatFragement)
            .addToBackStack(null) // Back button se wapas aane ke liye
            .commit()
    }
    private fun openFragment2() {
        val ChatFragement = Search()

        // Sab views hide kar rahe hain
        hideMainUI()

        // Fragment ko replace kar rahe hain
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_ViewProfile, ChatFragement)
            .addToBackStack(null) // Back button se wapas aane ke liye
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Ensure ClickListener works after coming back from Fragment
        binding.MessageViewProfile.setOnClickListener {
            openFragment()
        }

        binding.Follow.setOnClickListener {
            openFragment2()
        }

    }

    fun hideMainUI() {
        binding.toolbar3.visibility = View.GONE
        binding.profileViewprofile.visibility = View.GONE
        binding.Post.visibility = View.GONE
        binding.ower.visibility = View.GONE
        binding.textView10.visibility = View.GONE
        binding.textView9.visibility = View.GONE
        binding.ProfileName.visibility = View.GONE
        binding.BioProfile.visibility = View.GONE
        binding.Follow.visibility = View.GONE
        binding.MessageViewProfile.visibility = View.GONE
        binding.textView12.visibility = View.GONE
        binding.recyclerProfileView.visibility = View.GONE
    }

    fun showMainUI() {
        binding.toolbar3.visibility = View.VISIBLE
        binding.profileViewprofile.visibility = View.VISIBLE
        binding.Post.visibility = View.VISIBLE
        binding.ower.visibility = View.VISIBLE
        binding.textView10.visibility = View.VISIBLE
        binding.textView9.visibility = View.VISIBLE
        binding.ProfileName.visibility = View.VISIBLE
        binding.BioProfile.visibility = View.VISIBLE
        binding.Follow.visibility = View.VISIBLE
        binding.MessageViewProfile.visibility = View.VISIBLE
        binding.textView12.visibility = View.VISIBLE
        binding.recyclerProfileView.visibility = View.VISIBLE
    }

    private  fun Friends(uidd: String)
    {
        val db3 = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uidd/RequestAccept")
        db3.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot)
            {
                FriendsCount = snapshot.childrenCount.toString()

                binding.ower.text = FriendsCount

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

}

