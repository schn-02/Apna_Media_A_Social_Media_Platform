package com.example.instagram

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class CommentsSaveFirebase {

    fun isLiked(context: Context,   isLiked: Boolean, postID: String, adminUID: String?, onComplete: (Boolean) -> Unit) {
        if (adminUID == null) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().getReference("User/UserInfo/$adminUID/PostInfo/$postID/LikedBy")
        val dbLikeCount = FirebaseDatabase.getInstance().getReference("User/UserInfo/$adminUID/PostInfo/$postID/LikeCount")

        // Handle the "LikedBy" data
        if (isLiked) {
            db.child(uid).setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Increment LikeCount if liked
                    dbLikeCount.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val currentLikes = mutableData.getValue(Int::class.java) ?: 0
                            mutableData.setValue(currentLikes + 1) // Increment by 1
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            onComplete(committed) // Notify completion
                        }
                    })
                } else {
                    onComplete(false) // If failed to set like
                }
            }
        } else {
            db.child(uid).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Decrement LikeCount if unliked
                    dbLikeCount.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val currentLikes = mutableData.getValue(Int::class.java) ?: 0
                            mutableData.setValue(currentLikes - 1) // Decrement by 1
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            onComplete(committed) // Notify completion
                        }
                    })
                } else {
                    onComplete(false) // If failed to remove like
                }
            }
        }

        val db2 = FirebaseDatabase.getInstance().getReference("User/UserInfo/${adminUID}/PostInfo/$postID")
        if (isLiked) {
            // If post is liked, add the user to the "LikedBy" node
            db2.child("LikedBy").child(uid).setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // After adding, update the LikeCount based on the number of children in LikedBy
                    db2.child("LikedBy").get().addOnSuccessListener { snapshot ->
                        val likeCount = snapshot.childrenCount
                        db2.child("LikeCount").setValue(likeCount)

                    }
                }
            }
        } else {
            // If post is unliked, remove the user from the "LikedBy" node
            db2.child("LikedBy").child(uid).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // After removing, update the LikeCount based on the number of children in LikedBy
                    db2.child("LikedBy").get().addOnSuccessListener { snapshot ->
                        val likeCount = snapshot.childrenCount
                        db2.child("LikeCount").setValue(likeCount)

                    }
                }
            }
        }


    }
}
