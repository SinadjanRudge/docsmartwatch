package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
import kotlinx.coroutines.withContext

class Home_VitalMonitor : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001;
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var exerciseCallback: ExerciseUpdateCallback
    private lateinit var loadingText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var unavailabileHeartRate: TextView

    private lateinit var vitalsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_vital_monitor)

        val exit: Button = findViewById(R.id.exitBtn);
        loadingText = findViewById(R.id.loadingText)
        unavailabileHeartRate = findViewById(R.id.unavailabileHeartRateText)
        vitalsContainer = findViewById(R.id.vitalsContainer)
        heartRateText = findViewById(R.id.heartRateValue)

        exit.setOnClickListener {
            startActivity(Intent(this@Home_VitalMonitor, Home_Selection::class.java))
        };

        val healthClient = HealthServices.getClient(this)
        val measureClient = healthClient.measureClient
        exerciseClient = healthClient.exerciseClient

        initHeartRate(measureClient)

//        checkHeartRateAvailable()
//        setUpCallBacks()
    }


    private fun initHeartRate(measureClient: MeasureClient)
    {
        supportsHeartRate(measureClient, object:  EitherCallback {
            override fun onTrue() {
                // Handle successful result
                loadingText.visibility = View.GONE
                vitalsContainer.visibility = View.VISIBLE
                initMeasureClientCallbacks(measureClient)
            }

            override fun onFalse() {
                // Handle error
                loadingText.visibility = View.GONE
                unavailabileHeartRate.visibility = View.VISIBLE
            }
        });
    }

    private fun initMeasureClientCallbacks(measureClient: MeasureClient)
    {
        val heartRateCallback = object : MeasureCallback {
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
                    val value = data.sampleDataPoints[0].value
                    heartRateText.text = value.toString()
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


    private fun supportsHeartRate(measureClient: MeasureClient, eitherCallback : EitherCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val measureCapabilities = measureClient.getCapabilities()
            withContext(Dispatchers.Main) {
                when(DataType.HEART_RATE_BPM in measureCapabilities.supportedDataTypesMeasure) {
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
}