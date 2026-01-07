package com.example.instagram.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instagram.Models.PostModel
import com.example.instagram.R
import com.example.instagram.ViewProfile.ViewProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RequestMessengerAdapter(val context: Context,  val post: ArrayList<PostModel>):RecyclerView.Adapter<RequestMessengerAdapter.viewholder>()
{
    class  viewholder(itemview:View):RecyclerView.ViewHolder(itemview)
    {

        val confirm = itemview.findViewById<ImageView>(R.id.RequestPeoples)
        val ProfileImage = itemview.findViewById<ImageView>(R.id.messengerRequestProfile)
        val dates = itemview.findViewById<TextView>(R.id.RequestDate)
        val cancel = itemview.findViewById<ImageView>(R.id.RequestCancelPeoples)
        val usernames = itemview.findViewById<TextView>(R.id.RequestUsername)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewholder {
         val view:View = LayoutInflater.from(parent.context).inflate(R.layout.sample_request_layout , parent , false)
        return viewholder(view)
     }

    override fun getItemCount(): Int {
        return  post.size
     }

    override fun onBindViewHolder(holder: viewholder, position: Int)
    {
        val posts = post[position]
        holder.usernames.text = posts.userName
        Glide.with(context)
            .load(posts.profileImage)
            .into(holder.ProfileImage)

        holder.ProfileImage.setOnClickListener {
            val intent = Intent(context, ViewProfile::class.java)
            intent.putExtra("UIRequest", posts.AdminUID)
            context.startActivity(intent)
        }

        holder.confirm.setOnClickListener {
            RequestAccept(posts.AdminUID)

        }

        holder.cancel.setOnClickListener {
            Confirm_Cancel(posts.AdminUID)

        }


        holder.dates.text = posts.PostDate
    }

    private  fun RequestAccept(posts: String?)
    {

         val uid = FirebaseAuth.getInstance().currentUser?.uid

        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$posts/RequestAccept")
        val key = db.push().key
        if (key != null) {
            db.child(key).setValue(uid).addOnCompleteListener { task->
                if (task.isSuccessful){

                    requestaccept2(posts)


                }
                else{
                    Toast.makeText(context , "Please Try Again" , Toast.LENGTH_SHORT).show()

                }
            }
        }



    }

    private  fun requestaccept2(posts: String?)
    {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db2 = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/RequestAccept")
        val key1 = db2.push().key
        if (key1 != null) {
            
            db2.child(key1).setValue(posts).addOnCompleteListener {

                Toast.makeText(context , "Re:-- $posts" , Toast.LENGTH_SHORT).show()
                Confirm_Cancel(posts)

            }
        }

    }

    private  fun Confirm_Cancel(posts: String?)
    {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val db = FirebaseDatabase.getInstance().getReference().child("User/UserInfo/$uid/Request")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (req in snapshot.children) {
                    // Fetch the UID from the snapshot
                    val uidd = req.child("UID").getValue(String::class.java)

                    // Check if posts UID matches with the retrieved UID
                    if (posts == uidd) {
                        // Get the reference to the node you want to delete
                        val postRef = req.ref  // This is the reference to the current node
                        // Delete the node
                        postRef.removeValue()
                            .addOnSuccessListener {
                                // Handle success if needed (e.g., show a message or refresh UI)
                                Toast.makeText(context, "Node deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                // Handle failure if needed (e.g., show error message)
                                Toast.makeText(context, "Failed to delete node", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }
}