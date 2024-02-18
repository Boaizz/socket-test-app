package com.example.socketapp
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.socketapp.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.btnNormalSocket)
        connectButton.setOnClickListener {
            val intent = Intent(this, TimeDisplayActivity::class.java)
            startActivity(intent)
        }

        val btnWebSocket = findViewById<Button>(R.id.btnWebSocket)
        btnWebSocket.setOnClickListener {
            val intent = Intent(this, WebSocketActivity::class.java)
            startActivity(intent)
        }
    }
}
