package com.example.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        var activeService: AlarmService? = null
    }

    override fun onCreate() {
        super.onCreate()
        activeService = this

        // Acquire WakeLock
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "QuranApp::AlarmServiceWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes max */)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appointmentText = intent?.getStringExtra("appointment_text") ?: "حصة قرآنية"
        val appointmentDay = intent?.getStringExtra("appointment_day") ?: ""
        val appointmentTime = intent?.getStringExtra("appointment_time") ?: ""
        val ringtoneUri = intent?.getStringExtra("ringtone_uri") ?: ""

        val channelId = "appointments_alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "رنين المنبه النشط",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "رنين منبه الحصص القرآنية النشط"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Full-screen Activity Intent
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("appointment_text", appointmentText)
            putExtra("appointment_day", appointmentDay)
            putExtra("appointment_time", appointmentTime)
            putExtra("ringtone_uri", ringtoneUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions: Stop / Dismiss
        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "ACTION_DISMISS_ALARM"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions: Snooze
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "ACTION_SNOOZE_ALARM"
            putExtra("appointment_text", appointmentText)
            putExtra("appointment_day", appointmentDay)
            putExtra("appointment_time", appointmentTime)
            putExtra("ringtone_uri", ringtoneUri)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            1002,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان موعد الحصة القرآنية ⏰")
            .setContentText("$appointmentDay - الساعة $appointmentTime: $appointmentText")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "تأجيل (10 د)", snoozePendingIntent)
            .build()

        startForeground(999, notification)

        // Start ringing and vibrating
        startRinging(ringtoneUri)

        // Auto-launch Full Screen Activity over Lockscreen
        try {
            startActivity(activityIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return START_NOT_STICKY
    }

    private fun startRinging(uriStr: String) {
        // Vibrator setup
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 500),
                        0 // Loop from index 0
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Media player setup
        try {
            val uri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null
            mediaPlayer = MediaPlayer().apply {
                if (uri != null) {
                    setDataSource(this@AlarmService, uri)
                } else {
                    val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                        ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    setDataSource(this@AlarmService, defaultUri)
                }
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
            e.printStackTrace()
            try {
                mediaPlayer = MediaPlayer.create(
                    this,
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ).apply {
                    isLooping = true
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun stopAlarmService() {
        stopSelf()
    }

    fun snoozeAlarmService(context: Context, appointmentText: String, appointmentDay: String, appointmentTime: String, ringtoneUri: String) {
        // Schedule snooze alarm 10 minutes from now
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 10)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("appointment_text", "$appointmentText (تأجيل)")
            putExtra("appointment_day", appointmentDay)
            putExtra("appointment_time", appointmentTime)
            putExtra("ringtone_uri", ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999, // Special code for snooze
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
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        stopSelf()
    }

    override fun onDestroy() {
        activeService = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            vibrator = null
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            wakeLock = null
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
