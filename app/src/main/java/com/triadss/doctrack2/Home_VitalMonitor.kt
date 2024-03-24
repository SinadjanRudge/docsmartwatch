package com.triadss.doctrack2

import VitalSignsModel
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.getCapabilities
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject


class Home_VitalMonitor : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks {
    private lateinit var mGoogleApiClient: GoogleApiClient
    private val TAG = "VitalMonitor"
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var exerciseCallback: ExerciseUpdateCallback
    private lateinit var loadingText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var unavailabileHeartRate: TextView
    private lateinit var bloodPressureVal: TextView
    private lateinit var pulseRateVal: TextView
    private lateinit var temperatureVal: TextView
    private lateinit var oxygenLevelVal: TextView
    private lateinit var weightVal: TextView
    private lateinit var heightVal: TextView
    private lateinit var bmiVal: TextView
    private lateinit var vitalsContainer: LinearLayout
    private lateinit var vitalSigns: VitalSignsModel
    private val isConnectedToWearable = false
    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_vital_monitor)

        activity = this

        // Initialize mGoogleApiClient
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .build()

        unavailabileHeartRate = findViewById(R.id.unavailableHeartRateText)
        vitalsContainer = findViewById(R.id.vitalsContainer)
        heartRateText = findViewById(R.id.heartRateValue)
        bloodPressureVal = findViewById(R.id.bpVal)
        temperatureVal = findViewById(R.id.tempVal)
        oxygenLevelVal = findViewById(R.id.oxygenVal)
        weightVal = findViewById(R.id.weightVal)
        heightVal = findViewById(R.id.heightVal)
        bmiVal = findViewById(R.id.bmiVal)
        pulseRateVal = findViewById(R.id.pRateVal)

        val healthClient = HealthServices.getClient(this)
        val measureClient = healthClient.measureClient;
        exerciseClient = healthClient.exerciseClient

        //  Check if the required permissions are granted
        if (!hasPermissions()) {
            // Request permissions if not granted
            requestPermissions()
        }

        // Permissions are already granted, proceed with your code
        initHeartRate()

        // Use coroutines to perform the blocking operation on a background thread
        CoroutineScope(Dispatchers.Main).launch {
            val nodeListTask: Task<List<Node>> = Wearable.getNodeClient(activity).getConnectedNodes()
            val nodes: List<Node> = withContext(Dispatchers.IO) {
                Tasks.await(nodeListTask)
            }

            Log.e("Hello world", "" + nodes.size)
        }
    }

    private fun getNodes(): Collection<String> {
        val results = HashSet<String>()
        CoroutineScope(Dispatchers.IO).launch {
            // Perform the asynchronous operation in the background thread
            try {
                val nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await()
                for (node in nodes.nodes) {
                    results.add(node.id)
                }
                withContext(Dispatchers.Main) {
                    // Update UI or perform any UI-related tasks here
                    Log.e("VitalMonitor", "Node Size: " + nodes.nodes.size)
                }
            } catch (e: Exception) {
                Log.e("VitalMonitor", "Error fetching nodes: ${e.message}")
            }
        }

        return results
    }


    private val dataListener = DataClient.OnDataChangedListener { dataEventBuffer ->
        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonData = dataMap.getString("jsonData")
                Log.d(TAG, "Received JSON data: $jsonData")

                // Log the values from the JSON data
                try {
                    // Assuming your JSON structure matches the VitalSignsDto fields
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


    private fun updateHeartRateValue(newHeartRate: Any) {
        runOnUiThread {
            // Update the TextView displaying the heart rate value
            heartRateText.text = "Heart Rate: \n" + newHeartRate.toString() + "  bpm"
        }
    }


    private fun hasPermissions(): Boolean {
        // Check if Bluetooth permissions are granted
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted after user's response
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                // All permissions granted, proceed with your code
                initHeartRate()
            } else {
                // Permissions not granted, handle accordingly (e.g., show a message or exit the app)
                // You can also request permissions again or take appropriate action based on the user's response
            }
        }
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
                if (availability is DataTypeAvailability) {
                    "Heart rate availability changed: $availability".log()
                }
                "Heart rate availability changed: $availability".log()
            }

            override fun onDataReceived(data: DataPointContainer) {
                try {
                    // Assuming the heart rate data is in BPM (beats per minute)
                    val heartRateValue = data.sampleDataPoints[0].value
                    updateHeartRateValue(heartRateValue) // Update UI with the new heart rate value
//                    Log.e(TAG, "Heart rate data received: $heartRateValue BPM")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onRegistered() {
                super.onRegistered()
                "onRegistered Success".log()
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                super.onRegistrationFailed(throwable)
                throwable.printStackTrace()
                "onRegistrationFailed ${throwable.localizedMessage}".log()
            }
        }

        measureClient.registerMeasureCallback(
            DataType.Companion.HEART_RATE_BPM,
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

    interface EitherCallback {
        fun onTrue()
        fun onFalse()
    }

    companion object {
        fun Any.log(message: String = "") {
            Log.e("ADAMX", "$this $message")
        }
    }

    override fun onConnected(p0: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("Not yet implemented")
    }

}

