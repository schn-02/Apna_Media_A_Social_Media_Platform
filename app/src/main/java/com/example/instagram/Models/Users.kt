package com.example.instagram.Models

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.UUID

@Parcelize
data class Users(
    val imageUri: Uri?= null,
    val Caption :String? = null,
    var Profile_name:String ?= "Profile Name ",
    var Username:String ?= "",
    var AdminUID:String ? = null,
    var Email:String ? = null,
    var profileImage:String ? = null,
    var MusicName:String ? = null,
    var Password:String ? = null,
    var Bio:String ? = "",
    val  PostRandomUID :String= UUID.randomUUID().toString(),
    val musicUri: Uri?= null
) : Parcelable

