package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.example.myapplication.dataModel.TaskModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class Home_Appointment : AppCompatActivity() {
    lateinit var newbtn: Button

    val db = Firebase.firestore
    public fun addButton() {

        val layout = findViewById(R.id.linearLayout2) as LinearLayout
        newbtn = Button(this@Home_Appointment)
        newbtn.setText("New button")
        layout.addView(newbtn)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_appointment)

        for (x in 1..10)
        {
                addButton()

        }
    }

}