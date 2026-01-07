package com.example.instagram.Splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.instagram.Authentication.Signin
import com.example.instagram.Authentication.Signup
import com.example.instagram.R
import com.example.instagram.databinding.ActivityMainSplashBinding

class MainSplash : AppCompatActivity() {

    private lateinit var  binding: ActivityMainSplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_splash)

         binding =ActivityMainSplashBinding.inflate(layoutInflater)

        binding.root

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this , Signup::class.java))
            finish()
        },4000)



    }
}