package com.example.socketapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.text.SimpleDateFormat
import java.util.*

class WebSocketActivity : AppCompatActivity() {

    private var webSocket: WebSocket? = null
    private lateinit var timeStatusAdapter: TimeStatusAdapter
    private val timeStatusList = mutableListOf<String>()
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isConnected = false
    private var currentNetworkType: String = "Unknown"
    private val timeStatusRunnable = object : Runnable {
        override fun run() {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            webSocket?.send("Time status: $currentTime")
            handler.postDelayed(this, 10000) // Schedule this to run again after 10 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_socket)

        val recyclerView = findViewById<RecyclerView>(R.id.rvWebSocketTimeStatus)
        recyclerView.layoutManager = LinearLayoutManager(this)
        timeStatusAdapter = TimeStatusAdapter(timeStatusList)
        recyclerView.adapter = timeStatusAdapter

        findViewById<Button>(R.id.btnDisconnectWebSocket).setOnClickListener {
            webSocket?.close(1000, "User disconnected")
            finish()  // Go back to MainActivity
        }
        setupConnectivityManager()
    }

    private fun setupConnectivityManager() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                val networkType = getNetworkType(network)
                currentNetworkType = networkType // Set the current network type correctly when the network is available

                if (!isConnected) {
                    isConnected = true
                    runOnUiThread {
                        timeStatusList.add("$networkType connected.")
                        timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                    }
                    startWebSocketConnection()
                }
            }

            override fun onLost(network: Network) {
                isConnected = false
                runOnUiThread {
                    // Use the previously set current network type for the disconnection message
                    timeStatusList.add("$currentNetworkType disconnected.")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
                // After notifying about the disconnection, reset the current network type
                currentNetworkType = "Unknown"
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    private fun getNetworkType(network: Network): String {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "4G"
            else -> "Network"
        }
    }

    private fun startWebSocketConnection() {
        if (!isConnected) return

        val request = Request.Builder().url("wss://echo.websocket.org").build() // Replace with your WebSocket URL
        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                webSocket = ws
                val timeOfConnection = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                runOnUiThread {
                    timeStatusList.add("WebSocket Connected at: $timeOfConnection")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
                handler.post(timeStatusRunnable) // Start sending time status messages immediately
            }

            override fun onMessage(ws: WebSocket, text: String) {
                runOnUiThread {
                    timeStatusList.add("Received: $text")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                runOnUiThread {
                    timeStatusList.add("WebSocket Closing : $reason")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                runOnUiThread {
                    timeStatusList.add("WebSocket Error : ${t.message}")
                    timeStatusAdapter.notifyItemInserted(timeStatusList.size - 1)
                }
            }
        }
        client.newWebSocket(request, listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        handler.removeCallbacks(timeStatusRunnable)
        webSocket?.close(1000, "Activity Destroyed")
        client.dispatcher.executorService.shutdown()
    }
}
