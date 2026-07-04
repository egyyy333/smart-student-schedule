package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store app configurations like current password.
 */
@Entity(tableName = "configs")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

/**
 * Entity to store the custom names of the hour headers (from 12 to 7, total 8 slots).
 */
@Entity(tableName = "hour_headers")
data class HourHeaderEntity(
    @PrimaryKey val hourIndex: Int,
    val name: String
)

/**
 * Entity to store a cell's written text in the Appointments schedule.
 * dayIndex: 0 (Saturday) to 6 (Friday)
 * hourIndex: 0 (index for 1st hour) to 7 (index for 8th hour)
 */
@Entity(tableName = "appointment_cells", primaryKeys = ["dayIndex", "hourIndex"])
data class AppointmentCellEntity(
    val dayIndex: Int,
    val hourIndex: Int,
    val content: String
)

/**
 * Entity to store Quran student names.
 */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Entity to store the custom names of the months (January to June, total 6 slots).
 */
@Entity(tableName = "month_headers")
data class MonthHeaderEntity(
    @PrimaryKey val monthIndex: Int,
    val name: String
)

/**
 * Entity to store whether a student has paid for a particular month.
 * monthIndex: 0 (January/1st month) to 5 (June/6th month)
 */
@Entity(tableName = "payments", primaryKeys = ["studentId", "monthIndex"])
data class PaymentEntity(
    val studentId: Int,
    val monthIndex: Int,
    val paid: Boolean
)
