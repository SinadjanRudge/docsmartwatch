package com.triadss.doctrack2.dbHelper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.util.*

class VitalSignsDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        // Create the database table
        val createTableQuery = """
            CREATE TABLE ${MyDatabaseContract.VitalSignsEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_BLOOD_PRESSURE} TEXT,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_TEMPERATURE} REAL,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_PULSE_RATE} INTEGER,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_OXYGEN_LEVEL} INTEGER,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_WEIGHT} REAL,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_HEIGHT} REAL,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_BMI} REAL,
                ${MyDatabaseContract.VitalSignsEntry.COLUMN_LAST_SYNC} TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
    }

    companion object {
        private const val DATABASE_NAME = "vital_signs"
        private const val DATABASE_VERSION = 1
    }
}
