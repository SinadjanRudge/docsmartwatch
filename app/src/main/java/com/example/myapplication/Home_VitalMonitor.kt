package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.MeasureCapabilities
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.getCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Home_VitalMonitor : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001;
    private lateinit var measureClient: MeasureClient
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var heartRateCallback: MeasureCallback
    private lateinit var exerciseCallback: ExerciseUpdateCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_vital_monitor)

        val exit: Button = findViewById(R.id.exitBtn);

        exit.setOnClickListener {                                                //Appointment
            startActivity(Intent(this@Home_VitalMonitor, Home_Selection::class.java))
        };

        val healthClient = HealthServices.getClient(this)
        measureClient = healthClient.measureClient
        exerciseClient = healthClient.exerciseClient
        val pmonClient: PassiveMonitoringClient = healthClient.passiveMonitoringClient

        getMeasureCapabilities(measureClient, ::onMeasure);
        getExerciseCapabilities(exerciseClient, ::onExercise);
        getPMonCapabilities(pmonClient, ::onPmon);

//        checkHeartRateAvailable()
//        setUpCallBacks()

    }

    fun onMeasure(m: MeasureCapabilities)
    {
        System.out.println();
    }

    fun onExercise(e: ExerciseCapabilities)
    {
        System.out.println();
    }

    fun onPmon(e: PassiveMonitoringCapabilities)
    {
        System.out.println();
    }

    fun checkWatchPermission(): Boolean {
        val activityRecognitionPermission = android.Manifest.permission.ACTIVITY_RECOGNITION
        val bodySensorsPermission = android.Manifest.permission.BODY_SENSORS

        val activityRecognitionGranted = ContextCompat.checkSelfPermission(
            this,
            activityRecognitionPermission
        ) == PackageManager.PERMISSION_GRANTED

        val bodySensorsGranted = ContextCompat.checkSelfPermission(
            this,
            bodySensorsPermission
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!activityRecognitionGranted) {
            permissionsToRequest.add(activityRecognitionPermission)
        }
        if (!bodySensorsGranted) {
            permissionsToRequest.add(bodySensorsPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }

        return true
    }

    private fun getMeasureCapabilities(measureClient: MeasureClient, callback: (capabilities: MeasureCapabilities) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch {
            val measureCapabilities = measureClient.getCapabilities()
            callback(measureCapabilities)
        }
    }

    private fun getExerciseCapabilities(exerciseClient: ExerciseClient, callback: (capabilities: ExerciseCapabilities) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch {
            val exerciseCapabilities = exerciseClient.getCapabilities()
            callback(exerciseCapabilities)
        }
    }

    private fun getPMonCapabilities(pmonClient: PassiveMonitoringClient, callback: (capabilities: PassiveMonitoringCapabilities) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch {
            val pmonCapabilities = pmonClient.getCapabilities()
            callback(pmonCapabilities)
        }
    }

    private fun checkHeartRateAvailable() {
        CoroutineScope(Dispatchers.IO).launch {
            val measureCapabilities = measureClient.getCapabilities()
            val exerciseCapabilities = exerciseClient.getCapabilities()
            val supportsHeartRate =
                DataType.HEART_RATE_BPM in measureCapabilities.supportedDataTypesMeasure
            //"Heart rate supported: $supportsHeartRate".log()
            //"Exercise supported: $exerciseCapabilities".log()
            //"Measure supported: $measureCapabilities".log()
            System.out.println();
        }
    }

    private fun registerHeartRate() {
        if (checkWatchPermission()) {
            measureClient.registerMeasureCallback(
                DataType.Companion.HEART_RATE_BPM,
                heartRateCallback
            )
        }
    }

    private fun setUpCallBacks() {
        heartRateCallback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    // Handle availability change.
                    "Heart rate availability changed: $availability".log()
                }
                "Heart rate2 availability changed: $availability".log()
            }

            override fun onDataReceived(data: DataPointContainer) {
                // Inspect data points.
                try {
                    "Heart rate data received: ${data.sampleDataPoints[0].value}".log()
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

        exerciseCallback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val exerciseStateInfo = update.exerciseStateInfo
                val activeDuration = update.activeDurationCheckpoint
                val latestMetrics = update.latestMetrics
                val latestGoals = update.latestAchievedGoals
                "Exercise update received: $update".log()
                "Exercise update received: $exerciseStateInfo".log()
                "Exercise update received: $activeDuration".log()
                "Exercise update received: $latestMetrics".log()
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
                "Lap summary received: $lapSummary".log()
                // For ExerciseTypes that support laps, this is called when a lap is marked.
            }

            override fun onRegistered() {
                "Exercise update callback registered".log()
                //val exerciseTypes = exerciseClient.getCapabilities().supportedExerciseTypes
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                throwable.printStackTrace()
                "Exercise update callback registration failed: $throwable".log()
            }

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                "Availability changed: $availability".log()
                // Called when the availability of a particular DataType changes.
                when {
                    availability is LocationAvailability -> {}
                    // Relates to Location / GPS
                    availability is DataTypeAvailability -> {}
                    // Relates to another DataType
                }
            }
        }

        if (checkWatchPermission()) {
            registerHeartRate()
        }
    }

    companion object {
        fun Any.log(message: String = "") {
            Log.e("ADAMX", "$this $message")
        }
    }
}