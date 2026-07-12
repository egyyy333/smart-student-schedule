package com.smartstudentschedule.app.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartstudentschedule.app.MainActivity

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        
        val action = intent.action
        if ("ACTION_DISMISS_ALARM" == action) {
            stopSelf()
            return START_NOT_STICKY
        } else if ("ACTION_SNOOZE_ALARM" == action) {
            snoozeAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val appointmentText = intent.getStringExtra("appointment_text") ?: "مذاكرة / حصة"
        val appointmentDay = intent.getStringExtra("appointment_day") ?: ""
        val appointmentTime = intent.getStringExtra("appointment_time") ?: ""

        // WAKE UP SCREEN PHYSICAL FORCE
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "SmartSchedule::AlarmScreenWakeLock"
                )
                wakeLock.acquire(15000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val channelId = "appointments_alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "المنبه النشط للحصص",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة منبه جدول الطالب الذكي لتنبيهك فورا في الموعد"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Fullscreen Overlay Intent
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("trigger_alarm_overlay", "true")
            putExtra("appointment_text", appointmentText)
            putExtra("appointment_day", appointmentDay)
            putExtra("appointment_time", appointmentTime)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            999,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply { action = "ACTION_DISMISS_ALARM" }
        val dismissPendingIntent = PendingIntent.getService(this, 123, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(this, AlarmService::class.java).apply { action = "ACTION_SNOOZE_ALARM" }
        val snoozePendingIntent = PendingIntent.getService(this, 124, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان موعد الحصة أو المذاكرة ⏰")
            .setContentText("$appointmentText | $appointmentTime")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق المنبه", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "عمل غفوة (١٠ دقائق)", snoozePendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(999, notification)
        }

        startRingingAndVibration()

        // Launch MainActivity as fallback
        try {
            startActivity(activityIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return START_NOT_STICKY
    }

    private fun startRingingAndVibration() {
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start ringtone", e)
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 800, 800, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snoozeAlarm() {
        // Schedule snooze alarm for 10 minutes in the future
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.smartstudentschedule.app.ALARM_TRIGGER"
            putExtra("appointment_text", "غفوة المنبه النشط")
            putExtra("appointment_day", "")
            putExtra("appointment_time", "بعد ١٠ دقائق")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeCal.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeCal.timeInMillis, pendingIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
    }
}
