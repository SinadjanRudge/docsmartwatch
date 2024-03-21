package com.triadss.doctrack2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.triadss.doctrack2.R

class Home_Selection : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_selection)

        val close = findViewById(R.id.btn_close) as Button
        val syncing = findViewById(R.id.btn_syncing) as Button
        val vitalMonitor = findViewById(R.id.vitalMonitor) as Button

        close.setOnClickListener {                                                      //Close App
            finishAffinity()
        }
        syncing.setOnClickListener {                                                //Appointment
            syncing.setOnClickListener {
                Syncing()
            }
        }

        vitalMonitor.setOnClickListener {                                                //Appointment
            VitalMonitor()
        }
    }

    fun Syncing(){
        startActivity(Intent(this@Home_Selection, Home_Syncing::class.java))
    }

    fun VitalMonitor(){
        startActivity(Intent(this@Home_Selection, Home_VitalMonitor::class.java))
    }
}