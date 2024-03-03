package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Random


public class DoctorChecker : AppCompatActivity() {

    private lateinit var Start: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val Start = findViewById(R.id.btn_Start) as Button

        Start.setOnClickListener {
            //Toast.makeText(this@DoctorChecker, "You clicked me.", Toast.LENGTH_SHORT).show()
            //ntext.setText(arraylist.random())
            //setContentView(R.layout.activity_home_selection)
            StartActivity()
        }
    }
    fun StartActivity(){
        startActivity(Intent(this@DoctorChecker, Home_Selection::class.java))
    }
}