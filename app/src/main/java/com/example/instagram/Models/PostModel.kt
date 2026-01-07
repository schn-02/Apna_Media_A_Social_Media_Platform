package com.example.instagram.Models

import android.net.Uri
import com.denzcoskun.imageslider.models.SlideModel
import java.util.UUID

data class PostModel(
    var images: List<SlideModel> ?=null, // Post images
    val Notification: List<String> ?=null, //notification
    var LikedBy: ArrayList<String> ?=null, //LikedBy
    var caption: String?= " ",         // Post caption
    var PostDate: String?= null,         // Post Date
    var userName: String? = " ",        // User's username
    var profileImage: Uri? =null,     // User's profile image URL
    var Music_Name: String? =null,     // User's profile image URL
    var AdminUID:String ? = null,
    var postName:String ?=" ",
    var PostID: String? =null,
    val LastMessage: String? ="",
    var comments: List<PostModel>? = null,
    val userId: String?=null,
    val commentText: String?=null,

    var FirstPic: ArrayList<Uri>? =null,
    val comment: String = "",

    var LikesCount: Int?  =0,
    var PostCount: Int?  =0,
    var PostRandomId :ArrayList<String> ?=null,
    var isLiked: Boolean =false,
    var musicplay: Boolean =false,
    var musicUri: Uri?= null,
    var isPlaying: Boolean = false
)
