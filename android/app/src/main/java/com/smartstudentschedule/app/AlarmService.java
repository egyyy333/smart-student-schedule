package com.smartstudentschedule.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class AlarmService extends Service {
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;

    public static AlarmService activeService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        activeService = this;

        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartSchedule::AlarmServiceWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes max
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if ("ACTION_DISMISS_ALARM".equals(action)) {
            stopAlarmService();
            return START_NOT_STICKY;
        }

        String appointmentText = intent.getStringExtra("appointment_text");
        if (appointmentText == null) appointmentText = "حصة دراسية";
        String appointmentDay = intent.getStringExtra("appointment_day");
        if (appointmentDay == null) appointmentDay = "";
        String appointmentTime = intent.getStringExtra("appointment_time");
        if (appointmentTime == null) appointmentTime = "";

        String channelId = "appointments_alarm_service_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                "رنين المنبه النشط",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("رنين منبه الحصص والمذاكرة النشط");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        // Launch MainActivity (which is our Capacitor WebView app)
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.putExtra("appointment_text", appointmentText);
        activityIntent.putExtra("appointment_day", appointmentDay);
        activityIntent.putExtra("appointment_time", appointmentTime);
        activityIntent.putExtra("trigger_alarm_overlay", "true");
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان موعد الحصة أو المذاكرة ⏰")
            .setContentText(appointmentDay + " - الساعة " + appointmentTime + ": " + appointmentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build();

        startForeground(999, notification);

        startRinging();

        // Auto-launch MainActivity
        try {
            startActivity(activityIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_NOT_STICKY;
    }

    private void startRinging() {
        // Vibrator
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
                } else {
                    vibrator.vibrate(new long[]{0, 500, 500}, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // MediaPlayer
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alert);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                );
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void stopAlarmService() {
        stopSelf();
    }

    public void snoozeAlarmService(Context context, String appointmentText, String appointmentDay, String appointmentTime, String ringtoneUri) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("appointment_text", appointmentText + " (تأجيل)");
        intent.putExtra("appointment_day", appointmentDay);
        intent.putExtra("appointment_time", appointmentTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            9999, // Special request code for snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }

        stopSelf();
    }

    @Override
    public void onDestroy() {
        activeService = null;
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mediaPlayer = null;
        }

        try {
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            vibrator = null;
        }

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wakeLock = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
