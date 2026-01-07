package com.example.instagram.Fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.models.SlideModel
import com.example.instagram.Adapters.Profile_Post_Adapter
import com.example.instagram.Authentication.Signup
import com.example.instagram.MainActivity
import com.example.instagram.Messenger.messenger
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.databinding.ActivityEditProfileInfoBinding
import com.example.instagram.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream



class Profile : Fragment() {
    private var _binding: FragmentProfileBinding?=null
    private val PICK_IMAGE_REQUEST = 1
    private var FriendsCount:String = "0"
    lateinit var  adapter:Profile_Post_Adapter;
    private var  postmodel = ArrayList<PostModel>()

    private   var  posts = ArrayList<Uri>()


    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.shimmer.visibility =View.VISIBLE
        binding.profilePostRecycler.visibility = View.GONE

          adapter = Profile_Post_Adapter(requireContext() , posts , postmodel)

        binding.profilePostRecycler.adapter = adapter
        binding.profilePostRecycler.layoutManager = GridLayoutManager(requireContext() , 3)

         binding.FriendsName.setOnClickListener {
             startActivity(Intent(requireContext() , messenger::class.java))
         }

        binding.messageprofile.setOnClickListener {
            startActivity(Intent(requireContext() , messenger::class.java))

        }

        binding.info.setOnClickListener {

            val dialog = AlertDialog.Builder(requireContext())
            dialog.setMessage("Are you sure you want to Logout ?")

            dialog.setPositiveButton("Yes"){dialog ,_->
                logOut()
            }

            dialog.setNegativeButton("No"){dialog , _->

                dialog.dismiss()

            }

            val alertDialog = dialog.create()
            alertDialog.show()
        }
        getPost()
        Friends()
        getData()
        showBioName()
         userNameGet()
        loadProfileImage()

        binding.editProfile.setOnClickListener{
            saveBioName()
        }

        binding.profileImage.setOnClickListener {
            openImageChooser()
        }


        binding.shareProfile.setOnClickListener {
            shareProfile()
        }






        return  binding.root
    }

    private fun shareProfile() {
        try {
            // Create a new Intent with action to share text
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"  // Define the type of data to share

            // Create your custom deep link URL
            val text = "com.example.instagram://profile"
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "" // Get current user UID
            val url = "com.example.instagram://profile/$userId" // Append the user ID to the deep link

            // Add the URL to the intent's extra data
            intent.putExtra(Intent.EXTRA_TEXT, url)

            // Start the intent to share the profile URL
            startActivity(Intent.createChooser(intent, "Share Profile"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error while sharing profile", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showBioName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid!=null)
        {
            val db = FirebaseDatabase.getInstance().getReference().child("User").child("UserInfo").child(uid)
            db.addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists())
                    {
                        Toast.makeText(requireContext(), "there is no name" , Toast.LENGTH_SHORT).show()
                    }
                    val name = snapshot.child("name").value.toString()
                    val bio = snapshot.child("bio").value.toString()

                    binding.nameProfile.text = name
                    binding.Bio.text = bio
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }


    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type= "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                try {
                    // URI se bitmap banake ImageView par set karo
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImageUri)
                    binding.profileImage.setImageBitmap(bitmap)

                     saveIntoFireBasestorage(bitmap)


                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Image load karne me error aayi!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Image select nahi hui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveIntoFireBasestorage(bitmap: Bitmap) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val imageREF = FirebaseStorage.getInstance().reference.child("UserProfilePic").child("$uid.jpg")

        // Convert Bitmap to ByteArray
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Upload ByteArray to Firebase
        val uploadTask = imageREF.putBytes(data)
        uploadTask.addOnSuccessListener {
            imageREF.downloadUrl.addOnSuccessListener { uri ->
                saveImageIntoFirebaseDatabase(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Image Upload Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun saveImageIntoFirebaseDatabase(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().reference.child("User").child("UserInfo").child(uid!!)

        val updates = mapOf(
            "ProfileImage" to imageUrl
        )

        db.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                Toast.makeText(requireContext(), "Profile Upload Successfully", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(requireContext(), "Please Try Again", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadProfileImage() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().reference.child("User").child("UserInfo").child(uid)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileImageUrl = snapshot.child("ProfileImage").value?.toString()
                    if (!profileImageUrl.isNullOrEmpty()) {
                        // Load the image into ImageView using Glide
                        Glide.with(requireContext())
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profileadd)  // Placeholder while loading
                            .into(binding.profileImage)
                    } else {
                        Toast.makeText(requireContext(), "No Profile Image Found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveBioName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the specific user's data
        val db = FirebaseDatabase.getInstance().reference.child("User").child("UserInfo").child(uid)

        // Fetch data from the database
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentName = snapshot.child("name").value?.toString() ?: ""
                val currentBio = snapshot.child("bio").value?.toString() ?: ""

                // Inflate the dialog layout
                val editProfile = ActivityEditProfileInfoBinding.inflate(LayoutInflater.from(requireContext()))
                editProfile.apply {
                    // Pre-fill the EditTexts with current values
                    EditName.setText(currentName)
                    EditBio.setText(currentBio)

                    // Create and show the dialog
                    val dialog = AlertDialog.Builder(requireContext())
                    dialog.setView(editProfile.root)
                    val alertDialog = dialog.create()

                    saveProfileInfo.setOnClickListener {
                        val name = EditName.text.toString().trim()
                        val bio = EditBio.text.toString().trim()

                        if (name.isEmpty() || bio.isEmpty()) {
                            Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Update the database with new values
                        val updates = mapOf(
                            "name" to name,
                            "bio" to bio
                        )

                        db.updateChildren(updates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "Successfully updated", Toast.LENGTH_SHORT).show()
                                    alertDialog.dismiss() // Dismiss dialog after success
                                } else {
                                    Toast.makeText(requireContext(), "Update failed. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    alertDialog.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun userNameGet() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid


        if (uid!=null)
        {
            val db = FirebaseDatabase.getInstance().getReference().child("User").child("UserInfo").child(uid!!)
            db.addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists())
                    {
                        Toast.makeText(requireContext(), "there is no Username" , Toast.LENGTH_SHORT).show()
                    }
                    val usernameProfile = snapshot.child("username").value.toString()
                    binding.UsernameProfile.text = usernameProfile

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }

    }

    private  fun getPost()
    {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/PostInfo")
        db.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                for (pics in snapshot.children)
                {
                     val postPics = pics.child("PostPics")
                    for (firstPic in postPics.children)
                    {
                         val value = firstPic.getValue(String::class.java)
                        value?.let { posts.add(it.toUri()) }
                        break
                    }

                }
                binding.shimmer.visibility =View.GONE
                binding.profilePostRecycler.visibility = View.VISIBLE

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }
    private fun getData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid")

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postmodel.clear() // Prevent duplicate entries
                posts.clear()

                val username = snapshot.child("username").getValue(String::class.java).toString()
                val profileImageUri = snapshot.child("ProfileImage").getValue(String::class.java)?.toUri()

                val postInfo = snapshot.child("PostInfo")
                for (pi in postInfo.children) {
                    val postss = PostModel()  // Create a new instance for each post
                    val likeList = ArrayList<String>()

                    val caption = pi.child("Caption").getValue(String::class.java)
                    val likeCount = pi.child("LikeCount").getValue(Int::class.java)
                    val Date = pi.child("PostDate").getValue(String::class.java)
                    val MusicName = pi.child("Music_Name:-").getValue(String::class.java)
                    val music = pi.child("Music:-").getValue(String::class.java)
                    val randomid = pi.child("RandomID").getValue(String::class.java)
                    val musicUri = if (music.isNullOrEmpty()) null else Uri.parse(music)

                     val postCount = postInfo.childrenCount


                    //  Fix: Iterate through PostPics as a Map
                    val postPicsNode = pi.child("PostPics")

                    val LikedBy = pi.child("LikedBy")

                  if (LikedBy.exists())
                  {
                      for (l in LikedBy.children)
                    {
                          val LikeBYY = l.key
                        LikeBYY?.let { likeList.add(it) }
                       }

                 }

                    val imageList = ArrayList<SlideModel>()

                    if (postPicsNode.exists()) {
                        for (pic in postPicsNode.children) {
                            val postPicUrl = pic.getValue(String::class.java)?.toUri()
                            if (postPicUrl != null) {
                                posts.add(postPicUrl)  // Add image Uri to posts list
                                break
                            }
                        }
                    }




                    for (image in postPicsNode.children) {
                        val imageUrl = image.getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            imageList.add(SlideModel(imageUrl))
                        }
                    }


                     postss.PostCount = postCount.toInt()

                    postss.profileImage = profileImageUri
                    postss.images = imageList
                    postss.userName = username
                    postss.PostDate = Date
                    postss.caption = caption
                    postss.LikedBy = likeList
                    postss.Music_Name = MusicName
                   postss.musicUri = musicUri
                    postss.LikesCount = likeCount
                    postss.PostID= randomid


                    binding.PostCount.text = postss.PostCount.toString()

                    postmodel.add(postss)  // Add each post to the list
                }

                binding.profilePostRecycler.visibility = View.VISIBLE
                binding.shimmer.visibility = View.GONE

                adapter.notifyDataSetChanged()  // Refresh RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private  fun Friends()
    {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db3 = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/RequestAccept")
        db3.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot)
            {
                FriendsCount = snapshot.childrenCount.toString()

                binding.Friends.text = FriendsCount

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){

            startActivity(Intent(requireContext() , MainActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun logOut() {
        FirebaseAuth.getInstance().signOut()

        // Start the Signin activity
        val intent = Intent(requireContext(), Signup::class.java)

        // Clear the activity stack and prevent going back to the Profile activity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

    }

}