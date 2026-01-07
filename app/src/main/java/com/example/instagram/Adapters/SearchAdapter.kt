package com.example.instagram.Adapters

import android.content.Context
import android.content.Intent
import android.util.Log
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
import com.example.instagram.Filter.FilterHome
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.ViewProfile.ViewProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SearchAdapter(
    val context: Context,
    val posts: ArrayList<PostModel>,
     val AddUsers: ArrayList<String>
) :RecyclerView.Adapter<SearchAdapter.viewholder>(), Filterable {

    val auth = FirebaseAuth.getInstance()
    var original = posts
    val difutill = object : DiffUtil.ItemCallback<PostModel>() {

        override fun areItemsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            return oldItem.PostID == newItem.PostID
        }

        override fun areContentsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            return oldItem == newItem
        }


    }
    val differ = AsyncListDiffer(this, difutill)


    class viewholder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        val profileImage = itemview.findViewById<ImageView>(R.id.messengerAddProfile)
        val Add = itemview.findViewById<ImageView>(R.id.AddPeoples)
        val Cancel = itemview.findViewById<ImageView>(R.id.AddCancelPeoples)
        val userName = itemview.findViewById<TextView>(R.id.AddUsername)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {

        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.add_messenger_sample_layout, parent, false)
        return viewholder(view)
    }

    override fun getItemCount(): Int {

        return differ.currentList.size

    }

    override fun onBindViewHolder(holder: viewholder, position: Int) {
        val post = differ.currentList[position]  // ✅ Yehi correct list hai

        // AdminUID ko filter karne ke liye condition
        if (!AddUsers.contains(post.AdminUID?.trim())) {
            holder.userName.text = post.userName
            Glide.with(context)
                .load(post.profileImage)
                .placeholder(R.drawable.userstory)
                .into(holder.profileImage)



            holder.profileImage.setOnClickListener {
                val intent = Intent(context, ViewProfile::class.java)
                intent.putExtra("UIDadd", post.AdminUID)
                context.startActivity(intent)
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val dbRef = FirebaseDatabase.getInstance().getReference()
                    .child("User/UserInfo/${post.AdminUID}/Request")

                dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var requestExists = false
                        for (req in snapshot.children) {
                            val uidd = req.child("UID").getValue(String::class.java)
                            if (uidd == uid) {
                                requestExists = true
                                break
                            }
                        }

                        if (requestExists) {
                            holder.Cancel.visibility = View.VISIBLE
                            holder.Add.visibility = View.GONE
                        } else {
                            holder.Cancel.visibility = View.GONE
                            holder.Add.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error: ${error.message}")
                    }
                })
            }
        }


        // **Send Request Button Click**
        holder.Add.setOnClickListener {
            holder.Cancel.visibility = View.VISIBLE
            holder.Add.visibility = View.GONE
            sendRequest(post)
        }

        // **Cancel Request Button Click**
        holder.Cancel.setOnClickListener {
            holder.Cancel.visibility = View.GONE
            holder.Add.visibility = View.VISIBLE
            cancelRequest(post)
        }
    }

    private fun sendRequest(post: PostModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val randomUID = UUID.randomUUID().toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.format(Date())
        val dbRef = FirebaseDatabase.getInstance().getReference()
            .child("User/UserInfo/${post.AdminUID}/Request/$randomUID")



        dbRef.child("UID").setValue(uid).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                dbRef.child("Date").setValue(date)
                Toast.makeText(context, "Request Sent Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to Send Request", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun cancelRequest(post: PostModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance().getReference()
            .child("User/UserInfo/${post.AdminUID}/Request")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (req in snapshot.children) {
                    val uidd = req.child("UID").getValue(String::class.java)
                    if (uidd == uid) {
                        req.ref.removeValue().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Request Cancelled", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to Cancel Request",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error: ${error.message}")
            }
        })
    }

    private var filterInstance: FilterHome? = null  // ✅ Ye object ko maintain karega

    override fun getFilter(): Filter {
        if (filterInstance == null) {
            filterInstance = FilterHome(this, original, context)  // ✅ Proper initialization
        }
        return filterInstance!!
    }
}