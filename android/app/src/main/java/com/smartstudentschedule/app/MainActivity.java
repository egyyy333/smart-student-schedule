package com.smartstudentschedule.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable showing over keyguard and waking the screen up
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "true".equals(intent.getStringExtra("trigger_alarm_overlay"))) {
            String text = intent.getStringExtra("appointment_text");
            String day = intent.getStringExtra("appointment_day");
            String time = intent.getStringExtra("appointment_time");
            
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
