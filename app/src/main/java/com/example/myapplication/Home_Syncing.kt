package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivityHomeSyncingBinding
import java.io.IOException
import java.util.UUID.fromString


class Home_Syncing : AppCompatActivity() {

    private val REQUEST_BT = 1
    private val REQUEST_ENABLE_BT = 2
    private val REQUEST_CONNECT_BT = 3
    private val REQUEST_ADVERTISE_BT = 4

    private val NAME = "DoctSmaartwatch"
    private val MY_UUID = "18db211d-d69b-4024-a843-b1ebc45d00aa"

    private lateinit var bluetoothAdapter: BluetoothAdapter;

    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var stopButton: Button
    private lateinit var findPhoneBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_syncing)
        context = this;
        activity = this;

        val bluetoothManager = getSystemService(
            BluetoothManager::class.java
        )
        bluetoothAdapter = bluetoothManager.adapter

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH),
                REQUEST_BT)
            return
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_ENABLE_BT)
                return
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        val findPhoneBtn: Button = findViewById(R.id.findPhoneBtn)
        val stopBtn: Button = findViewById(R.id.stopBtn)

        var btServerThread = BluetoothServerThread()

        stopBtn.setOnClickListener {
            btServerThread.stopThread()
            stopBtn.visibility = GONE
            findPhoneBtn.visibility = VISIBLE
        }

        findPhoneBtn.setOnClickListener {
            makeDiscoverable()
            stopBtn.visibility = VISIBLE
            findPhoneBtn.visibility = GONE
            btServerThread = BluetoothServerThread()
            btServerThread.start()
        }
    }

    fun makeDiscoverable() {
        val requestCode = 1
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ), REQUEST_ADVERTISE_BT)
            return
        }
        startActivityForResult(discoverableIntent, requestCode)
    }

    @SuppressLint("MissingPermission")
    private inner class BluetoothServerThread() : Thread() {
        private final val TAG = "Bluetooth"

        private var shouldLoop = true;

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, fromString(MY_UUID))
        }

        public fun stopThread() {
            shouldLoop = false
            cancel()
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}

