package com.example.instagram.Post

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.instagram.Fragments.Home
import com.example.instagram.MainActivity
import com.example.instagram.Models.Users
import com.example.instagram.R
import com.example.instagram.databinding.ActivityPosting2Binding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class Post : AppCompatActivity() {

    private lateinit var binding: ActivityPosting2Binding
    private var mediaPlayer: MediaPlayer? = null
    private var CHANNEL_ID ="channel_id"
           val randomUID = UUID.randomUUID().toString()
    private lateinit var musicPickerLauncher: ActivityResultLauncher<Intent>

    private   var save =false

    lateinit var firebaseStorage: FirebaseStorage
   lateinit var firebaseDatabase: FirebaseDatabase
   lateinit var firebaseAuth: FirebaseAuth
    private var musicUri: Uri? = null
    private  lateinit  var progress:ProgressDialog
    private var no: Int? = 0
   lateinit var storageReference: StorageReference
    private var selectedImages: ArrayList<Users> = ArrayList()







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosting2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
       progress = ProgressDialog(this@Post)
        progress.setMessage("Uploading")
        progress.setCancelable(false)

        NotificationChannel()
        progress.setIcon(R.drawable.adduser)

        musicPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                 musicUri = data?.data // Get the music URI

                if (musicUri != null) {
                    playMusic(musicUri!!) // Play the selected music
                } else {
                    Toast.makeText(this, "Failed to load music!", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Get selected images from intent
        val imagesFromIntent = intent.getParcelableArrayListExtra<Users>("selected_images")
        selectedImages = imagesFromIntent ?: ArrayList()

        if (selectedImages.isNullOrEmpty()) {
            Toast.makeText(this, "No images selected!", Toast.LENGTH_SHORT).show()
            return
        }

        // Image Slider Setup
        val imageList = ArrayList<SlideModel>()
        selectedImages.forEach { image ->
            imageList.add(SlideModel(image.imageUri.toString(), ScaleTypes.FIT))
        }


       storageReference = Firebase.storage.reference


        if (imageList.isNotEmpty()) {
            binding.imageSliderPost.setImageList(imageList)
        } else {
            Toast.makeText(this, "No images available for the slider", Toast.LENGTH_SHORT).show()
        }



        binding.Post.setOnClickListener {
            progress.show()
            val caption = binding.Caption.text.toString()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val currentDate = sdf.format(Date())

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            firebaseDatabase.reference. child("User").child("UserInfo").child(uid!!).child("PostInfo").child(randomUID).child("Caption").setValue(caption)

                .addOnCompleteListener { task->
                    if (task.isSuccessful)
                    {

                        firebaseDatabase.reference. child("User").child("UserInfo").child(uid!!).child("PostInfo").child(randomUID).child("RandomID").setValue(randomUID)
                        firebaseDatabase.reference. child("User").child("UserInfo").child(uid!!).child("PostInfo").child(randomUID).child("PostDate").setValue(currentDate)

                        Toast.makeText(this@Post, "Posting Successfully" , Toast.LENGTH_SHORT).show()

                        uploadImages()


                    }

                }

            musicUri?.let { it1 -> saveMusicIntoFirebae(it1) }

        }


        // Music Selection Button
        binding.selectMusic.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*" // To filter only audio files
            }

            no =1;
            musicPickerLauncher.launch(intent) // Launch the music picker
            mediaPlayer?.stop()
            mediaPlayer = null

            // Hide views
            binding.MusicName.text = null
            binding.MusicName.visibility = View.GONE
            binding.musicAnimation.visibility = View.GONE
            binding.musicPlay.visibility = View.GONE
            binding.stopMusic.visibility = View.GONE
        }
        // Stop Music Button
        binding.stopMusic.setOnClickListener {
            binding.musicPlay.visibility = View.VISIBLE
            binding.stopMusic.visibility = View.GONE
            stopMusic()
        }

        binding.musicPlay.setOnClickListener {
            binding.musicPlay.visibility = View.GONE
            binding.stopMusic.visibility = View.VISIBLE
            PlayStopMusic()
        }



    }

    private fun uploadImages()
    {
        if (selectedImages.isEmpty()) {
       Toast.makeText(this , "Images Not Available Please Try again" , Toast.LENGTH_SHORT).show()
        }

          for (imageUri in selectedImages)
          {
                   uploadimagesIntoFirebase(imageUri)
          }
    }


    private fun uploadimagesIntoFirebase(imageUri: Users)
    {
        val uid = firebaseAuth.currentUser?.uid
         val imageRef = storageReference.child("User").child(uid!!).child("$uid/User_Post/${System.currentTimeMillis()}.jpg")
             imageRef.putFile(imageUri.imageUri!!).addOnSuccessListener {

              imageRef.downloadUrl.addOnSuccessListener {
                      saveImageIntoFirebase(it.toString())
              }

           }
    }

    private fun saveMusicIntoFirebae(musicUri: Uri) {
               val uid = FirebaseAuth.getInstance().currentUser?.uid
              val fileExtension = contentResolver.getType(musicUri)?.substringAfterLast("/")?:"mp3"
                    val musicRef = storageReference.child("User").child(uid!!).child("$uid/User_Music/${System.currentTimeMillis()}.$fileExtension")
                       musicRef.putFile(musicUri).addOnSuccessListener {
                           musicRef.downloadUrl.addOnSuccessListener {
                               saveMusicIntoFirebase(it.toString())




                           }
                       }

    }

    private fun saveMusicIntoFirebase(MusicUri: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
               val db = firebaseDatabase.reference.child("User").child("UserInfo").child(uid!!)
                   .child("PostInfo").child(randomUID)

                   val musicId = db.push().key
                          musicId?.let {
                             val songName = musicUri?.let { it1 -> getMusicName(it1) }
                             db.child("Music:-").setValue(MusicUri).addOnCompleteListener { task->
                                  if (task.isSuccessful)
                                  {
                                      db.child("Music_Name:-").setValue(songName)

                                      save = !save
                                      Toast.makeText(this , "DataAddedSuccessfully" , Toast.LENGTH_SHORT).show()

                                      progress.dismiss()
                                      NotificationForPost()

                                      startActivity(Intent(this , MainActivity::class.java))
                                           finish()


                                  }
                             }
                         }

    }

    private fun saveImageIntoFirebase(imageURl: String){
           val uid = FirebaseAuth.getInstance().currentUser?.uid
             val database =firebaseDatabase.reference.child("User").child("UserInfo").child(uid!!).child("PostInfo").child(randomUID)
                val imageId =database.push().key
                    imageId?.let {
                           database.child("PostPics").child(it).setValue(imageURl).addOnCompleteListener {task->
                               if(task.isSuccessful)
                               {
                                    Toast.makeText(this , "Post Upload Successfully" , Toast.LENGTH_SHORT).show()
                                   if (no !=1 )
                                   {
                                  progress.dismiss()

                                       NotificationForPost()
                                       startActivity(Intent(this , MainActivity::class.java))
                                       finish()
                                   }

                               }
                           }
                   }


    }


    private fun PlayStopMusic() {

        mediaPlayer?.let {
            it.start()

            Toast.makeText(this, "Music resumed!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "No music to resume!", Toast.LENGTH_SHORT).show()
        }
            binding.musicPlay.visibility = View.GONE
            binding.stopMusic.visibility = View.VISIBLE

         Toast.makeText(this , "Music Start" , Toast.LENGTH_SHORT).show()
    }



    private fun playMusic(musicUri: Uri) {
        // Release any existing MediaPlayer instance
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@Post, musicUri)
            prepare()
            isLooping = true
            start()
        }
        binding.stopMusic.visibility = View.VISIBLE
             val songName = getMusicName(musicUri)?: "Unknown Song"





        binding.MusicName.visibility = View.VISIBLE
        binding.musicAnimation.visibility = View.VISIBLE

        binding.MusicName.text = songName


    }



    private fun stopMusic() {
        mediaPlayer?.let {
            it.pause()
            binding.musicPlay.visibility = View.VISIBLE
            binding.stopMusic.visibility = View.GONE
        }
        Toast.makeText(this, "Music stopped!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic() // Release MediaPlayer on activity destruction
    }
    private fun getMusicName(musicUri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME) // Use DISPLAY_NAME for better compatibility
        contentResolver.query(musicUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex) // Get and return the display name
            }
        }
        return null // If name not found, return null
    }


     private  fun  NotificationForPost()
     {
          val builder = NotificationCompat.Builder(this ,CHANNEL_ID )
         builder.setSmallIcon(R.drawable.media)
             .setContentTitle("APNA MEDIA")
             .setContentText("Post Upload Successfully")
             .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(this))
            {
             if (ActivityCompat.checkSelfPermission(
                     applicationContext,
                     Manifest.permission.POST_NOTIFICATIONS
                 ) != PackageManager.PERMISSION_GRANTED
             ) {

                 return
             }
                notify(1 , builder.build())
            }
     }

    private  fun NotificationChannel()
    {
         if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
         {
              val channel = NotificationChannel(CHANNEL_ID , "APNA MEDIA" , NotificationManager.IMPORTANCE_DEFAULT)
                    channel.description ="POSTS"
                     val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.createNotificationChannel(channel)
         }
    }

}
