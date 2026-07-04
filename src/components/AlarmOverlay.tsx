import { useEffect } from 'react';
import { Clock, Volume2, BellRing } from 'lucide-react';
import { motion } from 'motion/react';
import { startAlarmSound, stopAlarmSound } from '../audioHelper';

interface AlarmOverlayProps {
  subjectName: string;
  periodName: string;
  scheduleType: string; // 'tutoring' | 'study'
  onDismiss: () => void;
  onSnooze: () => void;
}

export default function AlarmOverlay({ subjectName, periodName, scheduleType, onDismiss, onSnooze }: AlarmOverlayProps) {
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

  const typeLabel = scheduleType === 'tutoring' ? 'جدول الدروس الخصوصية 📚' : 'جدول المذاكرة والمراجعة ✏️';
  const typeColorClass = scheduleType === 'tutoring' 
    ? 'text-emerald-400 bg-emerald-950/80 border-emerald-500/30' 
    : 'text-amber-400 bg-amber-950/80 border-amber-500/30';

  return (
    <div id="alarm-overlay-container" className="fixed inset-0 bg-slate-950 flex flex-col justify-center items-center z-50 p-6 text-white font-sans text-center overflow-y-auto">
      
      {/* Dynamic particles in background */}
      <div className="absolute inset-0 opacity-10 pointer-events-none overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-emerald-500 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-sky-500 rounded-full blur-3xl animate-pulse delay-1000" />
      </div>

      <div className="my-auto flex flex-col items-center relative z-10 w-full max-w-md">
        {/* Pulsating Ringing Icon */}
        <motion.div
          animate={{
            scale: [1, 1.12, 1],
            rotate: [-10, 10, -10, 10, 0]
          }}
          transition={{
            repeat: Infinity,
            duration: 1.5,
            ease: "easeInOut"
          }}
          className="w-20 h-20 bg-gradient-to-tr from-emerald-500 to-emerald-700 rounded-full flex items-center justify-center shadow-2xl shadow-emerald-500/50 mb-4"
        >
          <BellRing className="w-10 h-10 text-white animate-bounce" />
        </motion.div>

        {/* Alarm Title */}
        <h2 className="text-emerald-400 font-black tracking-wider text-xs mb-3 uppercase flex items-center gap-1.5 justify-center">
          <span>🚨 حان الآن موعد المنبه الذكي</span>
        </h2>

        {/* Highlighted Category Label (جدول الدروس الخصوصية / جدول المذاكرة والمراجعة) */}
        <div className={`px-6 py-2.5 rounded-2xl border text-base font-black shadow-lg mb-4 leading-none ${typeColorClass}`}>
          {typeLabel}
        </div>

        {/* Elegant box containing subject name and hour only */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 w-full shadow-2xl flex flex-col items-center justify-center gap-4">
          <span className="text-[10px] uppercase tracking-widest text-slate-400 font-extrabold">الدرس / المادة</span>
          <div className="text-2xl md:text-3xl font-black text-white leading-tight">
            {subjectName || "وقت المذاكرة والتحصيل الدراسي"}
          </div>
          
          <div className="w-2/3 h-px bg-slate-800" />
          
          <span className="text-[10px] uppercase tracking-widest text-slate-400 font-extrabold">الساعة والوقت</span>
          <div className="flex items-center gap-2 text-emerald-400 font-black text-lg bg-slate-950 px-4 py-1.5 rounded-xl border border-slate-900 shadow-inner">
            <Clock className="w-4 h-4" />
            <span>{periodName}</span>
          </div>
        </div>

        {/* Control Action Buttons */}
        <div className="w-full max-w-xs flex flex-col gap-3 mt-6">
          {/* Stop Button */}
          <button
            onClick={onDismiss}
            className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-extrabold rounded-2xl shadow-lg shadow-emerald-950/40 transition-all text-base cursor-pointer"
          >
            إيقاف
          </button>
          
          {/* Snooze Button */}
          <button
            onClick={onSnooze}
            className="w-full py-3.5 bg-amber-600 hover:bg-amber-500 active:bg-amber-700 text-white font-extrabold rounded-2xl shadow-lg shadow-amber-950/40 transition-all text-base cursor-pointer"
          >
            تأجيل (10 دقائق)
          </button>
        </div>

        {/* Highly Visible centered Copyright Badge */}
        <div className="mt-8 text-center select-all">
          <p className="text-xs md:text-sm font-bold text-slate-200 bg-slate-900 border border-slate-800/80 px-6 py-2.5 rounded-full shadow-md">
            بواسطة الشيخ أحمد النمس غفر الله له
          </p>
        </div>

      </div>

    </div>
  );
}
