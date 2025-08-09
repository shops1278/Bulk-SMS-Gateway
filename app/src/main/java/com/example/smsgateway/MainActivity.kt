package com.example.smsgateway

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val txtInfo = findViewById<TextView>(R.id.txtInfo)

        btnStart.setOnClickListener {
            val i = Intent(this, SmsService::class.java)
            startService(i)
            txtInfo.text = "Service started — listening on port 8080"
        }

        btnStop.setOnClickListener {
            val i = Intent(this, SmsService::class.java)
            stopService(i)
            txtInfo.text = "Service stopped"
        }
    }
}
