import { useEffect } from 'react';
import { Clock, Volume2, BellRing } from 'lucide-react';
import { motion } from 'motion/react';
import { startAlarmSound, stopAlarmSound } from '../audioHelper';

interface AlarmOverlayProps {
  subjectName: string;
  periodName: string;
  scheduleType: string; // 'tutoring' | 'study'
  onDismiss: () => void;
}

export default function AlarmOverlay({ subjectName, periodName, scheduleType, onDismiss }: AlarmOverlayProps) {
  useEffect(() => {
    // Start alarm audio on mount
    startAlarmSound();
    
    // Auto vibrator if supported
    if ("vibrate" in navigator) {
      navigator.vibrate([500, 300, 500, 300, 500]);
    }

    return () => {
      // Clean up audio on unmount
      stopAlarmSound();
    };
  }, []);

  const typeLabel = scheduleType === 'tutoring' ? 'جدول الدروس الخصوصية' : 'جدول المذاكرة والمراجعة';

  return (
    <div id="alarm-overlay-container" className="fixed inset-0 bg-slate-950/95 flex flex-col landscape:flex-row justify-between landscape:justify-center items-center z-50 p-6 landscape:p-8 text-white font-sans text-center overflow-y-auto gap-6">
      
      {/* Dynamic particles in background */}
      <div className="absolute inset-0 opacity-10 pointer-events-none overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-emerald-500 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-sky-500 rounded-full blur-3xl animate-pulse delay-1000" />
      </div>

      <div className="my-auto flex flex-col items-center relative z-10 landscape:max-w-md">
        {/* Pulsating Ringing Icon */}
        <motion.div
          animate={{
            scale: [1, 1.15, 1],
            rotate: [-10, 10, -10, 10, 0]
          }}
          transition={{
            repeat: Infinity,
            duration: 1.5,
            ease: "easeInOut"
          }}
          className="w-20 h-20 bg-gradient-to-tr from-emerald-500 to-emerald-700 rounded-full flex items-center justify-center shadow-2xl shadow-emerald-500/50 mb-6 landscape:mb-4"
        >
          <BellRing className="w-10 h-10 text-white animate-bounce" />
        </motion.div>

        {/* Alarm Headers */}
        <h2 className="text-emerald-400 font-bold tracking-wider text-xs mb-1.5 uppercase">
          🚨 حان الآن موعد المنبه الذكي
        </h2>
        <span className="text-[10px] text-slate-400 bg-slate-900 border border-slate-800 px-3 py-0.5 rounded-full mb-4 inline-block">
          {typeLabel}
        </span>

        {/* Subject Name / Period Title */}
        <motion.h1 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-3xl font-black text-white tracking-tight mb-3 px-4 drop-shadow"
        >
          {subjectName || "وقت المذاكرة والتحصيل الدراسي"}
        </motion.h1>

        <p className="text-base text-slate-300 font-medium">
          بناءً على تخطيطك الذكي لـ: <span className="text-emerald-300 font-semibold">{periodName}</span>
        </p>

        {/* Decorative Quote */}
        <p className="text-[10px] text-slate-500 mt-4 max-w-xs italic leading-relaxed">
          "يا بطل، الوقت هو كنزك الأثمن. ركّز الآن وابدأ بكل همّة ونشاط وجدّ!"
        </p>
      </div>

      {/* Dismiss Button */}
      <div className="mb-4 landscape:mb-0 w-full max-w-xs flex flex-col items-center justify-center relative z-10 landscape:my-auto">
        <button
          onClick={onDismiss}
          className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-bold rounded-2xl shadow-xl shadow-emerald-900/40 transition-all flex items-center justify-center gap-3 text-base cursor-pointer"
        >
          <Volume2 className="w-5 h-5" />
          <span>إيقاف المنبه والبدء</span>
        </button>

        {/* Immutable footnote signature */}
        <div className="mt-4 text-center">
          <p className="text-[10px] text-slate-500 font-serif">
            بواسطة الشيخ أحمد النمس غفر الله له
          </p>
        </div>
      </div>

    </div>
  );
}
