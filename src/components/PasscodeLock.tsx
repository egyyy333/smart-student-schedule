import { useState, useEffect } from 'react';
import { GraduationCap, Delete, RotateCcw } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface PasscodeLockProps {
  correctPasscode: string;
  onSuccess: () => void;
}

export default function PasscodeLock({ correctPasscode, onSuccess }: PasscodeLockProps) {
  const [code, setCode] = useState<string>('');
  const [isError, setIsError] = useState<boolean>(false);

  useEffect(() => {
    if (code.length === 4) {
      if (code === correctPasscode) {
        onSuccess();
      } else {
        setIsError(true);
        // Play quick feedback beep or vibrate
        if ("vibrate" in navigator) {
          navigator.vibrate([100, 50, 100]);
        }
        // Reset code after delay
        const timer = setTimeout(() => {
          setCode('');
          setIsError(false);
        }, 800);
        return () => clearTimeout(timer);
      }
    }
  }, [code, correctPasscode, onSuccess]);

  const handleKeyPress = (num: string) => {
    if (code.length < 4 && !isError) {
      setCode(prev => prev + num);
    }
  };

  const handleBackspace = () => {
    if (code.length > 0 && !isError) {
      setCode(prev => prev.slice(0, -1));
    }
  };

  const handleClearAll = () => {
    if (!isError) {
      setCode('');
    }
  };

  // Keyboard support
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (isError) return;
      if (e.key >= '0' && e.key <= '9') {
        handleKeyPress(e.key);
      } else if (e.key === 'Backspace') {
        handleBackspace();
      } else if (e.key === 'Escape' || e.key === 'Delete') {
        handleClearAll();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [code, isError]);

  return (
    <div id="passcode-container" className="fixed inset-0 bg-slate-50 flex flex-col landscape:flex-row justify-between landscape:justify-center items-center z-50 p-6 landscape:p-8 select-none font-sans overflow-y-auto">
      
      {/* Left Column in Landscape, Top Column in Portrait */}
      <div className="flex flex-col items-center text-center landscape:max-w-xs landscape:my-auto">
        {/* Upper Brand Info */}
        <div className="flex flex-col items-center mt-6 landscape:mt-0 text-center">
          <motion.div 
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.5 }}
            className="w-14 h-14 bg-gradient-to-tr from-emerald-500 to-emerald-700 rounded-2xl flex items-center justify-center shadow-lg shadow-emerald-200 mb-3"
          >
            <GraduationCap className="w-8 h-8 text-white" />
          </motion.div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight mb-1">
            جدول الطالب الذكيِ
          </h1>
          <p className="text-xs text-slate-500 max-w-xs font-medium">
            تنظيم الحصص والدروس والمذاكرة اليومية والمهام
          </p>
        </div>

        {/* Code Dots & Status */}
        <div className="flex flex-col items-center my-4 landscape:my-6">
          <motion.div 
            animate={isError ? { x: [-10, 10, -10, 10, 0] } : {}}
            transition={{ type: "spring", stiffness: 500, damping: 15 }}
            className="flex gap-4 justify-center items-center h-10"
          >
            {[0, 1, 2, 3].map((index) => {
              const isFilled = code.length > index;
              return (
                <motion.div
                  key={index}
                  animate={{
                    scale: isFilled ? 1.25 : 1,
                    backgroundColor: isError 
                      ? "#f43f5e" 
                      : isFilled 
                        ? "#059669" 
                        : "#cbd5e1"
                  }}
                  className="w-3.5 h-3.5 rounded-full shadow-sm"
                />
              );
            })}
          </motion.div>

          {/* Status message */}
          <div className="h-5 mt-1">
            <AnimatePresence mode="wait">
              {isError ? (
                <motion.p 
                  initial={{ opacity: 0, y: -5 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 5 }}
                  className="text-[10px] font-bold text-rose-500"
                >
                  ⚠️ رمز الدخول خاطئ، يرجى المحاولة مرة أخرى.
                </motion.p>
              ) : code.length === 0 ? (
                <p className="text-[10px] text-slate-400 font-medium">
                  الرمز الافتراضي: <span className="font-mono bg-slate-200 px-1.5 py-0.5 rounded text-slate-600">1234</span>
                </p>
              ) : null}
            </AnimatePresence>
          </div>
        </div>

        {/* Spiritual Signature under brand in landscape */}
        <div className="hidden landscape:block mt-2 text-center">
          <p className="text-[10px] text-slate-400 font-serif leading-relaxed">
            بواسطة الشيخ أحمد النمس غفر الله له
          </p>
        </div>
      </div>

      {/* Right Column in Landscape, Bottom Column in Portrait */}
      <div className="w-full max-w-xs flex flex-col items-center landscape:my-auto">
        {/* Numeric Keypad */}
        <div className="w-full mb-4 landscape:mb-0">
          <div className="grid grid-cols-3 gap-3 justify-items-center">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((num) => (
              <button
                key={num}
                onClick={() => handleKeyPress(num)}
                className="w-14 h-14 rounded-full bg-white border border-slate-100 shadow-sm active:bg-slate-100 hover:border-slate-200 flex items-center justify-center text-lg font-bold text-slate-800 transition-colors cursor-pointer"
              >
                {num}
              </button>
            ))}
            
            {/* Backspace All */}
            <button
              onClick={handleClearAll}
              className="w-14 h-14 rounded-full bg-rose-50 hover:bg-rose-100 active:bg-rose-200 flex items-center justify-center text-rose-600 text-xs font-bold transition-colors cursor-pointer"
              title="مسح الكل"
            >
              <RotateCcw className="w-5 h-5" />
            </button>

            {/* Zero */}
            <button
              onClick={() => handleKeyPress('0')}
              className="w-14 h-14 rounded-full bg-white border border-slate-100 shadow-sm active:bg-slate-100 hover:border-slate-200 flex items-center justify-center text-lg font-bold text-slate-800 transition-colors cursor-pointer"
            >
              0
            </button>

            {/* Backspace single */}
            <button
              onClick={handleBackspace}
              className="w-14 h-14 rounded-full bg-slate-100 hover:bg-slate-200 active:bg-slate-300 flex items-center justify-center text-slate-600 transition-colors cursor-pointer"
              title="حذف"
            >
              <Delete className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Immutable Footnote for Portrait Mode */}
        <div className="block landscape:hidden mt-4 text-center">
          <p className="text-[10px] text-slate-400 font-serif leading-relaxed">
            بواسطة الشيخ أحمد النمس غفر الله له
          </p>
        </div>
      </div>

    </div>
  );
}
