import { Flame, Clock, BookOpen, MapPin, User, CheckCircle } from 'lucide-react';
import { AppState } from '../types';
import { DAYS } from '../defaultData';
import { motion } from 'motion/react';

interface DashboardProps {
  state: AppState;
  onNavigateToTab: (tab: string) => void;
}

export default function Dashboard({ state, onNavigateToTab }: DashboardProps) {
  // Determine current day in Arabic
  const todayIndex = new Date().getDay(); // 0 is Sunday, 1 is Monday, etc.
  const JS_DAY_TO_ARABIC = [
    "الأحد",
    "الاثنين",
    "الثلاثاء",
    "الأربعاء",
    "الخميس",
    "الجمعة",
    "السبت"
  ];
  const currentArabicDay = JS_DAY_TO_ARABIC[todayIndex];

  // Helper to get current time in 12-hour Arabic format
  const getFormattedTime12 = () => {
    const now = new Date();
    let hours = now.getHours();
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? 'م' : 'ص';
    hours = hours % 12;
    hours = hours ? hours : 12; // conversion of 0 to 12
    return `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`;
  };

  // Fetch today's items from all 3 schedules
  const schoolToday = state.schoolSchedule.grid[currentArabicDay] || {};
  const tutoringToday = state.tutoringSchedule.grid[currentArabicDay] || {};
  const studyToday = state.studySchedule.grid[currentArabicDay] || {};

  // Form timelines
  const schoolTimeline = state.schoolSchedule.headers
    .map(header => ({ header, subject: schoolToday[header] || "", type: "school" }))
    .filter(item => item.subject !== "");

  const tutoringTimeline = state.tutoringSchedule.headers
    .map(header => ({ header, subject: tutoringToday[header] || "", type: "tutoring" }))
    .filter(item => item.subject !== "");

  const studyTimeline = state.studySchedule.headers
    .map(header => ({ header, subject: studyToday[header] || "", type: "study" }))
    .filter(item => item.subject !== "");

  const totalTodayCount = schoolTimeline.length + tutoringTimeline.length + studyTimeline.length;

  // Render greeting text
  const displayName = state.studentName && state.enableNameCall 
    ? <span className="text-amber-400 font-extrabold underline decoration-amber-500/50 decoration-2 underline-offset-4">{state.studentName}</span> 
    : <span className="text-amber-400 font-bold">يا بطل</span>;

  return (
    <div className="space-y-6 font-sans">
      
      {/* 1. Large Greeting Banner */}
      <motion.div 
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden bg-slate-900 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-slate-900/10 border border-slate-800"
      >
        {/* Background ambient accent circles */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl -mr-16 -mt-16" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-sky-500/10 rounded-full blur-3xl -ml-16 -mb-16" />

        <div className="relative z-10 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold bg-emerald-500/25 border border-emerald-500/30 text-emerald-300 px-3 py-1 rounded-full">
              لوحة التحكم اليومية 🗓️
            </span>
            <div className="flex items-center gap-1.5 bg-amber-500/20 text-amber-300 px-3 py-1 rounded-full text-xs font-bold border border-amber-500/30 animate-pulse">
              <Flame className="w-4 h-4 fill-amber-500 text-amber-500" />
              <span>{state.streakCount} أيام متتالية</span>
            </div>
          </div>

          <h1 className="text-2xl md:text-3.5xl font-black text-white leading-tight">
            مرحباً بك {displayName} في لوحة التحكم
          </h1>

          <div className="text-slate-300 text-sm md:text-base space-y-1 font-medium max-w-2xl leading-relaxed">
            <p>يومك مرتب ومنظم بذكاء! 🔥</p>
            <p>
              اليوم هو <span className="text-white font-bold underline decoration-emerald-400 decoration-2 underline-offset-2">{currentArabicDay}</span>، الساعة <span className="text-amber-300 font-extrabold">{getFormattedTime12()}</span>. تصفح مواعيد حصصك ودروسك بذكاء ونفذ خطتك الدراسية.
            </p>
          </div>
        </div>
      </motion.div>

      {/* 2. Today's Overview Grid & Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* Today's Stats Cards */}
        <div className="md:col-span-1 space-y-4">
          
          <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm">
            <h3 className="text-xs font-bold text-slate-400 mb-4">ملخص الأنشطة اليومية</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-cyan-500" />
                  <span className="text-xs font-bold text-slate-600">حصص مدرسية اليوم</span>
                </div>
                <span className="text-xs font-mono font-bold text-slate-800 bg-cyan-100/50 px-2.5 py-0.5 rounded-full">
                  {schoolTimeline.length} حصص
                </span>
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                  <span className="text-xs font-bold text-slate-600">دروس خصوصية اليوم</span>
                </div>
                <span className="text-xs font-mono font-bold text-slate-800 bg-emerald-100/50 px-2.5 py-0.5 rounded-full">
                  {tutoringTimeline.length} دروس
                </span>
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-indigo-500" />
                  <span className="text-xs font-bold text-slate-600">جلسات مذاكرة اليوم</span>
                </div>
                <span className="text-xs font-mono font-bold text-slate-800 bg-indigo-100/50 px-2.5 py-0.5 rounded-full">
                  {studyTimeline.length} جلسات
                </span>
              </div>
            </div>
          </div>

          {/* Productivity Prompt */}
          <div className="bg-emerald-50/50 border border-emerald-100 rounded-2xl p-5">
            <h4 className="text-sm font-bold text-emerald-800 mb-2 flex items-center gap-1.5">
              <span>نصيحة اليوم الذكية 💡</span>
            </h4>
            <p className="text-xs text-emerald-700/95 leading-relaxed font-medium">
              تصفح جدولك بدقة، والتزم بالتوقيتات المحددة. لا تؤجل عمل اليوم إلى الغد، وحافظ على شعلة تفوقك متقدة!
            </p>
            <button 
              onClick={() => onNavigateToTab('tasks')}
              className="mt-4 text-xs font-bold text-emerald-600 hover:text-emerald-800 flex items-center gap-1 cursor-pointer transition-all"
            >
              <span>استعرض المهام والواجبات المطلوبة ←</span>
            </button>
          </div>

        </div>

        {/* 3. Dynamic Timeline Agenda */}
        <div className="md:col-span-2 bg-white border border-slate-100 rounded-2xl p-5 shadow-sm flex flex-col">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-slate-50">
            <h2 className="text-base font-extrabold text-slate-800 flex items-center gap-2">
              <Clock className="w-5 h-5 text-emerald-600" />
              <span>أجندة وسير العمل اليومي ({currentArabicDay})</span>
            </h2>
            <span className="text-xs text-slate-400 font-mono font-bold">
              {totalTodayCount} مهام مجدولة
            </span>
          </div>

          {totalTodayCount === 0 ? (
            <div className="my-auto py-12 flex flex-col items-center text-center justify-center space-y-3">
              <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center border border-slate-100">
                <CheckCircle className="w-8 h-8 text-slate-300" />
              </div>
              <h3 className="text-sm font-extrabold text-slate-700">يوم مريح وخفيف!</h3>
              <p className="text-xs text-slate-400 max-w-sm leading-relaxed font-medium">
                لا توجد حصص أو مذاكرة مجدولة اليوم، استغل وقتك في تنمية مهاراتك أو الترفيه المفيد!
              </p>
              <button 
                onClick={() => onNavigateToTab('schedules')}
                className="mt-2 text-xs font-bold bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 px-4 py-2 rounded-full transition-colors cursor-pointer"
              >
                تعبئة جدول الحصص والدروس الآن
              </button>
            </div>
          ) : (
            <div className="relative border-r-2 border-slate-100 mr-2 pr-6 py-2 space-y-6">
              
              {/* School Timeline block */}
              {schoolTimeline.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-bold text-cyan-600 bg-cyan-50 border border-cyan-100 px-2 py-1 rounded inline-block">
                    الحصص المدرسية
                  </h4>
                  {schoolTimeline.map((item, idx) => (
                    <div key={`school-${idx}`} className="relative group">
                      <div className="absolute -right-[31px] top-1.5 w-2 h-2 rounded-full bg-cyan-400 border-2 border-white ring-4 ring-cyan-100" />
                      <div className="p-3 bg-cyan-50/20 hover:bg-cyan-50/50 border border-cyan-100/50 rounded-xl transition-all">
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-extrabold text-cyan-900 font-mono">
                            {item.header}
                          </span>
                          <span className="text-xs font-black text-cyan-700 bg-cyan-100/60 px-2 py-0.5 rounded-md">
                            {item.subject}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Tutoring Timeline block */}
              {tutoringTimeline.length > 0 && (
                <div className="space-y-3 pt-2">
                  <h4 className="text-xs font-bold text-emerald-600 bg-emerald-50 border border-emerald-100 px-2 py-1 rounded inline-block">
                    الدروس الخصوصية ومراكز السناتر
                  </h4>
                  {tutoringTimeline.map((item, idx) => (
                    <div key={`tutoring-${idx}`} className="relative group">
                      <div className="absolute -right-[31px] top-1.5 w-2 h-2 rounded-full bg-emerald-400 border-2 border-white ring-4 ring-emerald-100" />
                      <div className="p-3 bg-emerald-50/20 hover:bg-emerald-50/50 border border-emerald-100/50 rounded-xl transition-all">
                        <div className="flex justify-between items-center">
                          <div className="flex items-center gap-1.5">
                            <span className="text-xs font-bold text-slate-600 font-mono">
                              {item.header}
                            </span>
                          </div>
                          <span className="text-xs font-black text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-md">
                            {item.subject}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Study Timeline block */}
              {studyTimeline.length > 0 && (
                <div className="space-y-3 pt-2">
                  <h4 className="text-xs font-bold text-indigo-600 bg-indigo-50 border border-indigo-100 px-2 py-1 rounded inline-block">
                    المذاكرة والتحضير المنزلي
                  </h4>
                  {studyTimeline.map((item, idx) => (
                    <div key={`study-${idx}`} className="relative group">
                      <div className="absolute -right-[31px] top-1.5 w-2 h-2 rounded-full bg-indigo-400 border-2 border-white ring-4 ring-indigo-100" />
                      <div className="p-3 bg-indigo-50/20 hover:bg-indigo-50/50 border border-indigo-100/50 rounded-xl transition-all">
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-bold text-indigo-900 font-mono">
                            {item.header}
                          </span>
                          <span className="text-xs font-black text-indigo-700 bg-indigo-100/60 px-2 py-0.5 rounded-md">
                            {item.subject}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

            </div>
          )}
        </div>

      </div>

    </div>
  );
}
