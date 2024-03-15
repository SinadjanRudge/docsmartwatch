package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class Home_Selection : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_selection)

        val close = findViewById(R.id.btn_close) as Button
        val appointment = findViewById(R.id.btn_appointment) as Button
        val vitalMonitor = findViewById(R.id.vitalMonitor) as Button

        close.setOnClickListener {                                                      //Close App
            finishAffinity()
        }
        appointment.setOnClickListener {                                                //Appointment
            appointment.setOnClickListener {
                Appointment()
            }
        }

        vitalMonitor.setOnClickListener {                                                //Appointment
            VitalMonitor()
        }
    }

    fun Appointment(){
        startActivity(Intent(this@Home_Selection, Home_Appointment::class.java))
    }

    fun VitalMonitor(){
        startActivity(Intent(this@Home_Selection, Home_VitalMonitor::class.java))
    }
}