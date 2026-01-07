package com.example.instagram

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram.Fragments.Search
import com.example.instagram.Fragments.Home
import com.example.instagram.Fragments.Post
import com.example.instagram.Fragments.Profile
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // Reference to BottomNavigationView
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Default fragment to display when activity is created
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, Home())
            .commit()

        // Listener for BottomNavigationView item selection
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.home -> Home()
                R.id.Post -> Post()
                R.id.search -> Search()
                R.id.profile ->Profile()
                else -> null
            }
            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
            }
            true
        }
    }
    }