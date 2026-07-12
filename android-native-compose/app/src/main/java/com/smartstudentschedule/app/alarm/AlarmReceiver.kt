package com.smartstudentschedule.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smartstudentschedule.app.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Broadcast received with action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            // Re-schedule all alarms on system restart
            val repo = AppRepository(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val state = repo.appStateFlow.first()
                AlarmScheduler.syncAlarms(context, state)
            }
        } else if ("com.smartstudentschedule.app.ALARM_TRIGGER" == action) {
            val appointmentText = intent.getStringExtra("appointment_text") ?: ""
            val appointmentDay = intent.getStringExtra("appointment_day") ?: ""
            val appointmentTime = intent.getStringExtra("appointment_time") ?: ""

            // Start foreground alarm ringer service (Oreo+)
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("appointment_text", appointmentText)
                putExtra("appointment_day", appointmentDay)
                putExtra("appointment_time", appointmentTime)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
