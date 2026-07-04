package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppointmentCellEntity
import com.example.data.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmScheduler {

    private val WEEK_DAYS = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

    fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val dao = db.quranDao()

                val alarmEnabled = dao.getConfig("alarm_enabled")?.value?.toBoolean() ?: false
                val ringtoneUri = dao.getConfig("alarm_ringtone_uri")?.value ?: ""

                val alarmTimes = mutableMapOf<Int, String>()
                for (h in 0..7) {
                    val defaultTime = when (h) {
                        0 -> "12:00"
                        1 -> "13:00"
                        2 -> "14:00"
                        3 -> "15:00"
                        4 -> "16:00"
                        5 -> "17:00"
                        6 -> "18:00"
                        7 -> "19:00"
                        else -> "12:00"
                    }
                    alarmTimes[h] = dao.getConfig("alarm_time_$h")?.value ?: defaultTime
                }

                val savedCells = dao.getAppointmentCellsList()
                scheduleAlarms(context, alarmEnabled, alarmTimes, ringtoneUri, savedCells)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scheduleAlarms(
        context: Context,
        alarmEnabled: Boolean,
        alarmTimes: Map<Int, String>,
        alarmRingtoneUri: String,
        savedCells: List<AppointmentCellEntity>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cellMap = savedCells.associate { (it.dayIndex to it.hourIndex) to it.content }

        for (d in 0..6) {
            for (h in 0..7) {
                val cellContent = cellMap[d to h] ?: ""
                val requestCode = d * 100 + h

                if (alarmEnabled && cellContent.trim().isNotEmpty()) {
                    val timeStr = alarmTimes[h] ?: "12:00"
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val alarmHour = parts[0].toIntOrNull() ?: 12
                        val alarmMinute = parts[1].toIntOrNull() ?: 0

                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, alarmHour)
                            set(Calendar.MINUTE, alarmMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)

                            val currentCalDay = get(Calendar.DAY_OF_WEEK)
                            val targetCalDay = mapDayIndexToCalendar(d)
                            var daysDiff = targetCalDay - currentCalDay
                            if (daysDiff < 0) {
                                daysDiff += 7
                            }
                            add(Calendar.DAY_OF_YEAR, daysDiff)

                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                        }

                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("appointment_text", cellContent)
                            putExtra("appointment_day", WEEK_DAYS[d])
                            putExtra("appointment_time", timeStr)
                            putExtra("ringtone_uri", alarmRingtoneUri)
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            }
                        } catch (e: SecurityException) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                } else {
                    // Cancel only this specific pending intent!
                    val intent = Intent(context, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                }
            }
        }
    }

    private fun mapDayIndexToCalendar(dayIndex: Int): Int {
        return when (dayIndex) {
            0 -> Calendar.SATURDAY
            1 -> Calendar.SUNDAY
            2 -> Calendar.MONDAY
            3 -> Calendar.TUESDAY
            4 -> Calendar.WEDNESDAY
            5 -> Calendar.THURSDAY
            6 -> Calendar.FRIDAY
            else -> Calendar.SATURDAY
        }
    }
}
