import { AppState, ScheduleData } from './types';

export const DAYS = [
  "السبت",
  "الأحد",
  "الاثنين",
  "الثلاثاء",
  "الأربعاء",
  "الخميس",
  "الجمعة"
];

export const DEFAULT_SCHOOL_HEADERS = [
  "الحصة الأولى (08:00 ص)",
  "الحصة الثانية (09:00 ص)",
  "الحصة الثالثة (10:00 ص)",
  "الحصة الرابعة (11:00 ص)",
  "الحصة الخامسة (12:00 م)",
  "الحصة السادسة (01:00 م)",
  "الحصة السابعة (02:00 م)",
  "الحصة الثامنة (03:00 م)"
];

export const DEFAULT_TUTORING_HEADERS = [
  "الساعة 01:00 م",
  "الساعة 02:00 م",
  "الساعة 03:00 م",
  "الساعة 04:00 م",
  "الساعة 05:00 م",
  "الساعة 06:00 م",
  "الساعة 07:00 م",
  "الساعة 08:00 م"
];

export const DEFAULT_STUDY_HEADERS = [
  "الساعة 03:00 م",
  "الساعة 04:00 م",
  "الساعة 05:00 م",
  "الساعة 06:00 م",
  "الساعة 07:00 م",
  "الساعة 08:00 م",
  "الساعة 09:00 م",
  "الساعة 10:00 م"
];

// Helper to generate empty grid
const createEmptyGrid = (headers: string[]): Record<string, Record<string, string>> => {
  const grid: Record<string, Record<string, string>> = {};
  DAYS.forEach(day => {
    grid[day] = {};
    headers.forEach(header => {
      grid[day][header] = "";
    });
  });
  return grid;
};

// Templates
export const getSchoolTemplate = (): Record<string, Record<string, string>> => {
  const grid = createEmptyGrid(DEFAULT_SCHOOL_HEADERS);
  
  grid["الأحد"]["الحصة الأولى (08:00 ص)"] = "رياضيات";
  grid["الأحد"]["الحصة الثانية (09:00 ص)"] = "لغة عربية";
  grid["الأحد"]["الحصة الثالثة (10:00 ص)"] = "فيزياء";
  grid["الأحد"]["الحصة الرابعة (11:00 ص)"] = "لغة إنجليزية";
  grid["الأحد"]["الحصة الخامسة (12:00 م)"] = "تربية إسلامية";
  
  grid["الاثنين"]["الحصة الأولى (08:00 ص)"] = "كيمياء";
  grid["الاثنين"]["الحصة الثانية (09:00 ص)"] = "أحياء";
  grid["الاثنين"]["الحصة الثالثة (10:00 ص)"] = "رياضيات";
  grid["الاثنين"]["الحصة الرابعة (11:00 ص)"] = "لغة عربية";
  grid["الاثنين"]["الحصة الخامسة (12:00 م)"] = "حاسب آلي";
  
  grid["الثلاثاء"]["الحصة الأولى (08:00 ص)"] = "فيزياء";
  grid["الثلاثاء"]["الحصة الثانية (09:00 ص)"] = "لغة إنجليزية";
  grid["الثلاثاء"]["الحصة الثالثة (10:00 ص)"] = "كيمياء";
  grid["الثلاثاء"]["الحصة الرابعة (11:00 ص)"] = "رياضيات";
  grid["الثلاثاء"]["الحصة الخامسة (12:00 م)"] = "تربية إسلامية";
  
  grid["الأربعاء"]["الحصة الأولى (08:00 ص)"] = "أحياء";
  grid["الأربعاء"]["الحصة الثانية (09:00 ص)"] = "رياضيات";
  grid["الأربعاء"]["الحصة الثالثة (10:00 ص)"] = "لغة عربية";
  grid["الأربعاء"]["الحصة الرابعة (11:00 ص)"] = "فيزياء";
  grid["الأربعاء"]["الحصة الخامسة (12:00 م)"] = "لغة إنجليزية";
  
  grid["الخميس"]["الحصة الأولى (08:00 ص)"] = "كيمياء";
  grid["الخميس"]["الحصة الثانية (09:00 ص)"] = "حاسب آلي";
  grid["الخميس"]["الحصة الثالثة (10:00 ص)"] = "تربية إسلامية";
  grid["الخميس"]["الحصة الرابعة (11:00 ص)"] = "لغة عربية";
  grid["الخميس"]["الحصة الخامسة (12:00 م)"] = "أحياء";
  
  return grid;
};

export const getTutoringTemplate = (): Record<string, Record<string, string>> => {
  const grid = createEmptyGrid(DEFAULT_TUTORING_HEADERS);
  grid["السبت"]["الساعة 04:00 م"] = "محاضرة فيزياء";
  grid["السبت"]["الساعة 07:00 م"] = "رياضيات - سنتر النخبة";
  grid["الاثنين"]["الساعة 04:00 م"] = "محاضرة كيمياء";
  grid["الأربعاء"]["الساعة 06:00 م"] = "لغة إنجليزية - أونلاين";
  return grid;
};

export const getStudyTemplate = (): Record<string, Record<string, string>> => {
  const grid = createEmptyGrid(DEFAULT_STUDY_HEADERS);
  DAYS.forEach(day => {
    if (day !== "الجمعة") {
      grid[day]["الساعة 05:00 م"] = "مراجعة دروس المدرسة";
      grid[day]["الساعة 07:00 م"] = "حل الواجبات الدراسية";
      grid[day]["الساعة 09:00 م"] = "مذاكرة رياضيات / كيمياء";
    } else {
      grid[day]["الساعة 05:00 م"] = "مراجعة أسبوعية عامة";
      grid[day]["الساعة 07:00 م"] = "تخطيط الأسبوع القادم";
    }
  });
  return grid;
};

export const INITIAL_STATE: AppState = {
  schoolSchedule: {
    zoomLevel: 100,
    headers: DEFAULT_SCHOOL_HEADERS,
    grid: createEmptyGrid(DEFAULT_SCHOOL_HEADERS),
    alarmConfig: {
      enabled: false,
      ringtoneName: "افتراضي",
      times: {
        "الحصة الأولى (08:00 ص)": "08:00",
        "الحصة الثانية (09:00 ص)": "09:00",
        "الحصة الثالثة (10:00 ص)": "10:00",
        "الحصة الرابعة (11:00 ص)": "11:00",
        "الحصة الخامسة (12:00 م)": "12:00",
        "الحصة السادسة (01:00 م)": "13:00",
        "الحصة السابعة (02:00 م)": "14:00",
        "الحصة الثامنة (03:00 م)": "15:00"
      }
    }
  },
  tutoringSchedule: {
    zoomLevel: 100,
    headers: DEFAULT_TUTORING_HEADERS,
    grid: createEmptyGrid(DEFAULT_TUTORING_HEADERS),
    alarmConfig: {
      enabled: true,
      ringtoneName: "افتراضي",
      times: {
        "الساعة 01:00 م": "13:00",
        "الساعة 02:00 م": "14:00",
        "الساعة 03:00 م": "15:00",
        "الساعة 04:00 م": "16:00",
        "الساعة 05:00 م": "17:00",
        "الساعة 06:00 م": "18:00",
        "الساعة 07:00 م": "19:00",
        "الساعة 08:00 م": "20:00"
      }
    }
  },
  studySchedule: {
    zoomLevel: 100,
    headers: DEFAULT_STUDY_HEADERS,
    grid: createEmptyGrid(DEFAULT_STUDY_HEADERS),
    alarmConfig: {
      enabled: true,
      ringtoneName: "افتراضي",
      times: {
        "الساعة 03:00 م": "15:00",
        "الساعة 04:00 م": "16:00",
        "الساعة 05:00 م": "17:00",
        "الساعة 06:00 م": "18:00",
        "الساعة 07:00 م": "19:00",
        "الساعة 08:00 م": "20:00",
        "الساعة 09:00 م": "21:00",
        "الساعة 10:00 م": "22:00"
      }
    }
  },
  tasks: [],
  studentName: "",
  enableNameCall: true,
  passcode: "1234",
  streakCount: 0,
  lastActiveDate: new Date().toISOString().split('T')[0]
} as AppState;
