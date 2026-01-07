package com.example.instagram.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.Chat.chatDetail
import com.example.instagram.Filter.FilterChat
import com.example.instagram.Filter.FilterHome
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.ViewProfile.ViewProfile

class ChatFragmentAdapter(
    val context: Context,
    val posts: ArrayList<PostModel>
)
    :RecyclerView.Adapter<ChatFragmentAdapter.viewholder>(),Filterable
{

        var original = posts
        val difutil =object :DiffUtil.ItemCallback<PostModel>()
        {
            override fun areItemsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {

               return  oldItem.PostID ==newItem.PostID
            }

            override fun areContentsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {

                 return  oldItem == newItem
            }

        }
        val differ = AsyncListDiffer(this , difutil)

    class viewholder(itemview:View):RecyclerView.ViewHolder(itemview)
    {
        val image = itemview.findViewById<ImageView>(R.id.ChatSelectionProfileImage)
        val name = itemview.findViewById<TextView>(R.id.ChatSelectionProfileName)
        val LastMessage = itemview.findViewById<TextView>(R.id.ChatSelectionLastMessage)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {
         val view:View =LayoutInflater.from(parent.context).inflate(R.layout.sample_chat_layout , parent , false)
        return  viewholder(view)
    }

    override fun getItemCount(): Int {
       return  differ.currentList.size
    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
        val post = differ.currentList[position]
        holder.name.text = post.userName
        Glide.with(context)
            .load(post.profileImage)
            .into(holder.image)

        holder.LastMessage.text = post.LastMessage

        holder.itemView.setOnClickListener{
            val intent = Intent(context , chatDetail::class.java)
            intent.putExtra("ChatDetail" , post.AdminUID)
            context.startActivity(intent)
        }

        holder.image.setOnClickListener {
            val intent = Intent(context, ViewProfile::class.java)
            intent.putExtra("UIDChat", post.AdminUID)
            context.startActivity(intent)
        }
    }

    private var filterInstance: FilterChat? = null  // ✅ Ye object ko maintain karega

    override fun getFilter(): Filter {
        if (filterInstance == null) {

            filterInstance = FilterChat(this, original, context)  // ✅ Proper initialization
        }
        return filterInstance!!
    }
}