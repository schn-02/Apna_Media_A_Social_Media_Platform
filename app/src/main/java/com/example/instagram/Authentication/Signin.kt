package com.example.instagram.Authentication

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.instagram.MainActivity
import com.example.instagram.R
import com.example.instagram.databinding.ActivitySigninBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Signin : AppCompatActivity()
{
    lateinit var  binding: ActivitySigninBinding
    lateinit var database: FirebaseDatabase
    lateinit var auth: FirebaseAuth
    private  lateinit  var progress: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser !=null)
        {
            startActivity(Intent(this , MainActivity::class.java))
            finish()
        }
        binding.DontAccount.setOnClickListener{
            startActivity(Intent(this, Signup::class.java))

        }
        progress = ProgressDialog(this@Signin)
        progress.setMessage("Uploading")
        progress.setCancelable(false)

        progress.setIcon(R.drawable.media)

        database = FirebaseDatabase.getInstance()
auth = FirebaseAuth.getInstance()
binding.Login.setOnClickListener{
    login()
}
    }

    private fun login() {
        progress.show()
        val emailOrUsername = binding.enNameSignin.text.toString().trim()
        val pass = binding.enPassSignin.text.toString().trim()

        if (emailOrUsername.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = database.getReference("User").child("UserInfo")

        // Check if the input is an email or username
        if (emailOrUsername.contains("@")) {
            // If email, use Firebase Authentication
            auth.signInWithEmailAndPassword(emailOrUsername, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Successfully signed in", Toast.LENGTH_SHORT).show()
                    progress.dismiss()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Failed to login. Please check your email or password", Toast.LENGTH_SHORT).show()
                    progress.dismiss()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Please signup first", Toast.LENGTH_SHORT).show()
                progress.dismiss()
            }
        } else {
            // If username, query the database
            dbRef.orderByChild("username").equalTo(emailOrUsername)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Username found, now check the password
                            for (userSnapshot in snapshot.children) {
                                val userPass = userSnapshot.child("password").value.toString()
                                if (userPass == pass) {
                                    Toast.makeText(this@Signin, "Successfully signed in", Toast.LENGTH_SHORT).show()
                                    progress.dismiss()
                                    startActivity(Intent(this@Signin, MainActivity::class.java))
                                    finish()

                                }
                            }
                            Toast.makeText(this@Signin, "Incorrect password", Toast.LENGTH_SHORT).show()
                            progress.dismiss()
                        } else {
                            Toast.makeText(this@Signin, "Username not found. Please signup first", Toast.LENGTH_SHORT).show()
                            progress.dismiss()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@Signin, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                        progress.dismiss()
                    }
                })
        }
    }
}