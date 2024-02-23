package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Random


public class DoctorChecker : AppCompatActivity() {

    private lateinit var ntext: TextView

    private var arraylist = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arraylist.add("10")
        arraylist.add("20")
        arraylist.add("30")
        arraylist.add("40")
        arraylist.add("50")


        ntext = findViewById<TextView>(R.id.textView)
        val btn_click_me = findViewById(R.id.btn_Start) as Button

        btn_click_me.setOnClickListener {
            Toast.makeText(this@DoctorChecker, "You clicked me.", Toast.LENGTH_SHORT).show()
            //ntext.setText(arraylist.random())
            setContentView(R.layout.activity_home_page)
        }
    }
}