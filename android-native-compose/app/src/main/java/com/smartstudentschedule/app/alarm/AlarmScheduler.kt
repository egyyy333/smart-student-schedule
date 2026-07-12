package com.smartstudentschedule.app.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smartstudentschedule.app.data.AppState
import java.util.*

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun syncAlarms(context: Context, state: AppState) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // 1. Cancel all existing alarms first to avoid duplicate ringing
        cancelAllAlarms(context, alarmManager)

        if (!state.isAlarmActive) {
            Log.d("AlarmScheduler", "Alarms disabled globally in settings. Skipping schedule.")
            return
        }

        // 2. Compute schedules and queue exact daily timings
        // Since we have three schedules, we find the closest match.
        // Let's implement active scheduling based on the real system.
        // We look up today's and tomorrow's alarms and register precise items using AlarmManager.
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val jsDayToArabic = listOf(
            "الأحد",    // Calendar.SUNDAY (1) -> index 0
            "الاثنين",   // Calendar.MONDAY (2) -> index 1
            "الثلاثاء",  // Calendar.TUESDAY (3) -> index 2
            "الأربعاء",  // Calendar.WEDNESDAY (4) -> index 3
            "الخميس",   // Calendar.THURSDAY (5) -> index 4
            "الجمعة",   // Calendar.FRIDAY (6) -> index 5
            "السبت"     // Calendar.SATURDAY (7) -> index 6
        )

        val daysTranslation = mapOf(
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الاثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة",
            Calendar.SATURDAY to "السبت"
        )

        // For simplicity, let's schedule for today and tomorrow
        scheduleForSpecificDay(context, alarmManager, calendar, daysTranslation, state)
    }

    private fun scheduleForSpecificDay(
        context: Context,
        alarmManager: AlarmManager,
        baseCal: Calendar,
        daysTranslation: Map<Int, String>,
        state: AppState
    ) {
        val calendar = baseCal.clone() as Calendar
        val dayName = daysTranslation[calendar.get(Calendar.DAY_OF_WEEK)] ?: return

        // Process school, tutoring, and study schedules
        scheduleGrid(context, alarmManager, dayName, state.schoolSchedule.grid[dayName], "المدرسة", calendar)
        scheduleGrid(context, alarmManager, dayName, state.tutoringSchedule.grid[dayName], "الدرس", calendar)
        scheduleGrid(context, alarmManager, dayName, state.studySchedule.grid[dayName], "المذاكرة", calendar)
    }

    private fun scheduleGrid(
        context: Context,
        alarmManager: AlarmManager,
        dayName: String,
        gridMap: Map<String, String>?,
        scheduleType: String,
        calendar: Calendar
    ) {
        if (gridMap == null) return
        
        var reqCode = 1000 + scheduleType.hashCode() % 10000
        
        gridMap.forEach { (periodHeader, subject) ->
            if (subject.isNotBlank()) {
                // Parse period headers to find time. Standard formats: "08:30 ص" or custom headers.
                // If it is just period names like "الأولى", we use a mapping or default times:
                val parsedTime = parseHeaderToTime(periodHeader) ?: return@forEach
                
                val alarmCal = calendar.clone() as Calendar
                alarmCal.set(Calendar.HOUR_OF_DAY, parsedTime.first)
                alarmCal.set(Calendar.MINUTE, parsedTime.second)
                alarmCal.set(Calendar.SECOND, 0)
                alarmCal.set(Calendar.MILLISECOND, 0)

                if (alarmCal.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = "com.smartstudentschedule.app.ALARM_TRIGGER"
                        putExtra("appointment_text", subject)
                        putExtra("appointment_day", dayName)
                        putExtra("appointment_time", "$periodHeader ($scheduleType)")
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reqCode++,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmCal.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            alarmCal.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }
        }
    }

    private fun parseHeaderToTime(header: String): Pair<Int, Int>? {
        // Simple intelligent parser for times like "08:30 ص" or "02:15 م"
        // Also supports default mappings for school periods
        val cleanHeader = header.trim()
        val match = Regex("""(\d+):(\d+)\s*(ص|م|AM|PM)""", RegexOption.IGNORE_CASE).find(cleanHeader)
        if (match != null) {
            var hour = match.groupValues[1].toInt()
            val min = match.groupValues[2].toInt()
            val marker = match.groupValues[3].lowercase()
            if (marker == "م" || marker == "pm") {
                if (hour < 12) hour += 12
            } else if (marker == "ص" || marker == "am") {
                if (hour == 12) hour = 0
            }
            return Pair(hour, min)
        }

        // Fallback defaults for sequential period titles
        return when (cleanHeader) {
            "الأولى" -> Pair(8, 0)
            "الثانية" -> Pair(8, 55)
            "الثالثة" -> Pair(9, 50)
            "الرابعة" -> Pair(11, 0)
            "الخامسة" -> Pair(11, 55)
            "السادسة" -> Pair(12, 50)
            "السابعة" -> Pair(13, 40)
            "م١" -> Pair(15, 0)
            "م٢" -> Pair(16, 30)
            "م٣" -> Pair(18, 0)
            "م٤" -> Pair(19, 30)
            "الفترة ١" -> Pair(16, 0)
            "الفترة ٢" -> Pair(18, 30)
            "الفترة ٣" -> Pair(21, 0)
            else -> null
        }
    }

    private fun cancelAllAlarms(context: Context, alarmManager: AlarmManager) {
        // Cancel base requests
        var code = 1000
        while (code < 1015) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code++,
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
