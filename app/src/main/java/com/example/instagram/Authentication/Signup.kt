package com.example.instagram.Authentication

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram.MainActivity
import com.example.instagram.Models.Users
import com.example.instagram.R
import com.example.instagram.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Signup : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private  lateinit  var progress: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progress = ProgressDialog(this@Signup)
        progress.setMessage("Connecting....")
        progress.setCancelable(false)

        progress.setIcon(R.drawable.media)

        if (FirebaseAuth.getInstance().currentUser !=null)
        {
            startActivity(Intent(this , MainActivity::class.java))
            finish()
        }

        val filter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ") || source.contains("\n")) "" else source
        }

        binding.EnNameSignup.filters = arrayOf(filter) // Yeh yaha lagao
        binding.EnPassSignup.filters = arrayOf(filter) // Yeh yaha lagao
        binding.EnEmailSignUp.filters = arrayOf(filter) // Yeh yaha lagao

        // Initialize FirebaseAuth and FirebaseDatabase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.Already.setOnClickListener {
            startActivity(Intent(this , Signin::class.java))
            finish()
        }
        // Set onClick listener for sign-up button
        binding.CreateNew.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        progress.show()
        // Extract input values
        val name = binding.EnNameSignup.text.toString().trim()
        val pass = binding.EnPassSignup.text.toString().trim()
        val email = binding.EnEmailSignUp.text.toString().trim()

        // Validate input fields
        if (name.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            progress.dismiss()
            return
        }



        // Check if username exists
        val dbRef = database.reference.child("User").child("UserInfo")
        dbRef.orderByChild("username").equalTo(name)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        progress.dismiss()

                        // Username already exists
                        Toast.makeText(this@Signup, "Username already exists. Please choose another.", Toast.LENGTH_SHORT).show()
                        return
                    } else {
                        // Proceed with sign-up as username is unique
                        auth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        // Create user object
                                        val user = Users(Username = name, Password = pass, Email = email , AdminUID = uid)

                                        // Save user data in Firebase
                                        dbRef.child(uid).setValue(user)
                                            .addOnCompleteListener { dbTask ->
                                                if (dbTask.isSuccessful) {
                                                    Toast.makeText(this@Signup, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                                                    progress.dismiss()

                                                    startActivity(Intent(this@Signup, MainActivity::class.java))
                                                    finish()
                                                } else {
                                                    Toast.makeText(this@Signup, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                                    progress.dismiss()

                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this@Signup, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                                progress.dismiss()

                                            }
                                    }
                                } else {
                                    Toast.makeText(this@Signup, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    progress.dismiss()

                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Signup, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    progress.dismiss()

                }
            })
    }
}
