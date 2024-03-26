import android.provider.BaseColumns

data class VitalSignsModel(
    val bloodPressure: String,
    val temperature: Double,
    val pulseRate: Int,
    val oxygenLevel: Int,
    val weight: Double,
    val height: Double,
    val BMI: Double,
) {
}

object MyDatabaseContract {
    object VitalSignsEntry : BaseColumns {
        const val TABLE_NAME = "vital_signs"
        const val COLUMN_BLOOD_PRESSURE = "blood_pressure"
        const val COLUMN_TEMPERATURE = "temperature"
        const val COLUMN_PULSE_RATE = "pulse_rate"
        const val COLUMN_OXYGEN_LEVEL = "oxygen_level"
        const val COLUMN_WEIGHT = "weight"
        const val COLUMN_HEIGHT = "height"
        const val COLUMN_BMI = "bmi"
        const val COLUMN_LAST_SYNC = "last_sync"

        // SQL command to create the table with the "Last Sync" column
        const val SQL_CREATE_ENTRIES = "CREATE TABLE $TABLE_NAME (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "$COLUMN_BLOOD_PRESSURE TEXT," +
                "$COLUMN_TEMPERATURE REAL," +
                "$COLUMN_PULSE_RATE INTEGER," +
                "$COLUMN_OXYGEN_LEVEL INTEGER," +
                "$COLUMN_WEIGHT REAL," +
                "$COLUMN_HEIGHT REAL," +
                "$COLUMN_BMI REAL," +
                "$COLUMN_LAST_SYNC TEXT)" //! Include the "Last Sync" column in the table creation
    }
}
