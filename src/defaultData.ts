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
  "الحصة 1 (08:00 ص)",
  "الحصة 2 (09:00 ص)",
  "الحصة 3 (10:00 ص)",
  "الحصة 4 (11:00 ص)",
  "الحصة 5 (12:00 م)",
  "الحصة 6 (01:00 م)"
];

export const DEFAULT_TUTORING_HEADERS = [
  "الفترة 1 (03:00 م)",
  "الفترة 2 (04:30 م)",
  "الفترة 3 (06:00 م)",
  "الفترة 4 (07:30 م)"
];

export const DEFAULT_STUDY_HEADERS = [
  "الجلسة 1 (05:00 م)",
  "الجلسة 2 (07:00 م)",
  "الجلسة 3 (09:00 م)",
  "الجلسة 4 (10:30 م)"
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
  
  grid["الأحد"]["الحصة 1 (08:00 ص)"] = "رياضيات";
  grid["الأحد"]["الحصة 2 (09:00 ص)"] = "لغة عربية";
  grid["الأحد"]["الحصة 3 (10:00 ص)"] = "فيزياء";
  grid["الأحد"]["الحصة 4 (11:00 ص)"] = "لغة إنجليزية";
  grid["الأحد"]["الحصة 5 (12:00 م)"] = "تربية إسلامية";
  
  grid["الاثنين"]["الحصة 1 (08:00 ص)"] = "كيمياء";
  grid["الاثنين"]["الحصة 2 (09:00 ص)"] = "أحياء";
  grid["الاثنين"]["الحصة 3 (10:00 ص)"] = "رياضيات";
  grid["الاثنين"]["الحصة 4 (11:00 ص)"] = "لغة عربية";
  grid["الاثنين"]["الحصة 5 (12:00 م)"] = "حاسب آلي";
  
  grid["الثلاثاء"]["الحصة 1 (08:00 ص)"] = "فيزياء";
  grid["الثلاثاء"]["الحصة 2 (09:00 ص)"] = "لغة إنجليزية";
  grid["الثلاثاء"]["الحصة 3 (10:00 ص)"] = "كيمياء";
  grid["الثلاثاء"]["الحصة 4 (11:00 ص)"] = "رياضيات";
  grid["الثلاثاء"]["الحصة 5 (12:00 م)"] = "تربية إسلامية";
  
  grid["الأربعاء"]["الحصة 1 (08:00 ص)"] = "أحياء";
  grid["الأربعاء"]["الحصة 2 (09:00 ص)"] = "رياضيات";
  grid["الأربعاء"]["الحصة 3 (10:00 ص)"] = "لغة عربية";
  grid["الأربعاء"]["الحصة 4 (11:00 ص)"] = "فيزياء";
  grid["الأربعاء"]["الحصة 5 (12:00 م)"] = "لغة إنجليزية";
  
  grid["الخميس"]["الحصة 1 (08:00 ص)"] = "كيمياء";
  grid["الخميس"]["الحصة 2 (09:00 ص)"] = "حاسب آلي";
  grid["الخميس"]["الحصة 3 (10:00 ص)"] = "تربية إسلامية";
  grid["الخميس"]["الحصة 4 (11:00 ص)"] = "لغة عربية";
  grid["الخميس"]["الحصة 5 (12:00 م)"] = "أحياء";
  
  return grid;
};

export const getTutoringTemplate = (): Record<string, Record<string, string>> => {
  const grid = createEmptyGrid(DEFAULT_TUTORING_HEADERS);
  grid["السبت"]["الفترة 2 (04:30 م)"] = "محاضرة فيزياء";
  grid["السبت"]["الفترة 4 (07:30 م)"] = "رياضيات - سنتر النخبة";
  grid["الاثنين"]["الفترة 2 (04:30 م)"] = "محاضرة كيمياء";
  grid["الأربعاء"]["الفترة 3 (06:00 م)"] = "لغة إنجليزية - أونلاين";
  return grid;
};

export const getStudyTemplate = (): Record<string, Record<string, string>> => {
  const grid = createEmptyGrid(DEFAULT_STUDY_HEADERS);
  DAYS.forEach(day => {
    if (day !== "الجمعة") {
      grid[day]["الجلسة 1 (05:00 م)"] = "مراجعة دروس المدرسة";
      grid[day]["الجلسة 2 (07:00 م)"] = "حل الواجبات الدراسية";
      grid[day]["الجلسة 3 (09:00 م)"] = "مذاكرة رياضيات / كيمياء";
    } else {
      grid[day]["الجلسة 1 (05:00 م)"] = "مراجعة أسبوعية عامة";
      grid[day]["الجلسة 2 (07:00 م)"] = "تخطيط الأسبوع القادم";
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
        "الحصة 1 (08:00 ص)": "08:00",
        "الحصة 2 (09:00 ص)": "09:00",
        "الحصة 3 (10:00 ص)": "10:00",
        "الحصة 4 (11:00 ص)": "11:00",
        "الحصة 5 (12:00 م)": "12:00",
        "الحصة 6 (01:00 م)": "13:00"
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
        "الفترة 1 (03:00 م)": "15:00",
        "الفترة 2 (04:30 م)": "16:30",
        "الفترة 3 (06:00 م)": "18:00",
        "الفترة 4 (07:30 م)": "19:30"
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
        "الجلسة 1 (05:00 م)": "17:00",
        "الجلسة 2 (07:00 م)": "19:00",
        "الجلسة 3 (09:00 م)": "21:00",
        "الجلسة 4 (10:30 م)": "22:30"
      }
    }
  },
  tasks: [
    {
      id: "1",
      title: "حل واجب الرياضيات (الصفحة 42)",
      notes: "تمارين الهندسة وحساب المثلثات كاملة",
      priority: "high",
      dueDate: new Date(Date.now() + 86400000).toISOString().split('T')[0], // tomorrow
      completed: false
    },
    {
      id: "2",
      title: "تحضير تجربة الكيمياء",
      notes: "كتابة التقرير الخاص بالتفاعل الكيميائي لغدا",
      priority: "medium",
      dueDate: new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0],
      completed: false
    },
    {
      id: "3",
      title: "حفظ الكلمات الجديدة لغة إنجليزية",
      notes: "الوحدة الثالثة - الكلمات والترجمات المصاحبة",
      priority: "low",
      dueDate: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0],
      completed: true
    }
  ],
  studentName: "",
  enableNameCall: true,
  passcode: "1234",
  streakCount: 5,
  lastActiveDate: new Date().toISOString().split('T')[0]
};
