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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class Post : AppCompatActivity() {

    private lateinit var binding: ActivityPosting2Binding

    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "channel_id"
    private val randomUID = UUID.randomUUID().toString()

    private lateinit var musicPickerLauncher: ActivityResultLauncher<Intent>

    private var save = false

    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var progress: ProgressDialog

    private var musicUri: Uri? = null
    private var no: Int? = 0
    private var selectedImages: ArrayList<Users> = ArrayList()

    private var isCompressingMusic = false
    private var selectedMusicName: String? = null

    private var originalMusicSizeKB: Long = 0
    private var compressedMusicSizeKB: Long = 0

    private val LOWEST_AUDIO_BITRATE = 16_000  // 16kbps = very small, lowest usable
    private val FALLBACK_AUDIO_BITRATE = 24_000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPosting2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        storageReference = Firebase.storage.reference

        progress = ProgressDialog(this@Post)
        progress.setMessage("Uploading")
        progress.setCancelable(false)
        progress.setIcon(R.drawable.adduser)

        NotificationChannel()

        setupMusicPicker()
        setupSelectedImages()
        setupClickListeners()
    }

    private fun setupMusicPicker() {
        musicPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val selectedUri = result.data?.data

                if (selectedUri == null) {
                    Toast.makeText(this, "Failed to load music!", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                selectedMusicName = getMusicName(selectedUri) ?: "Selected Music"
                originalMusicSizeKB = getUriSizeInKB(selectedUri)

                isCompressingMusic = true

                binding.selectMusic.isEnabled = false
                binding.Post.isEnabled = false

                binding.MusicName.visibility = View.VISIBLE
                binding.MusicName.text = "Compressing music..."
                binding.musicAnimation.visibility = View.VISIBLE
                binding.musicPlay.visibility = View.GONE
                binding.stopMusic.visibility = View.GONE

                showCompressionLoader()

                compressMusicForFirebase(selectedUri) { compressedUri ->

                    hideCompressionLoader()

                    isCompressingMusic = false
                    binding.selectMusic.isEnabled = true
                    binding.Post.isEnabled = true

                    if (compressedUri != null) {
                        musicUri = compressedUri

                        val compressedFile = File(compressedUri.path ?: "")
                        compressedMusicSizeKB = if (compressedFile.exists()) {
                            compressedFile.length() / 1024
                        } else {
                            0
                        }

                        binding.MusicName.text = selectedMusicName
                        binding.musicAnimation.visibility = View.VISIBLE
                        binding.stopMusic.visibility = View.VISIBLE
                        binding.musicPlay.visibility = View.GONE

                        Toast.makeText(
                            this,
                            "Compressed: ${originalMusicSizeKB}KB → ${compressedMusicSizeKB}KB",
                            Toast.LENGTH_LONG
                        ).show()

                        playMusic(compressedUri)

                    } else {
                        musicUri = null
                        compressedMusicSizeKB = 0

                        binding.MusicName.text = "Music compression failed"
                        binding.musicAnimation.visibility = View.GONE
                        binding.musicPlay.visibility = View.GONE
                        binding.stopMusic.visibility = View.GONE

                        Toast.makeText(
                            this,
                            "Music compression failed. Please select another audio.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupSelectedImages() {
        val imagesFromIntent = intent.getParcelableArrayListExtra<Users>("selected_images")
        selectedImages = imagesFromIntent ?: ArrayList()

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "No images selected!", Toast.LENGTH_SHORT).show()
            return
        }

        val imageList = ArrayList<SlideModel>()

        selectedImages.forEach { image ->
            image.imageUri?.let {
                imageList.add(SlideModel(it.toString(), ScaleTypes.FIT))
            }
        }

        if (imageList.isNotEmpty()) {
            binding.imageSliderPost.setImageList(imageList)
        } else {
            Toast.makeText(this, "No images available for the slider", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.selectMusic.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            no = 1

            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mediaPlayer = null
            musicUri = null

            binding.MusicName.text = null
            binding.MusicName.visibility = View.GONE
            binding.musicAnimation.visibility = View.GONE
            binding.musicPlay.visibility = View.GONE
            binding.stopMusic.visibility = View.GONE

            musicPickerLauncher.launch(intent)
        }

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

        binding.Post.setOnClickListener {
            if (isCompressingMusic) {
                Toast.makeText(this, "Please wait, music is compressing...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadPost()
        }
    }

    private fun uploadPost() {
        progress.setMessage("Uploading post...")
        progress.show()

        val caption = binding.Caption.text.toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            progress.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val postDb = firebaseDatabase.reference
            .child("User")
            .child("UserInfo")
            .child(uid)
            .child("PostInfo")
            .child(randomUID)

        postDb.child("Caption").setValue(caption).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                postDb.child("RandomID").setValue(randomUID)
                postDb.child("PostDate").setValue(currentDate)

                Toast.makeText(this@Post, "Posting Successfully", Toast.LENGTH_SHORT).show()

                uploadImages()

                musicUri?.let {
                    saveMusicIntoFirebae(it)
                }

                if (musicUri == null && no == 1) {
                    progress.dismiss()
                    Toast.makeText(this, "Please select music again. Compression failed.", Toast.LENGTH_SHORT).show()
                }

            } else {
                progress.dismiss()
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImages() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Images Not Available Please Try again", Toast.LENGTH_SHORT).show()
            return
        }

        for (imageUri in selectedImages) {
            uploadimagesIntoFirebase(imageUri)
        }
    }

    private fun uploadimagesIntoFirebase(imageUri: Users) {
        val uid = firebaseAuth.currentUser?.uid

        if (uid == null || imageUri.imageUri == null) {
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRef = storageReference
            .child("User")
            .child(uid)
            .child("$uid/User_Post/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri.imageUri!!).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener {
                saveImageIntoFirebase(it.toString())
            }
        }.addOnFailureListener {
            progress.dismiss()
            Toast.makeText(this, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageIntoFirebase(imageURl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            progress.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val database = firebaseDatabase.reference
            .child("User")
            .child("UserInfo")
            .child(uid)
            .child("PostInfo")
            .child(randomUID)

        val imageId = database.push().key

        imageId?.let {
            database.child("PostPics").child(it).setValue(imageURl).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Post Upload Successfully", Toast.LENGTH_SHORT).show()

                    if (no != 1) {
                        progress.dismiss()
                        NotificationForPost()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun saveMusicIntoFirebae(musicUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            progress.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        progress.setMessage("Uploading compressed music...")

        val musicRef = storageReference
            .child("User")
            .child(uid)
            .child("$uid/User_Music/${System.currentTimeMillis()}.m4a")

        musicRef.putFile(musicUri).addOnSuccessListener {
            musicRef.downloadUrl.addOnSuccessListener {
                saveMusicIntoFirebase(it.toString())
            }
        }.addOnFailureListener {
            progress.dismiss()
            Toast.makeText(this, "Music upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveMusicIntoFirebase(MusicUri: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            progress.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = firebaseDatabase.reference
            .child("User")
            .child("UserInfo")
            .child(uid)
            .child("PostInfo")
            .child(randomUID)

        val songName = selectedMusicName ?: "Unknown Song"

        db.child("Music:-").setValue(MusicUri).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                db.child("Music_Name:-").setValue(songName)
                db.child("Music_Format").setValue("m4a")
                db.child("Music_Codec").setValue("aac")
                db.child("Music_Compressed").setValue(true)
                db.child("Music_Bitrate").setValue("16kbps")
                db.child("Music_Quality").setValue("lowest")
                db.child("Music_Original_Size_KB").setValue(originalMusicSizeKB)
                db.child("Music_Compressed_Size_KB").setValue(compressedMusicSizeKB)

                save = !save

                Toast.makeText(this, "Data Added Successfully", Toast.LENGTH_SHORT).show()

                progress.dismiss()
                NotificationForPost()

                startActivity(Intent(this, MainActivity::class.java))
                finish()

            } else {
                progress.dismiss()
                Toast.makeText(this, "Music data save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun PlayStopMusic() {
        mediaPlayer?.let {
            try {
                it.start()
                binding.musicPlay.visibility = View.GONE
                binding.stopMusic.visibility = View.VISIBLE
                Toast.makeText(this, "Music resumed!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Unable to resume music", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No music to resume!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic(musicUri: Uri) {
        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@Post, musicUri)
                prepare()
                isLooping = true
                start()
            }

            binding.stopMusic.visibility = View.VISIBLE

            val songName = selectedMusicName ?: "Unknown Song"

            binding.MusicName.visibility = View.VISIBLE
            binding.musicAnimation.visibility = View.VISIBLE
            binding.MusicName.text = songName

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Music play error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }

            binding.musicPlay.visibility = View.VISIBLE
            binding.stopMusic.visibility = View.GONE

            Toast.makeText(this, "Music stopped!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMusicName(musicUri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        contentResolver.query(musicUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }

        return null
    }

    private fun getUriSizeInKB(uri: Uri): Long {
        return try {
            val fileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
            val sizeBytes = fileDescriptor?.length ?: 0
            fileDescriptor?.close()

            if (sizeBytes > 0) {
                sizeBytes / 1024
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    private fun showCompressionLoader() {
        try {
            if (!progress.isShowing) {
                progress.setMessage("Uploading music, please wait...")
                progress.show()
            } else {
                progress.setMessage("Uploading music, please wait...")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideCompressionLoader() {
        try {
            if (progress.isShowing) {
                progress.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(UnstableApi::class)
    private fun compressMusicForFirebase(inputUri: Uri, onComplete: (Uri?) -> Unit) {
        compressMusicWithBitrate(
            inputUri = inputUri,
            bitrate = LOWEST_AUDIO_BITRATE
        ) { firstCompressedUri ->

            if (firstCompressedUri != null) {
                onComplete(firstCompressedUri)
            } else {
                compressMusicWithBitrate(
                    inputUri = inputUri,
                    bitrate = FALLBACK_AUDIO_BITRATE
                ) { fallbackCompressedUri ->
                    onComplete(fallbackCompressedUri)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun compressMusicWithBitrate(
        inputUri: Uri,
        bitrate: Int,
        onComplete: (Uri?) -> Unit
    ) {
        try {
            val outputFile = File(
                cacheDir,
                "apna_media_low_music_${bitrate}_${System.currentTimeMillis()}.m4a"
            )

            if (outputFile.exists()) {
                outputFile.delete()
            }

            val mediaItem = MediaItem.fromUri(inputUri)

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .build()

            val audioEncoderSettings = AudioEncoderSettings.Builder()
                .setBitrate(bitrate)
                .build()

            val encoderFactory = DefaultEncoderFactory.Builder(this)
                .setRequestedAudioEncoderSettings(audioEncoderSettings)
                .build()

            val transformer = Transformer.Builder(this)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        runOnUiThread {
                            if (outputFile.exists() && outputFile.length() > 0) {
                                onComplete(Uri.fromFile(outputFile))
                            } else {
                                onComplete(null)
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        exportException.printStackTrace()

                        runOnUiThread {
                            onComplete(null)
                        }
                    }
                })
                .build()

            transformer.start(editedMediaItem, outputFile.absolutePath)

        } catch (e: Exception) {
            e.printStackTrace()

            runOnUiThread {
                onComplete(null)
            }
        }
    }

    private fun NotificationForPost() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.media)
            .setContentTitle("APNA MEDIA")
            .setContentText("Post Upload Successfully")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            if (
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            notify(1, builder.build())
        }
    }

    private fun NotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APNA MEDIA",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.description = "POSTS"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }
}