package com.triadss.doctrack2.dataModel

import com.google.type.DateTime

class TaskModel (
    val patientId: String = "",
    val status: String = "",
    val purpose:String = "",
    val nameOfRequester:String = "",
    val dateOfAppointment: DateTime,
    val createdAt: DateTime)