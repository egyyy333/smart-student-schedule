import { useState, useEffect } from 'react';
import { AppState } from './types';
import { INITIAL_STATE } from './defaultData';
import PasscodeLock from './components/PasscodeLock';
import AlarmOverlay from './components/AlarmOverlay';
import Dashboard from './components/Dashboard';
import SchedulesTab from './components/SchedulesTab';
import TasksTab from './components/TasksTab';
import SettingsTab from './components/SettingsTab';
import { GraduationCap, Home, Calendar, CheckSquare, Settings, Lock, Flame } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { syncAndroidAlarms, AlarmPlugin } from './utils/alarmSync';

const STORAGE_KEY = 'smart_student_schedule_state';

export default function App() {
  // 1. Core State Initialization with LocalStorage persistence
  const [state, setState] = useState<AppState>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        // Make sure newly added fields are initialized safely if present from old versions
        return { ...INITIAL_STATE, ...parsed };
      } catch (e) {
        console.error("Failed to parse saved state", e);
        return INITIAL_STATE;
      }
    }
    return INITIAL_STATE;
  });

  // Save state to local storage whenever it changes
  const handleSaveState = (newState: AppState) => {
    setState(newState);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newState));
    // Synchronize alarms natively
    syncAndroidAlarms(newState);
  };

  // Sync alarms on initial mount
  useEffect(() => {
    syncAndroidAlarms(state);
  }, []);

  // 2. Lock screen verification state
  const [isUnlocked, setIsUnlocked] = useState<boolean>(false);

  // 3. Tab routing state
  const [activeTab, setActiveTab] = useState<string>('dashboard');

  // 4. Background Alarm Listener & Overlay trigger state
  const [activeAlarm, setActiveAlarm] = useState<{
    subjectName: string;
    periodName: string;
    scheduleType: 'school' | 'tutoring' | 'study';
    isTest?: boolean;
  } | null>(null);

  const [lastFiredAlarm, setLastFiredAlarm] = useState<string>('');

  // 5. JavaScript Native Event Listener for lockscreen alarms
  useEffect(() => {
    const handleNativeAlarm = (event: any) => {
      console.log("Custom native alarm event received in React application:", event);
      
      const detail = event.detail;
      let payload = detail;
      if (typeof detail === 'string') {
        try {
          payload = JSON.parse(detail);
        } catch (e) {
          console.error("Failed to parse native alarm detail string", e);
        }
      } else if (!detail && event.subject) {
        payload = event;
      }

      if (payload && payload.subject) {
        // Show the alarm overlay immediately (rendered on top at z-[1000] layer)
        setActiveAlarm({
          subjectName: payload.subject,
          periodName: payload.time || payload.header || 'المنبه الموقوت',
          scheduleType: 'tutoring', // Default to tutoring
          isTest: payload.isTest || false
        });
      }
    };

    // Cold-start pending alarm retrieval (if app was booted fresh by system alarm manager)
    const checkPendingAlarmOnStartup = async () => {
      try {
        if (AlarmPlugin && typeof AlarmPlugin.getPendingAlarm === 'function') {
          const res = await AlarmPlugin.getPendingAlarm();
          if (res && res.hasPendingAlarm) {
            console.log("Found pending native alarm on startup:", res);
            setActiveAlarm({
              subjectName: res.subject || 'المنبه الموقوت',
              periodName: res.time || 'الحصة الحالية',
              scheduleType: 'tutoring',
              isTest: false
            });
          }
        }
      } catch (err) {
        console.warn("Failed to fetch pending alarm natively on startup:", err);
      }
    };

    window.addEventListener('alarmTriggered', handleNativeAlarm);
    checkPendingAlarmOnStartup();

    return () => {
      window.removeEventListener('alarmTriggered', handleNativeAlarm);
    };
  }, []);

  // Web fallback daemon - checks every 5 seconds for desktop/web simulation
  useEffect(() => {
    const interval = setInterval(() => {
      const now = new Date();
      const currentDayIdx = now.getDay(); // 0: Sunday, 1: Monday...
      const JS_DAY_TO_ARABIC = [
        "الأحد",
        "الاثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة",
        "السبت"
      ];
      const todayArabic = JS_DAY_TO_ARABIC[currentDayIdx];
      
      const hh = String(now.getHours()).padStart(2, '0');
      const mm = String(now.getMinutes()).padStart(2, '0');
      const currentTimeStr = `${hh}:${mm}`;

      // Check School Alarm (if enabled)
      if (state.schoolSchedule && state.schoolSchedule.alarmConfig && state.schoolSchedule.alarmConfig.enabled) {
        const schoolToday = state.schoolSchedule.grid[todayArabic] || {};
        state.schoolSchedule.headers.forEach(header => {
          const alarmTime = state.schoolSchedule.alarmConfig.times[header];
          const subject = schoolToday[header];
          
          if (alarmTime === currentTimeStr && subject) {
            const fireKey = `${todayArabic}-${currentTimeStr}-school-${subject}`;
            if (lastFiredAlarm !== fireKey) {
              setLastFiredAlarm(fireKey);
              setActiveAlarm({
                subjectName: subject,
                periodName: header,
                scheduleType: 'school'
              });
            }
          }
        });
      }

      // Check Tutoring Alarm (if enabled)
      if (state.tutoringSchedule.alarmConfig.enabled) {
        const tutoringToday = state.tutoringSchedule.grid[todayArabic] || {};
        state.tutoringSchedule.headers.forEach(header => {
          const alarmTime = state.tutoringSchedule.alarmConfig.times[header];
          const subject = tutoringToday[header];
          
          if (alarmTime === currentTimeStr && subject) {
            const fireKey = `${todayArabic}-${currentTimeStr}-tutoring-${subject}`;
            if (lastFiredAlarm !== fireKey) {
              setLastFiredAlarm(fireKey);
              setActiveAlarm({
                subjectName: subject,
                periodName: header,
                scheduleType: 'tutoring'
              });
            }
          }
        });
      }

      // Check Study Alarm (if enabled)
      if (state.studySchedule.alarmConfig.enabled) {
        const studyToday = state.studySchedule.grid[todayArabic] || {};
        state.studySchedule.headers.forEach(header => {
          const alarmTime = state.studySchedule.alarmConfig.times[header];
          const subject = studyToday[header];
          
          if (alarmTime === currentTimeStr && subject) {
            const fireKey = `${todayArabic}-${currentTimeStr}-study-${subject}`;
            if (lastFiredAlarm !== fireKey) {
              setLastFiredAlarm(fireKey);
              setActiveAlarm({
                subjectName: subject,
                periodName: header,
                scheduleType: 'study'
              });
            }
          }
        });
      }

    }, 5000);

    return () => clearInterval(interval);
  }, [state, lastFiredAlarm]);

  // Streak update check upon app load
  useEffect(() => {
    const todayStr = new Date().toISOString().split('T')[0];
    if (state.lastActiveDate !== todayStr) {
      // Calculate day difference
      const lastActive = new Date(state.lastActiveDate);
      const today = new Date(todayStr);
      const diffTime = Math.abs(today.getTime() - lastActive.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

      let newStreak = state.streakCount;
      if (diffDays === 1) {
        // Logged in consecutive day, increment streak!
        newStreak += 1;
      } else if (diffDays > 1) {
        // Missed a day, streak resets to 1 (or keeps at 1)
        newStreak = 1;
      }

      handleSaveState({
        ...state,
        streakCount: newStreak,
        lastActiveDate: todayStr
      });
    }
  }, [state.lastActiveDate]);

  // Lock back action
  const handleLock = () => {
    setIsUnlocked(false);
  };

  return (
    <div 
      className="min-h-screen bg-slate-50 flex flex-col justify-between font-sans selection:bg-emerald-200" 
      dir="rtl"
    >
      
      {/* 1. Passcode Gate Screen */}
      <AnimatePresence mode="wait">
        {!isUnlocked && (
          <motion.div
            initial={{ opacity: 1 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
            className="fixed inset-0 z-50"
          >
            <PasscodeLock 
              correctPasscode={state.passcode} 
              onSuccess={() => setIsUnlocked(true)} 
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* 2. Active Alarm Screen Modal */}
      <AnimatePresence>
        {activeAlarm && (
          <AlarmOverlay
            subjectName={activeAlarm.subjectName}
            periodName={activeAlarm.periodName}
            scheduleType={activeAlarm.scheduleType}
            isTest={activeAlarm.isTest}
            onDismiss={async () => {
              setActiveAlarm(null);
              try {
                await AlarmPlugin.stopAlarm();
              } catch (e) {
                console.warn("Native stopAlarm not available:", e);
              }
            }}
            onSnooze={async () => {
              const currentAlarm = { ...activeAlarm };
              setActiveAlarm(null);
              try {
                await AlarmPlugin.snoozeAlarm({
                  subject: currentAlarm.subjectName,
                  day: "اليوم",
                  time: currentAlarm.periodName
                });
              } catch (e) {
                console.warn("Native snoozeAlarm not available:", e);
              }
              // Keep fallback setTimeout for web browser sandbox testing
              setTimeout(() => {
                setActiveAlarm(currentAlarm);
              }, 600000); // 10 minutes
            }}
          />
        )}
      </AnimatePresence>

      {/* 3. Main Authenticated Application Area */}
      {isUnlocked && (
        <>
          {/* Header Bar */}
          <header className="bg-white border-b border-slate-100 shadow-xs sticky top-0 z-40 select-none">
            <div className="max-w-7xl mx-auto px-4 py-3 md:py-4 flex items-center justify-between">
              
              {/* Brand Logo & Name */}
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 bg-gradient-to-tr from-emerald-500 to-emerald-700 rounded-xl flex items-center justify-center shadow-md shadow-emerald-200">
                  <GraduationCap className="w-5.5 h-5.5 text-white" />
                </div>
                <div>
                  <h1 className="text-sm md:text-base font-extrabold text-slate-900 tracking-tight leading-none">
                    جدول الطالب الذكيِ
                  </h1>
                  <span className="text-[10px] text-slate-400 font-medium">النجاح يبدأ بالتنظيم</span>
                </div>
              </div>

              {/* Status and Private Lock Trigger */}
              <div className="flex items-center gap-3">
                {/* Active Streak Badge */}
                <div className="flex items-center gap-1 bg-amber-50/70 border border-amber-100 px-2.5 py-1 rounded-full text-[10px] md:text-xs font-black text-amber-700">
                  <Flame className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                  <span>{state.streakCount} 🔥</span>
                </div>

                {/* Lock icon button */}
                <button
                  onClick={handleLock}
                  className="p-2 bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-xl border border-slate-150 transition-colors cursor-pointer"
                  title="قفل التطبيق يدوياً"
                >
                  <Lock className="w-4 h-4" />
                </button>
              </div>

            </div>
          </header>

          {/* Core Scrollable Panel viewport */}
          <main className="max-w-7xl mx-auto w-full px-4 py-6 flex-1 mb-36 md:mb-32 flex flex-col justify-between">
            <div className="flex-1">
              <AnimatePresence mode="wait">
                <motion.div
                  key={activeTab}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ duration: 0.2 }}
                >
                  {activeTab === 'dashboard' && (
                    <Dashboard 
                      state={state} 
                      onNavigateToTab={(tab) => setActiveTab(tab)} 
                    />
                  )}
                  {activeTab === 'schedules' && (
                    <SchedulesTab 
                      state={state} 
                      onSaveState={handleSaveState} 
                    />
                  )}
                  {activeTab === 'tasks' && (
                    <TasksTab 
                      state={state} 
                      onSaveState={handleSaveState} 
                    />
                  )}
                  {activeTab === 'settings' && (
                    <SettingsTab 
                      state={state} 
                      onSaveState={handleSaveState} 
                      onLockApp={handleLock}
                    />
                  )}
                </motion.div>
              </AnimatePresence>
            </div>
          </main>

          {/* Centralized Highly Prominent Copyright Footer fixed above the navigation bar */}
          <div className="fixed bottom-[68px] left-0 right-0 flex justify-center items-center z-20 pointer-events-none select-none">
            <div className="pointer-events-auto select-all">
              <p className="text-[10px] md:text-xs font-bold text-slate-700 bg-white/95 backdrop-blur-xs border border-slate-200/80 shadow-md px-4 py-1 rounded-full">
                بواسطة الشيخ أحمد النمس غفر الله له
              </p>
            </div>
          </div>

          {/* Bottom/Footer Floating Navigation Menu */}
          <nav className="fixed bottom-3 left-4 right-4 z-30 max-w-xl mx-auto bg-white/95 backdrop-blur-md border border-slate-200/80 rounded-2xl shadow-xl select-none">
            <div className="px-4 py-1 md:py-1.5 flex justify-around">
              
              {/* Home Tab */}
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`flex flex-col items-center gap-0.5 p-1 rounded-xl transition-all cursor-pointer ${
                  activeTab === 'dashboard' ? 'text-emerald-600 scale-105 font-bold' : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                <Home className="w-4.5 h-4.5" />
                <span className="text-[9px]">الرئيسية</span>
              </button>

              {/* Schedules Tab */}
              <button
                onClick={() => setActiveTab('schedules')}
                className={`flex flex-col items-center gap-0.5 p-1 rounded-xl transition-all cursor-pointer ${
                  activeTab === 'schedules' ? 'text-emerald-600 scale-105 font-bold' : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                <Calendar className="w-4.5 h-4.5" />
                <span className="text-[9px]">الجداول الثلاثة</span>
              </button>

              {/* Tasks Tab */}
              <button
                onClick={() => setActiveTab('tasks')}
                className={`flex flex-col items-center gap-0.5 p-1 rounded-xl transition-all cursor-pointer ${
                  activeTab === 'tasks' ? 'text-emerald-600 scale-105 font-bold' : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                <CheckSquare className="w-4.5 h-4.5" />
                <span className="text-[9px]">المهام والواجبات</span>
              </button>

              {/* Settings Tab */}
              <button
                onClick={() => setActiveTab('settings')}
                className={`flex flex-col items-center gap-0.5 p-1 rounded-xl transition-all cursor-pointer ${
                  activeTab === 'settings' ? 'text-emerald-600 scale-105 font-bold' : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                <Settings className="w-4.5 h-4.5" />
                <span className="text-[9px]">الإعدادات</span>
              </button>

            </div>
          </nav>
        </>
      )}

    </div>
  );
}
