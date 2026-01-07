package com.example.instagram.Adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.instagram.Models.Users
import com.example.instagram.R

class GalleryAdapter(
    private val context: Context,
    private val images: List<Users>,
    private val onImageClick: (Uri) -> Unit,  // Only pass Uri to onImageClick
    private val selectedImagesOrder: Map<Uri, Int>
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        val circlePost: ImageView = itemView.findViewById(R.id.circlePost)
        val numberOverlay: TextView = itemView.findViewById(R.id.imageNumberOverlay) // TextView to show number
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thumbnail, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val image = images[position]

        // Set image
        image.imageUri?.let { uri ->
            holder.image.setImageURI(uri)
        } ?: run {
            holder.image.setImageResource(R.drawable.add) // Placeholder image
        }

        // Show number if selected, otherwise hide
        image.imageUri?.let { uri ->
            if (selectedImagesOrder.containsKey(uri)) { // Explicitly cast to Uri if needed
                holder.numberOverlay.visibility = View.VISIBLE
                holder.circlePost.visibility = View.VISIBLE
                holder.numberOverlay.text = selectedImagesOrder[uri].toString()
            } else {
                holder.numberOverlay.visibility = View.GONE
                holder.circlePost.visibility = View.GONE
            }
        }

        // Handle click
        image.imageUri?.let { uri ->
            holder.itemView.setOnClickListener {
                onImageClick(uri)
            }
        } ?: run {
            holder.itemView.setOnClickListener {
                Toast.makeText(context, "Invalid image URI", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun getItemCount(): Int = images.size
}
