package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.nio.charset.StandardCharsets

class Home_Syncing : AppCompatActivity(), MessageClient.OnMessageReceivedListener {
    private lateinit var messageClient: MessageClient
    private lateinit var sendMessageButton: Button
    private var phoneNodeId: String? = null // Initialize as nullable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_syncing)

        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)

        sendMessageButton = findViewById(R.id.send_message)
        sendMessageButton.setOnClickListener {
            val message = "Hello from Wear OS!"
            // Check if phoneNodeId is available before sending the message
            if (phoneNodeId != null) {

                sendWearableMessage(phoneNodeId!!, "/action_turn_on", message.toByteArray(StandardCharsets.UTF_8))
                Log.e("Home Syncing", "Sending message")
            } else {
                Toast.makeText(this@Home_Syncing, "Phone node ID not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Connect to Google API Client to get connected nodes
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            // Iterate through connected nodes and get the phoneNodeId
            for (node in nodes) {
                if (node.isNearby) {
                    phoneNodeId = node.id
                    break
                }
            }
        }

        // Call logNearbyNodes to log nearby nodes
        logNearbyNodes()
    }

    override fun onDestroy() {
        super.onDestroy()
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data, StandardCharsets.UTF_8)
        // Handle received message based on your application's logic
        // For example, update UI or trigger actions
        Toast.makeText(this@Home_Syncing, "Received Message: $message", Toast.LENGTH_SHORT).show()
    }

    private fun sendWearableMessage(nodeId: String, messagePath: String, message: ByteArray) {
        messageClient.sendMessage(nodeId, messagePath, message)
    }

    private fun logNearbyNodes() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.e(
                    "Nearby Node",
                    "Node ID: " + node.id + ", Display Name: " + node.displayName
                )
            }
        }
    }

}
