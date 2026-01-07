package com.example.instagram.Adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.Comments.Comment
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.databinding.SampleNotificationLayoutBinding
import com.google.firebase.auth.FirebaseAuth

class NotificationAdapter(
    val context: Context, private val posts: ArrayList<PostModel>
):RecyclerView.Adapter<NotificationAdapter.viewHolder>()
{

    class  viewHolder(val binding: SampleNotificationLayoutBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
         val binding = SampleNotificationLayoutBinding.inflate(LayoutInflater.from(parent.context) , parent , false)

        return  viewHolder(binding)
    }

    override fun getItemCount(): Int {
         return  posts.size;
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.binding.apply {
             val postt = posts[position]
            notification.text = "Your Post Was Liked By" +" "+ postt.postName

            Glide.with(context)
                .load(postt.profileImage)
                .placeholder(R.drawable.userstory)
                .error(R.drawable.userstory)
                .into(notificationProfile)

            Glide.with(context)
                .load(postt.FirstPic?.get(position))
                .placeholder(R.drawable.photos)   // Placeholder image
                .error(R.drawable.photos)               // Error image
                .into(notificationPostPic)

            val uid = FirebaseAuth.getInstance().currentUser?.uid

            notification.setOnClickListener {
                Toast.makeText(context, "LALAXX:-${postt.PostID}", Toast.LENGTH_SHORT).show()

            }

            holder.itemView.setOnClickListener{
                // Bottom Sheet ko show karna
                val bottomSheetFragment5 = Comment()

                // Fragment arguments mein AdminUID bhejna
                val bundle5 = Bundle()
                bundle5.putString("AdminID5", uid)
                bundle5.putString("RandomID5", postt.PostID)
                bottomSheetFragment5.arguments = bundle5
//                Toast.makeText(context, "LALAXX:-$uid", Toast.LENGTH_SHORT).show()


                // Fragment ko show karna
                if (context is AppCompatActivity) {
                    bottomSheetFragment5.show((context as AppCompatActivity).supportFragmentManager, bottomSheetFragment5.tag)


                }

            }
        }



        }

}