package com.example.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.constants.BluetoothConstants
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.ExecutionException


class Home_Syncing : AppCompatActivity() {
    private lateinit var talkClick: Button
    private lateinit var textLog: TextView
    private lateinit var activity: Activity

    private var phoneNodeId: String? = null // Initialize as nullable
    private var sentMessageNumber = 1;
    private var receivedMessageNumber = 1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_syncing)

        activity = this

        talkClick = findViewById(R.id.talkClick);
        textLog = findViewById(R.id.textLog);

        //Create an OnClickListener//
        //Create an OnClickListener//
        talkClick.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val onClickMessage = "I just sent the handheld a message " + sentMessageNumber++
                textLog.setText(onClickMessage)
                //Use the same path//
                val datapath = BluetoothConstants.DataPath
                SendMessage(datapath, onClickMessage).start()
            }
        })
        //Register to receive local broadcasts, which we'll be creating in the next step//
        val newFilter = IntentFilter(Intent.ACTION_SEND)
        val messageReceiver = Receiver()
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Display the following when a new message is received//
            val onMessageReceived =
                "\nI just received a message from the handheld: " + receivedMessageNumber++ +
                "\n" + intent?.getStringExtra(BluetoothConstants.MessageKey);
            textLog.append(onMessageReceived)
        }
    }

    internal inner class SendMessage //Constructor for sending information to the Data Layer//
        (var path: String, var message: String) : Thread() {
        override fun run() {
            //Retrieve the connected devices//
            val nodeListTask: Task<List<Node>> =
                Wearable.getNodeClient(activity).getConnectedNodes()
            try {
                //Block on a task and get the result synchronously//
                val nodes: List<Node> = Tasks.await<List<Node>>(nodeListTask)
                for (node in nodes) {
                    //Send the message///
                    val sendMessageTask: Task<Int> = Wearable.getMessageClient(this@Home_Syncing)
                        .sendMessage(node.getId(), path, message.toByteArray())
                    try {
                        val result = Tasks.await(sendMessageTask)
                        //Handle the errors//
                    } catch (exception: ExecutionException) {
                        //TO DO//
                    } catch (exception: InterruptedException) {
                        //TO DO//
                    }
                }
            } catch (exception: ExecutionException) {
                //TO DO//
            } catch (exception: InterruptedException) {
                //TO DO//
            }
        }
    }

}
