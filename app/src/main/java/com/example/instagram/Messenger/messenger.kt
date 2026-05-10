package com.example.instagram.Messenger

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.instagram.R

class messenger : AppCompatActivity() {

    private lateinit var chatBtn: Button
    private lateinit var requestBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messenger)

        chatBtn = findViewById(R.id.Chat)
        requestBtn = findViewById(R.id.RequestAdd)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Chat())
                .commit()

            selectChatTab()
        }

        chatBtn.setOnClickListener {
            selectChatTab()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Chat())
                .commit()
        }

        requestBtn.setOnClickListener {
            selectRequestTab()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentMessenger, Request())
                .commit()
        }
    }

    private fun selectChatTab() {
        chatBtn.setBackgroundResource(R.drawable.bg_messenger_tab_selected)
        requestBtn.setBackgroundResource(R.drawable.bg_messenger_tab_unselected)

        chatBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        requestBtn.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected_text))

        chatBtn.textSize = 16f
        requestBtn.textSize = 15f

        chatBtn.alpha = 1f
        requestBtn.alpha = 0.92f
    }

    private fun selectRequestTab() {
        requestBtn.setBackgroundResource(R.drawable.bg_messenger_tab_selected)
        chatBtn.setBackgroundResource(R.drawable.bg_messenger_tab_unselected)

        requestBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        chatBtn.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected_text))

        requestBtn.textSize = 16f
        chatBtn.textSize = 15f

        requestBtn.alpha = 1f
        chatBtn.alpha = 0.92f
    }
}