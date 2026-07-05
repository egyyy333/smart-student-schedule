import { registerPlugin } from '@capacitor/core';
import { AppState } from '../types';
import { DAYS } from '../defaultData';

export const AlarmPlugin = registerPlugin<any>('AlarmPlugin');

export async function syncAndroidAlarms(state: AppState) {
  try {
    const alarmsToSet: Array<{
      dayIndex: number;
      hourIndex: number;
      time: string;
      subject: string;
      header: string;
      enabled: boolean;
    }> = [];

    // 1. Tutoring schedule (الدروس الخصوصية) - Link Alarms!
    const tutoring = state.tutoringSchedule;
    if (tutoring && tutoring.alarmConfig && tutoring.alarmConfig.enabled) {
      const headers = tutoring.headers;
      const grid = tutoring.grid;
      const times = tutoring.alarmConfig.times;

      DAYS.forEach((day, dIdx) => {
        headers.forEach((header, hIdx) => {
          const subject = grid[day]?.[header] || "";
          const time = times[header] || "";
          if (subject && time) {
            alarmsToSet.push({
              dayIndex: dIdx,
              hourIndex: hIdx,
              time: time,
              subject: subject,
              header: header,
              enabled: true
            });
          }
        });
      });
    }

    // 2. Study schedule (المذاكرة والمراجعة) - Link Alarms!
    const study = state.studySchedule;
    if (study && study.alarmConfig && study.alarmConfig.enabled) {
      const headers = study.headers;
      const grid = study.grid;
      const times = study.alarmConfig.times;

      DAYS.forEach((day, dIdx) => {
        headers.forEach((header, hIdx) => {
          const subject = grid[day]?.[header] || "";
          const time = times[header] || "";
          if (subject && time) {
            alarmsToSet.push({
              dayIndex: dIdx,
              hourIndex: hIdx,
              time: time,
              subject: subject,
              header: header,
              enabled: true
            });
          }
        });
      });
    }

    // Call the native bridge
    await AlarmPlugin.setAlarms({ alarms: alarmsToSet });
    console.log("Successfully synchronized alarms with native Android subsystem:", alarmsToSet);
  } catch (err) {
    console.warn("Native AlarmPlugin is not available on this platform (or not initialized):", err);
  }
}
