package com.triadss.doctrack2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.getCapabilities
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.triadss.doctrack2.dbHelper.VitalSignsDbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

class Home_VitalMonitor : AppCompatActivity() {
    private val CHECK_INTERVAL_MILLISECONDS: Long = 3000
    private val TAG = "VitalMonitor"
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var loadingText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var unavailableVitalsText: TextView
    private lateinit var bloodPressureVal: TextView
    private lateinit var pulseRateVal: TextView
    private lateinit var temperatureVal: TextView
    private lateinit var oxygenLevelVal: TextView
    private lateinit var weightVal: TextView
    private lateinit var heightVal: TextView
    private lateinit var bmiVal: TextView
    private lateinit var vitalsContainer: LinearLayout
    private var checkOnce = false
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var lastSync: TextView

    private val dataListener = DataClient.OnDataChangedListener { dataEventBuffer ->
        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonData = dataMap.getString("jsonData")
                Log.d(TAG, "Received JSON data: $jsonData")

                try {
                    val jsonObject = JSONObject(jsonData)
                    val vitalsId = jsonObject.getInt("vitalsId")
                    val patientId = jsonObject.getString("patientId")
                    val bloodPressure = jsonObject.getString("bloodPressure")
                    val temperature = jsonObject.getDouble("temperature")
                    val pulseRate = jsonObject.getInt("pulseRate")
                    val oxygenLevel = jsonObject.getInt("oxygenLevel")
                    val weight = jsonObject.getDouble("weight")
                    val height = jsonObject.getDouble("height")
                    val bmi = jsonObject.getDouble("BMI")
                    val uid = jsonObject.getString("uid")

                    val lastSyncMillis = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a")
                    val lastSync = sdf.format(Date(lastSyncMillis))

                    Log.e(TAG, "Last sync: $lastSync")

                    // Update UI with received data
                    updateUI(bloodPressure, temperature, pulseRate, oxygenLevel, weight, height, bmi, lastSync)

                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing JSON data: ${e.message}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_vital_monitor)

        //* Create an instance of your database helper
//        val dbHelper = VitalSignsDbHelper(this)
//
//        //* Get the SQLiteDatabase object by calling writableDatabase
//        val db = dbHelper.writableDatabase


        //* Delete the existing table if it exists
//        db.execSQL("DROP TABLE IF EXISTS ${MyDatabaseContract.VitalSignsEntry.TABLE_NAME}")

        // Initialize views
        initViews()

        // Check and request permissions if needed
        if (!hasPermissions()) {
            requestPermissions()
        }
        initHeartRate()

        // Retrieve data from local storage or database and update UI
        loadDataAndUpdateUI()
        startContinuousCheck()
    }

    private fun initViews() {
        unavailableVitalsText = findViewById(R.id.unavailableVitalsText)
        vitalsContainer = findViewById(R.id.vitalsContainer)
        heartRateText = findViewById(R.id.heartRateValue)
        bloodPressureVal = findViewById(R.id.bpVal)
        temperatureVal = findViewById(R.id.tempVal)
        oxygenLevelVal = findViewById(R.id.oxygenVal)
        weightVal = findViewById(R.id.weightVal)
        heightVal = findViewById(R.id.heightVal)
        bmiVal = findViewById(R.id.bmiVal)
        pulseRateVal = findViewById(R.id.pRateVal)
        lastSync = findViewById(R.id.lastSyncText)

        val healthClient = HealthServices.getClient(this)
        val measureClient = healthClient.measureClient
        exerciseClient = healthClient.exerciseClient
    }

    private fun startContinuousCheck() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                checkIfPairedDevice()
                delay(CHECK_INTERVAL_MILLISECONDS)
            }
        }
    }

    private fun checkIfPairedDevice() {
        CoroutineScope(Dispatchers.Main).launch {
            val nodeListTask: Task<List<Node>> = Wearable.getNodeClient(this@Home_VitalMonitor).connectedNodes
            val nodes: List<Node> = withContext(Dispatchers.IO) {
                Tasks.await(nodeListTask)
            }

            showPairedDeviceStatus(nodes.size == 1)
        }
    }


    private fun showPairedDeviceStatus(isPaired: Boolean) {
        if (isPaired) {
            vitalsContainer.visibility = View.VISIBLE
            unavailableVitalsText.visibility = View.GONE
            if (!checkOnce) {
                Toast.makeText(this@Home_VitalMonitor, "Paired with a mobile phone", Toast.LENGTH_SHORT).show()
            }
            checkOnce = true
        } else {
            vitalsContainer.visibility = View.GONE
            unavailableVitalsText.visibility = View.VISIBLE
            if (checkOnce) {
                Toast.makeText(this@Home_VitalMonitor, "Please pair a device with the smartwatch", Toast.LENGTH_SHORT).show()
                checkOnce = false
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val bluetoothPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        )
        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        val bluetoothConnectPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        return bluetoothPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BODY_SENSORS
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permissions not granted, handle accordingly
//                Toast.makeText(activity, "All permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(dataListener)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(dataListener)
    }

    private fun updateUI(
        bloodPressure: String,
        temperature: Double,
        pulseRate: Int,
        oxygenLevel: Int,
        weight: Double,
        height: Double,
        bmi: Double,
        lastDateSync: String
    ) {
        runOnUiThread {
            bloodPressureVal.text = bloodPressure
            temperatureVal.text = temperature.toString()
            pulseRateVal.text = pulseRate.toString()
            oxygenLevelVal.text = oxygenLevel.toString()
            weightVal.text = weight.toString()
            heightVal.text = height.toString()
            bmiVal.text = bmi.toString()
            lastSync.text = "Last Sync: $lastDateSync"
        }

        saveDataLocally(
            bloodPressure,
            temperature,
            pulseRate,
            oxygenLevel,
            weight,
            height,
            bmi,
            lastDateSync
        )
    }

    private fun saveDataLocally(
        bloodPressure: String,
        temperature: Double,
        pulseRate: Int,
        oxygenLevel: Int,
        weight: Double,
        height: Double,
        bmi: Double,
        lastSync: String
    ) {
        val dbHelper = VitalSignsDbHelper(this@Home_VitalMonitor)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_BLOOD_PRESSURE, bloodPressure)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_TEMPERATURE, temperature)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_PULSE_RATE, pulseRate)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_OXYGEN_LEVEL, oxygenLevel)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_WEIGHT, weight)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_HEIGHT, height)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_BMI, bmi)
            put(MyDatabaseContract.VitalSignsEntry.COLUMN_LAST_SYNC, lastSync) // Save last sync time
        }

        // Insert the data into the database
        db.insert(MyDatabaseContract.VitalSignsEntry.TABLE_NAME, null, values)

        // Close the database connection
        db.close()
    }


    private fun initHeartRate() {
        val healthClient = HealthServices.getClient(this)
        val measureClient = healthClient.measureClient
        exerciseClient = healthClient.exerciseClient

        supportsHeartRate(measureClient, object : EitherCallback {
            override fun onTrue() {
                initMeasureClientCallbacks(measureClient)
            }

            override fun onFalse() {
                loadingText.visibility = View.GONE
            }
        })
    }



    private fun supportsHeartRate(measureClient: MeasureClient, eitherCallback: EitherCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val measureCapabilities = measureClient.getCapabilities()
            withContext(Dispatchers.Main) {
                when (DataType.HEART_RATE_BPM in measureCapabilities.supportedDataTypesMeasure) {
                    true -> eitherCallback.onTrue()
                    false -> eitherCallback.onFalse()
                }
            }
        }
    }

    private fun initMeasureClientCallbacks(measureClient: MeasureClient) {
        val heartRateCallback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                Log.d(TAG, "Heart rate availability changed: $availability")
            }

            override fun onDataReceived(data: DataPointContainer) {
                try {
                    val heartRateValue = data.sampleDataPoints[0].value
                    updateHeartRateValue(heartRateValue)
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving heart rate data: ${e.message}")
                }
            }

            override fun onRegistered() {
                super.onRegistered()
                Log.d(TAG, "Measure client registered")
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                super.onRegistrationFailed(throwable)
                Log.e(TAG, "Measure client registration failed: ${throwable.message}")
            }
        }

        measureClient.registerMeasureCallback(
            DataType.HEART_RATE_BPM,
            heartRateCallback
        )
    }
    private fun updateHeartRateValue(newHeartRate: Any) {
        runOnUiThread {
            heartRateText.text = "Heart Rate: \n$newHeartRate bpm"
        }
    }

    private fun loadDataAndUpdateUI() {
        val dbHelper = VitalSignsDbHelper(this@Home_VitalMonitor)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            MyDatabaseContract.VitalSignsEntry.COLUMN_BLOOD_PRESSURE,
            MyDatabaseContract.VitalSignsEntry.COLUMN_TEMPERATURE,
            MyDatabaseContract.VitalSignsEntry.COLUMN_PULSE_RATE,
            MyDatabaseContract.VitalSignsEntry.COLUMN_OXYGEN_LEVEL,
            MyDatabaseContract.VitalSignsEntry.COLUMN_WEIGHT,
            MyDatabaseContract.VitalSignsEntry.COLUMN_HEIGHT,
            MyDatabaseContract.VitalSignsEntry.COLUMN_BMI,
            MyDatabaseContract.VitalSignsEntry.COLUMN_LAST_SYNC
        )

        var cursor: Cursor? = null

        try {
            cursor = db.query(
                MyDatabaseContract.VitalSignsEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
            )

            cursor?.let { c ->
                if (c.moveToFirst()) {
                    val bloodPressureIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_BLOOD_PRESSURE)
                    val temperatureIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_TEMPERATURE)
                    val pulseRateIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_PULSE_RATE)
                    val oxygenLevelIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_OXYGEN_LEVEL)
                    val weightIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_WEIGHT)
                    val heightIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_HEIGHT)
                    val bmiIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_BMI)
                    val lastSyncIndex = c.getColumnIndexOrThrow(MyDatabaseContract.VitalSignsEntry.COLUMN_LAST_SYNC)

                    val bloodPressure = c.getString(bloodPressureIndex)
                    val temperature = c.getDouble(temperatureIndex)
                    val pulseRate = c.getInt(pulseRateIndex)
                    val oxygenLevel = c.getInt(oxygenLevelIndex)
                    val weight = c.getDouble(weightIndex)
                    val height = c.getDouble(heightIndex)
                    val bmi = c.getDouble(bmiIndex)
                    val lastSync = c.getString(lastSyncIndex)

                    // Update UI with loaded data
                    updateUI(bloodPressure, temperature, pulseRate, oxygenLevel, weight, height, bmi, lastSync)
                } else {
                    // Handle case where cursor is empty (no data found)
                    // You can show a message or perform other actions here
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data from database: ${e.message}")
            // Handle the error as needed (e.g., show an error message)
        } finally {
            cursor?.close()
            db.close()
        }
    }


    interface EitherCallback {
        fun onTrue()
        fun onFalse()
    }

    companion object {
        fun Any.log(message: String = "") {
            Log.e("ADAMX", "$this $message")
        }
    }

}
