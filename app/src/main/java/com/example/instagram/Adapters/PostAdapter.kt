import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.Comments.Comment
import com.example.instagram.CommentsSaveFirebase
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.ViewProfile.ViewProfile
import com.example.instagram.databinding.SamplePostLayoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class PostAdapter(
    private val context: Context,
    private val posts: MutableList<PostModel>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private var exoPlayer: ExoPlayer? = null
    private var adapter =this
    private var currentPlayingPosition = -1
    private   var  Commentlist =ArrayList<PostModel>()


    companion object {
        var isMuted: Boolean = true // Static variable, accessible globally
    }

    class ViewHolder(val binding: SamplePostLayoutBinding) : RecyclerView.ViewHolder(binding.root)



    init {

        setupScrollListener()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            SamplePostLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = posts.size





    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position] // Get the current post object
        playMusicAtPosition(position)

        holder.binding.apply {
            // Bind post data to UI components
            post.images?.let { PostImageSlider.setImageList(it) }
            PostCaption.text = post.caption
            PostUserName.text = post.userName
            PostSong.text = post.Music_Name
            ShowPostDate.text = post.PostDate.toString()


            ShowComment(post.AdminUID , post.PostID ,holder)

            val commomsaver = CommentsSaveFirebase()

            if (post.LikesCount?.equals(null) == true)
            {
                Likes.text = 0.toString()
            }


            Likes.text = post.LikesCount.toString()


            // Set initial like button state from Firebase
            val db = FirebaseDatabase.getInstance().getReference("User/UserInfo/${post.AdminUID}/PostInfo/${post.PostID}/LikedBy")
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

            db.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check if current user liked the post
                    if(currentUserID!=null)
                    {
                        post.isLiked = snapshot.hasChild(currentUserID)
                    }

                    PostLike.setImageResource(if (post.isLiked) R.drawable.redheart else R.drawable.heartpost)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading like state: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            PostComment.setOnClickListener {
                // Bottom Sheet ko show karna
                val bottomSheetFragment = Comment()

                // Fragment arguments mein AdminUID bhejna
                val bundle = Bundle()
                bundle.putString("AdminID", post.AdminUID)
                bundle.putString("RandomID", post.PostID)
                bottomSheetFragment.arguments = bundle
//                Toast.makeText(context, "LALAXX:-${post.AdminUID}", Toast.LENGTH_SHORT).show()
//                Toast.makeText(context, "LALAXX:-${post.PostID}", Toast.LENGTH_SHORT).show()


                // Fragment ko show karna
                if (context is AppCompatActivity) {
                    bottomSheetFragment.show((context as AppCompatActivity).supportFragmentManager, bottomSheetFragment.tag)


                }



            }


            PostLike.setOnClickListener {
                val newLikeStatus = !post.isLiked
                post.isLiked = newLikeStatus

                // **Optimistic UI Update (turant UI me effect dikhaye)**
                if (newLikeStatus) {
                    post.LikesCount = (post.LikesCount ?: 0) + 1
                } else {
                    post.LikesCount = (post.LikesCount ?: 0) - 1
                }
                Likes.text = post.LikesCount.toString() // Turant TextView update karein
                PostLike.setImageResource(if (newLikeStatus) R.drawable.redheart else R.drawable.heartpost)

                // **Firebase update karein**
                post.PostID?.let { postID ->
                    commomsaver.isLiked(context, newLikeStatus, postID, post.AdminUID) { success ->
                        if (!success) {
                            // **Revert UI changes if Firebase fails**
                            post.isLiked = !newLikeStatus
                            post.LikesCount = if (newLikeStatus) (post.LikesCount ?: 0) - 1 else (post.LikesCount ?: 0) + 1
                            Likes.text = post.LikesCount.toString()
                            PostLike.setImageResource(if (post.isLiked) R.drawable.redheart else R.drawable.heartpost)
                        }
                    }
                }
            }






            Glide.with(context)
                .load(post.profileImage)
                .into(PostProfile)


            PostUserName.setOnClickListener {
                val intent = Intent(context , ViewProfile::class.java)
                intent.putExtra("UID" , post.AdminUID)
                context.startActivity(intent)
            }

            PostProfile.setOnClickListener {
                val intent = Intent(context , ViewProfile::class.java)
                intent.putExtra("UID" , post.AdminUID)
                context.startActivity(intent)
            }


            if (post.musicUri == null) {
                stopMusic.visibility = View.GONE
                playMusic.visibility = View.GONE
            } else {


                if (isMuted) {

                    stopMusic.visibility = View.GONE
                    playMusic.visibility = View.VISIBLE
                    playMusicAtPosition(position)


                } else if(!isMuted) {

                    stopMusic.visibility = View.VISIBLE
                    playMusic.visibility = View.GONE
                    stopMusic()



                }
            }

            playMusic.setOnClickListener {

                isMuted = false
                adapter.notifyDataSetChanged()
//                stopMusic()

                exoPlayer?.pause()

            }
            stopMusic.setOnClickListener {

                isMuted = true
                adapter.notifyDataSetChanged()

                post.musicUri?.let { it1 -> playMusicAtPosition(position) }
            }


        }
    }

    private fun playMusicAtPosition(position: Int) {
        // Only play music if isMuted is true
        if (isMuted) {
            val post = posts[position]

            // Stop previous playback
            if (currentPlayingPosition != position) {
                stopMusic() // Stop previous music
                currentPlayingPosition = position

                // Initialize ExoPlayer with application context
                exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(post.musicUri.toString()))
                    setMediaItem(mediaItem)
                    play()
                    prepare()
                    playWhenReady = true  // Start playback immediately
                }
            }
        }
    }

    fun stopMusic() {
        exoPlayer?.apply {

            stop()


        }
        exoPlayer = null
        currentPlayingPosition = -1
    }
    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    handlePlayVisibleItem()
                }
            }
        })

        // Force scroll after layout to detect the first item
        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            handlePlayVisibleItem()
        }
    }

    private fun handlePlayVisibleItem() {
        val largestView = findLargestVisibleView(recyclerView)
        val position = largestView?.let { recyclerView.getChildAdapterPosition(it) }
        if (position != null && position != RecyclerView.NO_POSITION) {
            playMusicAtPosition(position)
        }
    }

    private fun findLargestVisibleView(recyclerView: RecyclerView): View? {
        var largestView: View? = null
        var maxOccupiedSpace = 0

        // Loop through all visible child views
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val visibleHeight = getVisibleHeight(child)
            val visibleWidth = child.width

            // Calculate the occupied area (visible height * visible width)
            val occupiedSpace = visibleHeight * visibleWidth

            // If this view occupies more space, update the largest view
            if (occupiedSpace > maxOccupiedSpace) {
                maxOccupiedSpace = occupiedSpace
                largestView = child
            }
        }

        return largestView
    }

    private fun getVisibleHeight(child: View): Int {
        val recyclerViewTop = recyclerView.top
        val recyclerViewBottom = recyclerView.bottom

        // Calculate visible portion of the child view
        val childTop = child.top
        val childBottom = child.bottom

        // Get the visible portion of the child
        val visibleTop = maxOf(childTop, recyclerViewTop)
        val visibleBottom = minOf(childBottom, recyclerViewBottom)

        // If visible portion is positive, return it; otherwise, return 0
        return if (visibleBottom > visibleTop) {
            visibleBottom - visibleTop
        } else {
            0
        }
    }

    fun resetisMuted()
    {
        isMuted = false
    }
    private var isFirstLoad = true // To track the first load

    private fun ShowComment(adminUID: String?, RandomId: String?, holder: ViewHolder) {
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
                            holder.binding.Comments.text = Commentlist.size.toString()



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
