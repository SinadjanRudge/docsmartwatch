package com.example.myapplication.bluetooth

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.constants.BluetoothConstants
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        //If the message’s path equals "/my_path"...//
        if (messageEvent.getPath().equals(BluetoothConstants.DataPath)) {
            //...retrieve the message//
            val message: String = String(messageEvent.getData())
            val messageIntent = Intent()
            messageIntent.setAction(Intent.ACTION_SEND)
            messageIntent.putExtra(BluetoothConstants.MessageKey, message)
            //Broadcast the received Data Layer messages locally//
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent)
        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}
