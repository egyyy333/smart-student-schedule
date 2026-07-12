package com.smartstudentschedule.app;

import android.app.KeyguardManager;
import android.content.Context;
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
        
        // Handle lockscreen flags as early as possible in onCreate
        Intent intent = getIntent();
        if (intent != null && "true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            applyLockscreenFlags();
        }
        
        super.onCreate(savedInstanceState);
    }

    private void applyLockscreenFlags() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                    setTurnScreenOn(true);
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    if (keyguardManager != null) {
                        keyguardManager.requestDismissKeyguard(MainActivity.this, null);
                    }
                }
                getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && "true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            applyLockscreenFlags();
        }
        handleIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AlarmService.activeService == null) {
            clearAlarmFlags();
        } else {
            // Re-apply flags if alarm is active to make sure it shows over lock screen
            applyLockscreenFlags();
        }
        handleIntent(getIntent());
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
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            applyLockscreenFlags();

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
            Intent mainIntent = getIntent();
            if (mainIntent != null) {
                mainIntent.removeExtra("trigger_alarm_overlay");
            }
            
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
