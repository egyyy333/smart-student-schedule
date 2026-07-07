package com.smartstudentschedule.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    public static String pendingSubject = null;
    public static String pendingDay = null;
    public static String pendingTime = null;
    public static boolean hasPendingAlarm = false;
    public static boolean isAlarmLaunch = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmPlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent == null || !"true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            clearAlarmFlags();
        }
        handleIntent(intent);
    }

    public void clearAlarmFlags() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false);
                    setTurnScreenOn(false);
                }
                getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                );
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            // Wake screen and show over lock screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            }
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );

            String text = intent.getStringExtra("appointment_text");
            String day = intent.getStringExtra("appointment_day");
            String time = intent.getStringExtra("appointment_time");
            
            pendingSubject = text;
            pendingDay = day;
            pendingTime = time;
            hasPendingAlarm = true;
            isAlarmLaunch = true;
            
            // Clear the trigger so it doesn't fire repeatedly
            intent.removeExtra("trigger_alarm_overlay");
            
            // Dispatch JS event to our React web application
            final String textEscaped = text != null ? text.replace("\"", "\\\"") : "";
            final String dayEscaped = day != null ? day.replace("\"", "\\\"") : "";
            final String timeEscaped = time != null ? time.replace("\"", "\\\"") : "";
            
            getBridge().getWebView().post(new Runnable() {
                @Override
                public void run() {
                    getBridge().triggerJSEvent("alarmTriggered", "window", 
                        "{ \"subject\": \"" + textEscaped + "\", \"day\": \"" + dayEscaped + "\", \"time\": \"" + timeEscaped + "\" }");
                }
            });
        }
    }
}
