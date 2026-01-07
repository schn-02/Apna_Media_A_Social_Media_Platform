package com.example.instagram.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.ViewProfile.ViewProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentAdapter(
    val context: Context,
    private val commentList: ArrayList<PostModel>
):RecyclerView.Adapter<CommentAdapter.viewholder>()
{

    class  viewholder(itemview:View):RecyclerView.ViewHolder(itemview)
    {
        val Message = itemview.findViewById<TextView>(R.id.CommentMessage)
        val username = itemview.findViewById<TextView>(R.id.commentProfileName)
        val ProfileImage = itemview.findViewById<ImageView>(R.id.commentProfileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {

         val view:View = LayoutInflater.from(parent.context).inflate(R.layout.sample_comment_layout , parent , false)
         return  viewholder(view)
     }

    override fun getItemCount(): Int {
        return  commentList.size
    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
         val post = commentList[position]
           holder.Message.text= post.commentText
           holder.username.text= post.userName
        Glide.with(context)
            .load(post.profileImage)
            .into(holder.ProfileImage)

        holder.ProfileImage.setOnClickListener {
            val intent = Intent(context , ViewProfile::class.java)
            intent.putExtra("commentUID" , post.AdminUID)

            context.startActivity(intent)

        }




      }





}