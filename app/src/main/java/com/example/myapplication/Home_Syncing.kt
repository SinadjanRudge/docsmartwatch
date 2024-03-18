package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
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

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var stopButton: Button
    private lateinit var findPhoneBtn: Button
    private var REQUEST_ENABLE_BLUETOOTH = 1001 // You can choose any value for the request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_syncing)
        context = this
        activity = this

        val bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Request BLUETOOTH permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_BT
            )
        } else {
            // Check if Bluetooth is enabled
            checkBluetoothEnabled()
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
            Log.e("Home Syncing", "Device Bluetooth Name: " + getBluetoothDeviceName());
            makeDiscoverable()
            stopBtn.visibility = VISIBLE
            findPhoneBtn.visibility = GONE
            btServerThread = BluetoothServerThread()
//            Toast.makeText(context, "Searching for device pair...", Toast.LENGTH_SHORT).show()
            btServerThread.start()
        }
    }

    private fun checkBluetoothEnabled() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
            bluetoothAdapter.isEnabled
        ) {
            // Bluetooth is enabled and permissions are granted, proceed with your Bluetooth operations
            setupBluetooth()
        } else {
            // Request Bluetooth and Bluetooth admin permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                REQUEST_ENABLE_BLUETOOTH
            )
        }
    }


    private fun setupBluetooth() {
        // Setup Bluetooth related functionality here
        // For example, initializing BluetoothAdapter, setting up listeners, etc.
    }

    @SuppressLint("MissingPermission")
    fun makeDiscoverable() {
        val requestCode = 1
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivityForResult(discoverableIntent, requestCode)
        // This line will show a dialog asking the user to make the device visible for 300 seconds
//        showDialog("Make visible to other Bluetooth devices for 300 seconds?")

    }

    private fun showDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                // User clicked Yes, proceed with making the device discoverable
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE),
                    REQUEST_ADVERTISE_BT
                )
                // Show a message indicating that the device is searching for its pair
                Toast.makeText(context, "Searching for device pair...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { dialog, _ ->
                // User clicked No, cancel the operation
                dialog.cancel()
            }
        builder.create().show()
    }

    @SuppressLint("MissingPermission")
    private fun getBluetoothDeviceName(): String? {
        return bluetoothAdapter.name
    }


    @SuppressLint("MissingPermission")
    private inner class BluetoothServerThread : Thread() {
        private val TAG = "Bluetooth"
        private var shouldLoop = true
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, fromString(MY_UUID))
        }

        public fun stopThread() {
            shouldLoop = false
            cancel()
        }

        override fun run() {
            var connected = false

            while (shouldLoop && !connected) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    // Handle the connection
                    mmServerSocket?.close()
                    connected = true
                    shouldLoop = false
                    activity.runOnUiThread {
                        // Update UI or show a message that a device is connected
                        Log.d(TAG, "Device connected")
                        // For example, show a toast message
                        Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT).show()
                    }
                    // Perform further actions with the connected socket if needed
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            shouldLoop = false
//            try {
//                mmServerSocket?.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "Could not close the connect socket", e)
//            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permission granted, check if Bluetooth is enabled
            checkBluetoothEnabled()
        }
    }
}
