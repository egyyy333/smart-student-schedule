package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Configs
    @Query("SELECT * FROM configs WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: ConfigEntity)

    // Hour Headers
    @Query("SELECT * FROM hour_headers ORDER BY hourIndex ASC")
    fun getHourHeaders(): Flow<List<HourHeaderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHourHeaders(headers: List<HourHeaderEntity>)

    @Query("SELECT COUNT(*) FROM hour_headers")
    suspend fun getHourHeadersCount(): Int

    // Appointment Cells
    @Query("SELECT * FROM appointment_cells")
    fun getAppointmentCells(): Flow<List<AppointmentCellEntity>>

    @Query("SELECT * FROM appointment_cells")
    suspend fun getAppointmentCellsList(): List<AppointmentCellEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppointmentCells(cells: List<AppointmentCellEntity>)

    // Students
    @Query("SELECT * FROM students ORDER BY id ASC")
    fun getStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudent(student: StudentEntity): Long

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Int)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    // Month Headers
    @Query("SELECT * FROM month_headers ORDER BY monthIndex ASC")
    fun getMonthHeaders(): Flow<List<MonthHeaderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthHeaders(headers: List<MonthHeaderEntity>)

    @Query("SELECT COUNT(*) FROM month_headers")
    suspend fun getMonthHeadersCount(): Int

    // Payments
    @Query("SELECT * FROM payments")
    fun getPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE studentId = :studentId")
    suspend fun deletePaymentsForStudent(studentId: Int)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    @Query("DELETE FROM hour_headers")
    suspend fun deleteAllHourHeaders()

    @Query("DELETE FROM appointment_cells")
    suspend fun deleteAllAppointmentCells()

    @Query("DELETE FROM month_headers")
    suspend fun deleteAllMonthHeaders()

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}
