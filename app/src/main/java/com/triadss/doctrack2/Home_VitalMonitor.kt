package com.triadss.doctrack2

import VitalSignsModel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.health.services.client.ExerciseUpdateCallback
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class Home_VitalMonitor : AppCompatActivity() {
    private val CHECK_INTERVAL_MILLISECONDS: Long = 3000
    private val TAG = "VitalMonitor"
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var exerciseCallback: ExerciseUpdateCallback
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
    private lateinit var vitalSigns: VitalSignsModel
    private lateinit var activity: Activity
    private var checkOnce = false;

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

                    vitalSigns = VitalSignsModel(
                        bloodPressure,
                        temperature,
                        pulseRate,
                        oxygenLevel,
                        weight,
                        height,
                        bmi
                    )

                    bloodPressureVal.text = bloodPressure
                    temperatureVal.text = temperature.toString();
                    pulseRateVal.text = pulseRate.toString()
                    oxygenLevelVal.text = oxygenLevel.toString()
                    weightVal.text = weight.toString()
                    heightVal.text = height.toString()
                    bmiVal.text = bmi.toString()
                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing JSON data: " + e.message)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_vital_monitor)

        activity = this

        // Initialize views
        initViews()

        val healthClient = HealthServices.getClient(this)
        val measureClient = healthClient.measureClient
        exerciseClient = healthClient.exerciseClient

        // Check and request permissions if needed
        if (!hasPermissions()) {
            requestPermissions()
        }

        initHeartRate()
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
            val nodeListTask: Task<List<Node>> = Wearable.getNodeClient(activity).getConnectedNodes()
            val nodes: List<Node> = withContext(Dispatchers.IO) {
                Tasks.await(nodeListTask)
            }

            if (nodes.size == 1) {
                showPairedDeviceStatus(true)
            } else {
                showPairedDeviceStatus(false)
            }
        }
    }

    private fun showPairedDeviceStatus(isPaired: Boolean) {
        if (isPaired) {
            vitalsContainer.visibility = View.VISIBLE
            unavailableVitalsText.visibility = View.GONE
            if (!checkOnce) {
                Toast.makeText(activity, "Paired with a mobile phone", Toast.LENGTH_SHORT).show()
            }
            checkOnce = true
        } else {
            vitalsContainer.visibility = View.GONE
            unavailableVitalsText.visibility = View.VISIBLE
            if (checkOnce) {
                Toast.makeText(activity, "Please pair a device with the smartwatch", Toast.LENGTH_SHORT).show()
                checkOnce = false
            }
        }
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


    private fun updateHeartRateValue(newHeartRate: Any) {
        runOnUiThread {
            heartRateText.text = "Heart Rate: \n$newHeartRate bpm"
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
                Toast.makeText(activity, "All permissions granted", Toast.LENGTH_SHORT).show()
                initHeartRate()
            } else {
                // Permissions not granted, handle accordingly
//                Toast.makeText(activity, "All permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register a listener to receive data events when the activity is resumed
        Wearable.getDataClient(this).addListener(dataListener)
    }

    override fun onPause() {
        super.onPause()
        // Unregister the data listener to avoid memory leaks when the activity is paused
        Wearable.getDataClient(this).removeListener(dataListener)
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
