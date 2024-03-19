package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.MessageEvent

class Home_Syncing : AppCompatActivity(), MessageClient.OnMessageReceivedListener {
    private lateinit var messageClient: MessageClient
    private lateinit var sendMessageButton: Button
    private var phoneNodeId: String = "" // Initialize with a default value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_syncing)

        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)

        sendMessageButton = findViewById(R.id.send_message)
        sendMessageButton.setOnClickListener {
            val message = "Hello from Wear OS!"
            sendWearableMessage(phoneNodeId, "/action_turn_on", message.toByteArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data)
        // Handle received message
    }

    private fun sendWearableMessage(nodeId: String, messagePath: String, message: ByteArray) {
        messageClient.sendMessage(nodeId, messagePath, message)
    }
}
