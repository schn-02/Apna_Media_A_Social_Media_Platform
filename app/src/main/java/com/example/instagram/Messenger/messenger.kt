package com.example.instagram.Messenger

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.instagram.R

class messenger : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messenger)

        val Chat = findViewById<Button>(R.id.Chat)
        val Request = findViewById<Button>(R.id.RequestAdd)

        // Set default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Chat())
                .commit()
        }

        // Replace fragments based on button clicks
        Chat.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Chat())
                .addToBackStack(null)  // Allow user to go back to previous fragment
                .commit()
        }

        Request.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Request())
                .addToBackStack(null)  // Allow user to go back to previous fragment
                .commit()
        }
    }
}
