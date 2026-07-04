package com.example.ui

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == "ACTION_DISMISS_ALARM") {
            AlarmService.activeService?.stopAlarmService()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(999)
            return
        }
        
        if (action == "ACTION_SNOOZE_ALARM") {
            val appointmentText = intent.getStringExtra("appointment_text") ?: "حصة قرآنية"
            val appointmentDay = intent.getStringExtra("appointment_day") ?: ""
            val appointmentTime = intent.getStringExtra("appointment_time") ?: ""
            val ringtoneUri = intent.getStringExtra("ringtone_uri") ?: ""
            
            AlarmService.activeService?.snoozeAlarmService(
                context, appointmentText, appointmentDay, appointmentTime, ringtoneUri
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(999)
            return
        }

        val appointmentText = intent.getStringExtra("appointment_text") ?: "حصة قرآنية"
        val appointmentDay = intent.getStringExtra("appointment_day") ?: ""
        val appointmentTime = intent.getStringExtra("appointment_time") ?: ""
        val ringtoneUri = intent.getStringExtra("ringtone_uri") ?: ""

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("appointment_text", appointmentText)
            putExtra("appointment_day", appointmentDay)
            putExtra("appointment_time", appointmentTime)
            putExtra("ringtone_uri", ringtoneUri)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Start AlarmActivity directly
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("appointment_text", appointmentText)
                putExtra("appointment_day", appointmentDay)
                putExtra("appointment_time", appointmentTime)
                putExtra("ringtone_uri", ringtoneUri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                context.startActivity(activityIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
