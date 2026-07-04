export interface Task {
  id: string;
  title: string;
  notes: string;
  priority: 'high' | 'medium' | 'low';
  dueDate: string;
  completed: boolean;
}

export interface AlarmConfig {
  enabled: boolean;
  ringtoneName: string;
  times: Record<string, string>; // Maps column header key to time string (e.g., "16:00")
}

export interface ScheduleData {
  zoomLevel: number; // Percentage (e.g., 100)
  headers: string[]; // Custom headers for the periods/hours (e.g., ["الحصة 1", "الحصة 2"])
  grid: Record<string, Record<string, string>>; // Maps day -> header -> subject name
  alarmConfig: AlarmConfig;
}

export interface AppState {
  schoolSchedule: ScheduleData;
  tutoringSchedule: ScheduleData;
  studySchedule: ScheduleData;
  tasks: Task[];
  studentName: string;
  enableNameCall: boolean;
  passcode: string;
  streakCount: number;
  lastActiveDate: string;
}
