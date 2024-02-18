package com.example.socketapp
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import com.example.socketapp.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.*

class TimeDisplayActivity : AppCompatActivity() {

    private lateinit var socket: Socket
    private lateinit var timeStatusAdapter: TimeStatusAdapter
    private val timeStatusList = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_display)

        val recyclerView = findViewById<RecyclerView>(R.id.timeStatusList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        timeStatusAdapter = TimeStatusAdapter(timeStatusList)
        recyclerView.adapter = timeStatusAdapter

        connectToServer()

        findViewById<Button>(R.id.goBackButton).setOnClickListener {
            disconnectFromServer()
            finish()
        }
    }

    private fun connectToServer() {
        Thread {
            try {
                socket = Socket("192.168.4.24", 3001)
                runOnUiThread {
                    timeStatusList.add("Connected")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
                displayTimeStatus()
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    timeStatusList.add("Connection Failed")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
            }
        }.start()
    }

    private fun sendMessageToServer(message: String) {
        Thread {
            try {
                if (!::socket.isInitialized || socket.isClosed) {
                    // Socket not initialized or closed, attempt to reconnect
                    connectToServer()
                }
                val out = PrintWriter(socket.getOutputStream(), true)
                val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))

                out.println(message)  // Send message to server
                val response = `in`.readLine()

                runOnUiThread {
                    if (response != null) {
                        timeStatusList.add("Sent: $message, Received: $response")
                    } else {
                        timeStatusList.add("Sent: $message, Received: null - Server may be down. Attempting to reconnect...")
                        // Attempt to reconnect
                        connectToServer()
                    }
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
            } catch (e: SocketException) {
                runOnUiThread {
                    timeStatusList.add("Connection lost. Attempting to reconnect...")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
                // Sleep for a while and then try reconnecting
                Thread.sleep(2000) // 2 seconds delay before reconnection
                connectToServer()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    timeStatusList.add("Error sending message: $e")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
                scheduleReconnect()
            }
        }.start()
    }

    private fun displayTimeStatus() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                sendMessageToServer("Time status: $currentTime")  // Send current time to server

                handler.postDelayed(this, 10000) // Schedule this to run again after 10 seconds
            }
        }, 10000) // Start after 10 seconds delay
    }

    private fun disconnectFromServer() {
        try {
            socket.close()
            runOnUiThread {
                timeStatusList.add("Disconnected")
                timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                timeStatusList.add("Error during disconnection: $e")
                timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
            }
        }
    }
    private fun scheduleReconnect() {
        reconnectRunnable = Runnable {
            connectToServer()
        }
        reconnectHandler.postDelayed(reconnectRunnable!!, 5000) // Retry after 5 seconds
    }

    private fun cancelReconnect() {
        reconnectHandler.removeCallbacks(reconnectRunnable!!)
    }


    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
        cancelReconnect()
        handler.removeCallbacksAndMessages(null) // Prevent memory leaks
    }
}