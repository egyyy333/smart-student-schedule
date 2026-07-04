package com.smartstudentschedule.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("ACTION_DISMISS_ALARM".equals(action)) {
            if (AlarmService.activeService != null) {
                AlarmService.activeService.stopAlarmService();
            }
            return;
        }

        String appointmentText = intent.getStringExtra("appointment_text");
        String appointmentDay = intent.getStringExtra("appointment_day");
        String appointmentTime = intent.getStringExtra("appointment_time");

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("appointment_text", appointmentText);
        serviceIntent.putExtra("appointment_day", appointmentDay);
        serviceIntent.putExtra("appointment_time", appointmentTime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
