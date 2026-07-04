package com.smartstudentschedule.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONException;
import java.util.Calendar;

@CapacitorPlugin(name = "AlarmPlugin")
public class AlarmPlugin extends Plugin {

    @PluginMethod
    public void setAlarms(PluginCall call) {
        JSArray alarms = call.getArray("alarms");
        if (alarms == null) {
            call.reject("Alarms array is required");
            return;
        }

        Context context = getContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // First cancel all previous exact alarms to avoid orphans
        cancelAllAlarmsInternal(context, alarmManager);

        try {
            for (int i = 0; i < alarms.length(); i++) {
                JSObject alarm = JSObject.fromJSONObject(alarms.getJSONObject(i));
                int dayIndex = alarm.getInteger("dayIndex"); // 0: Sat, 1: Sun, ..., 6: Fri
                int hourIndex = alarm.getInteger("hourIndex"); // 0 to 7
                String timeStr = alarm.getString("time"); // e.g., "15:00"
                String subject = alarm.getString("subject");
                String header = alarm.getString("header");
                boolean enabled = alarm.getBoolean("enabled", false);

                if (enabled && subject != null && !subject.trim().isEmpty() && timeStr != null && timeStr.contains(":")) {
                    String[] parts = timeStr.split(":");
                    if (parts.length == 2) {
                        int alarmHour = Integer.parseInt(parts[0]);
                        int alarmMinute = Integer.parseInt(parts[1]);

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, alarmHour);
                        calendar.set(Calendar.MINUTE, alarmMinute);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        int currentCalDay = calendar.get(Calendar.DAY_OF_WEEK);
                        int targetCalDay = mapDayIndexToCalendar(dayIndex);
                        int daysDiff = targetCalDay - currentCalDay;
                        if (daysDiff < 0) {
                            daysDiff += 7;
                        }
                        calendar.add(Calendar.DAY_OF_YEAR, daysDiff);

                        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_YEAR, 7);
                        }

                        int requestCode = dayIndex * 100 + hourIndex;

                        Intent intent = new Intent(context, AlarmReceiver.class);
                        intent.putExtra("appointment_text", subject);
                        intent.putExtra("appointment_day", mapDayIndexToDayName(dayIndex));
                        intent.putExtra("appointment_time", timeStr);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
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
                    }
                }
            }
            call.resolve();
        } catch (Exception e) {
            e.printStackTrace();
            call.reject("Error setting alarms: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopAlarm(PluginCall call) {
        if (AlarmService.activeService != null) {
            AlarmService.activeService.stopAlarmService();
        }
        call.resolve();
    }

    @PluginMethod
    public void snoozeAlarm(PluginCall call) {
        String subject = call.getString("subject");
        String day = call.getString("day");
        String time = call.getString("time");

        Context context = getContext();
        if (AlarmService.activeService != null) {
            AlarmService.activeService.snoozeAlarmService(context, subject, day, time, "");
        }
        call.resolve();
    }

    @PluginMethod
    public void cancelAllAlarms(PluginCall call) {
        Context context = getContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        cancelAllAlarmsInternal(context, alarmManager);
        call.resolve();
    }

    private void cancelAllAlarmsInternal(Context context, AlarmManager alarmManager) {
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 8; h++) {
                int requestCode = d * 100 + h;
                Intent intent = new Intent(context, AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }

    private int mapDayIndexToCalendar(int dayIndex) {
        switch (dayIndex) {
            case 0: return Calendar.SATURDAY;
            case 1: return Calendar.SUNDAY;
            case 2: return Calendar.MONDAY;
            case 3: return Calendar.TUESDAY;
            case 4: return Calendar.WEDNESDAY;
            case 5: return Calendar.THURSDAY;
            case 6: return Calendar.FRIDAY;
            default: return Calendar.SATURDAY;
        }
    }

    private String mapDayIndexToDayName(int dayIndex) {
        String[] days = {"السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة"};
        if (dayIndex >= 0 && dayIndex < 7) {
            return days[dayIndex];
        }
        return "السبت";
    }
}
