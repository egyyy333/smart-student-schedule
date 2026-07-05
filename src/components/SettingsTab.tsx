import { useState } from 'react';
import { Capacitor } from '@capacitor/core';
import { 
  User, Lock, Key, Database, Download, Upload, Trash2, RefreshCw, Check, AlertTriangle, ShieldCheck 
} from 'lucide-react';
import { AppState } from '../types';
import { INITIAL_STATE } from '../defaultData';
import { speakArabicText } from '../audioHelper';
import { motion, AnimatePresence } from 'motion/react';

interface SettingsTabProps {
  state: AppState;
  onSaveState: (newState: AppState) => void;
  onLockApp: () => void;
}

export default function SettingsTab({ state, onSaveState, onLockApp }: SettingsTabProps) {
  // 1. Name Profile States
  const [localName, setLocalName] = useState<string>(state.studentName);

  // 2. Passcode Update States
  const [currentPass, setCurrentPass] = useState<string>('');
  const [newPass, setNewPass] = useState<string>('');
  const [confirmPass, setConfirmPass] = useState<string>('');
  const [passError, setPassError] = useState<string | null>(null);
  const [passSuccess, setPassSuccess] = useState<boolean>(false);

  // 3. Security verification overlays
  const [securityAction, setSecurityAction] = useState<'backup' | 'restore' | 'wipe' | null>(null);
  const [securityPassAttempt, setSecurityPassAttempt] = useState<string>('');
  const [securityPassError, setSecurityPassError] = useState<string | null>(null);
  const [showWipeFinalConfirm, setShowWipeFinalConfirm] = useState<boolean>(false);

  // Name actions
  const handlePinName = () => {
    onSaveState({
      ...state,
      studentName: localName.trim()
    });
    speakArabicText("تم تثبيت اسم الطالب بنجاح");
  };

  const handleTestNameSpeech = () => {
    const textToSpeak = localName.trim() 
      ? `مرحباً بك يا ${localName.trim()}` 
      : "مرحباً بك يا بطل";
    speakArabicText(textToSpeak);
  };

  const handleToggleNameCall = () => {
    onSaveState({
      ...state,
      enableNameCall: !state.enableNameCall
    });
  };

  // Update passcode action
  const handleUpdatePasscode = () => {
    setPassError(null);
    setPassSuccess(false);

    if (currentPass !== state.passcode) {
      setPassError('⚠️ رمز الدخول الحالي غير صحيح.');
      return;
    }

    if (newPass.length !== 4 || !/^\D*\d+\D*/.test(newPass)) {
      // Must be 4 digits
      if (newPass.length !== 4 || isNaN(Number(newPass))) {
        setPassError('⚠️ يجب أن يتكون الرمز الجديد من 4 أرقام عددية فقط.');
        return;
      }
    }

    if (newPass !== confirmPass) {
      setPassError('⚠️ الرمز الجديد وتأكيده غير متطابقين.');
      return;
    }

    onSaveState({
      ...state,
      passcode: newPass
    });

    setCurrentPass('');
    setNewPass('');
    setConfirmPass('');
    setPassSuccess(true);
    speakArabicText("تم تحديث رمز الدخول");
  };

  // Passcode gating handler
  const handleTriggerSecurityCheck = (action: 'backup' | 'restore' | 'wipe') => {
    setSecurityAction(action);
    setSecurityPassAttempt('');
    setSecurityPassError(null);
  };

  const handleConfirmSecurityCheck = () => {
    if (securityPassAttempt === state.passcode) {
      const actionToExecute = securityAction;
      setSecurityAction(null); // Close gating modal

      if (actionToExecute === 'backup') {
        executeBackupDownload();
      } else if (actionToExecute === 'restore') {
        triggerRestoreUpload();
      } else if (actionToExecute === 'wipe') {
        setShowWipeFinalConfirm(true);
      }
    } else {
      setSecurityPassError('⚠️ رمز الدخول غير صحيح.');
    }
  };

  // Backup exporter
  const executeBackupDownload = async () => {
    try {
      // Exclude passcode from the backup as requested (except default value "1234" can be set or stripped entirely)
      const { passcode, ...backupData } = state;
      const dataStr = JSON.stringify(backupData, null, 2);
      const timestamp = new Date().toISOString().split('T')[0];
      const filename = `جدول_الطالب_الذكي_نسخة_احتياطية_${timestamp}.json`;

      let sharedNatively = false;

      // If running on native Android/iOS, trigger our native Storage Access Framework SAF File Picker
      if (Capacitor.isNativePlatform()) {
        try {
          const { AlarmPlugin } = await import('../utils/alarmSync');
          await AlarmPlugin.exportBackup({ data: dataStr, filename: filename });
          sharedNatively = true;
          speakArabicText("تم تصدير وحفظ النسخة الاحتياطية بنجاح");
        } catch (nativeErr) {
          console.warn("Native exportBackup failed, trying fallback Web Share API:", nativeErr);
        }
      }

      // If not completed natively, try standard Web Share API
      if (!sharedNatively && navigator.share) {
        try {
          const file = new File([dataStr], filename, { type: 'application/json' });
          if (navigator.canShare && navigator.canShare({ files: [file] })) {
            await navigator.share({
              files: [file],
              title: 'نسخة احتياطية لجدول الطالب الذكي',
              text: 'ملف البيانات الاحتياطية لتطبيق جدول الطالب الذكي',
            });
            sharedNatively = true;
            speakArabicText("تم تصدير وحفظ النسخة الاحتياطية بنجاح");
          } else {
            // Share as text fallback
            await navigator.share({
              title: 'نسخة احتياطية لجدول الطالب الذكي',
              text: dataStr
            });
            sharedNatively = true;
            speakArabicText("تم تصدير البيانات بنجاح");
          }
        } catch (shareErr) {
          console.warn("Native share failed, falling back to download anchor", shareErr);
        }
      }

      if (!sharedNatively) {
        // Safe standard web blob download (much better than data: URI inside webviews)
        const blob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const downloadAnchor = document.createElement('a');
        downloadAnchor.setAttribute("href", url);
        downloadAnchor.setAttribute("download", filename);
        document.body.appendChild(downloadAnchor);
        downloadAnchor.click();
        downloadAnchor.remove();
        URL.revokeObjectURL(url);
        speakArabicText("تم تحميل النسخة الاحتياطية بنجاح");
      }
    } catch (error) {
      console.error("Backup failed", error);
      speakArabicText("فشل تصدير النسخة الاحتياطية");
    }
  };

  // Restore importer
  const triggerRestoreUpload = () => {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.json';
    fileInput.onchange = (e: any) => {
      const file = e.target.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (event: any) => {
        try {
          const importedData = JSON.parse(event.target.result);
          
          // Basic schema sanity validation (excluding passcode since we strip it on export)
          if (importedData.schoolSchedule || importedData.tasks || importedData.tutoringSchedule) {
            // Merge into state while keeping the current customized passcode intact
            const mergedData = {
              ...state,
              ...importedData,
              passcode: state.passcode // Retain current password as requested
            };
            onSaveState(mergedData);
            speakArabicText("تم استرجاع كافة بياناتك بنجاح وبسرعة");
            alert("✅ تم استرجاع البيانات بنجاح وجاري إعادة تحميل التطبيق!");
          } else {
            alert("❌ الملف المختار لا يتوافق مع بنية بيانات جدول الطالب الذكي.");
          }
        } catch (err) {
          alert("❌ فشل قراءة أو تحليل ملف البيانات المستورد.");
        }
      };
      reader.readAsText(file);
    };
    fileInput.click();
  };

  // Factory reset executor
  const executeFactoryWipe = () => {
    onSaveState(INITIAL_STATE);
    setShowWipeFinalConfirm(false);
    speakArabicText("تم إعادة تهيئة التطبيق بالكامل");
    onLockApp(); // Go back to passcode screen immediately
  };

  return (
    <div className="space-y-6 font-sans">
      
      {/* 1. Header Information */}
      <div className="text-right space-y-1.5">
        <h1 className="text-xl md:text-2xl font-extrabold text-slate-800">
          إعدادات وتهيئة التطبيق ⚙️
        </h1>
        <p className="text-xs text-slate-400 font-medium">
          تخصيص الإعدادات وتعديل كلمة المرور وإدارة النسخ الاحتياطي والذاكرة
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

        {/* SECTION A: PROFILE CARD */}
        <div className="bg-white border border-slate-100 rounded-3xl p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-50 pb-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <User className="w-4.5 h-4.5" />
            </div>
            <h3 className="text-sm font-extrabold text-slate-800">تعريف هوية اسم الطالب</h3>
          </div>

          <div className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-500 block">اسم الطالب (ثنائي أو ثلاثي):</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={localName}
                  onChange={(e) => setLocalName(e.target.value)}
                  className="flex-1 p-2.5 rounded-xl border border-slate-200 text-slate-800 font-bold text-sm bg-slate-50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all"
                  placeholder="مثال: أحمد عبد الله"
                />
                
                {/* Save/Pin button */}
                <button
                  onClick={handlePinName}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer flex items-center gap-1"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>تثبيت</span>
                </button>
              </div>
            </div>

            {/* Checkbox toggle */}
            <label className="flex items-start gap-2.5 p-3 bg-slate-50 rounded-xl border border-slate-100 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={state.enableNameCall}
                onChange={handleToggleNameCall}
                className="mt-1 w-4 h-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
              />
              <div className="space-y-0.5">
                <span className="text-xs font-bold text-slate-700 block">تفعيل نداء الاسم بدلاً من "يا بطل"</span>
                <span className="text-[10px] text-slate-400 font-medium block">
                  عند تفعيله، سيقوم التطبيق بذكر اسمك الحقيقي في لافتة الترحيب والإشعارات الصوتية لتشجيعك.
                </span>
              </div>
            </label>
          </div>
        </div>

        {/* SECTION B: SECURITY CARD */}
        <div className="bg-white border border-slate-100 rounded-3xl p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-50 pb-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Lock className="w-4.5 h-4.5" />
            </div>
            <h3 className="text-sm font-extrabold text-slate-800">حماية وتحديث رمز الدخول</h3>
          </div>

          <div className="space-y-3">
            <div className="grid grid-cols-3 gap-2">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">الرمز الحالي:</label>
                <input
                  type="password"
                  maxLength={4}
                  value={currentPass}
                  onChange={(e) => setCurrentPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
                  placeholder="••••"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">الرمز الجديد:</label>
                <input
                  type="password"
                  maxLength={4}
                  value={newPass}
                  onChange={(e) => setNewPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
                  placeholder="••••"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">تأكيد الجديد:</label>
                <input
                  type="password"
                  maxLength={4}
                  value={confirmPass}
                  onChange={(e) => setConfirmPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
                  placeholder="••••"
                />
              </div>
            </div>

            {passError && (
              <p className="text-[10px] font-bold text-rose-500 mt-1">{passError}</p>
            )}
            
            {passSuccess && (
              <p className="text-[10px] font-bold text-emerald-600 mt-1">✅ تم تعديل وحفظ رمز الدخول الجديد بنجاح!</p>
            )}

            {/* Resized Blue/Cyan formatted button */}
            <div className="flex justify-end pt-1">
              <button
                onClick={handleUpdatePasscode}
                className="px-4 py-2.5 bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white text-xs font-black rounded-xl shadow-md shadow-blue-600/10 transition-colors cursor-pointer flex items-center gap-1.5"
              >
                <Key className="w-3.5 h-3.5" />
                <span>حفظ الرمز الجديد</span>
              </button>
            </div>
          </div>
        </div>

        {/* SECTION C: BACKUP & RESTORE */}
        <div className="bg-white border border-slate-100 rounded-3xl p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-50 pb-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Database className="w-4.5 h-4.5" />
            </div>
            <h3 className="text-sm font-extrabold text-slate-800">النسخ الاحتياطي والاسترجاع الشامل</h3>
          </div>

          <p className="text-[10px] text-slate-400 font-medium leading-relaxed">
            حافظ على جداولك وواجباتك من الضياع في حالة تغيير الهاتف أو مسح المتصفح. النسخ محمي بكلمة مرور.
          </p>

          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => handleTriggerSecurityCheck('backup')}
              className="p-3 bg-slate-50 hover:bg-slate-100 border border-slate-150 text-slate-700 rounded-2xl flex flex-col items-center justify-center gap-1.5 transition-colors cursor-pointer text-center"
            >
              <Download className="w-5 h-5 text-emerald-600" />
              <span className="text-xs font-black block">تصدير نسخة JSON</span>
            </button>

            <button
              onClick={() => handleTriggerSecurityCheck('restore')}
              className="p-3 bg-slate-50 hover:bg-slate-100 border border-slate-150 text-slate-700 rounded-2xl flex flex-col items-center justify-center gap-1.5 transition-colors cursor-pointer text-center"
            >
              <Upload className="w-5 h-5 text-sky-600" />
              <span className="text-xs font-black block">استرجاع نسخة JSON</span>
            </button>
          </div>
        </div>

        {/* SECTION D: WIPE DATA FACTORY RESET */}
        <div className="bg-white border border-slate-100 rounded-3xl p-5 shadow-sm flex flex-col justify-between space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-50 pb-2">
            <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Trash2 className="w-4.5 h-4.5" />
            </div>
            <h3 className="text-sm font-extrabold text-slate-800">التصفير وإعادة ضبط المصنع</h3>
          </div>

          {/* Quiet, concise layout - zero redundant paragraphs */}
          <div className="space-y-4 my-auto">
            <button
              onClick={() => handleTriggerSecurityCheck('wipe')}
              className="w-full py-4 bg-purple-600 hover:bg-purple-500 active:bg-purple-700 text-white font-black text-xs md:text-sm rounded-2xl shadow-lg shadow-purple-600/15 transition-colors cursor-pointer flex items-center justify-center gap-2"
            >
              <AlertTriangle className="w-4.5 h-4.5" />
              <span>إعادة تعيين البيانات ومسح الذاكرة</span>
            </button>
          </div>
        </div>

      </div>

      {/* ================= SECURITY GATE PASSCODE DIALOG ================= */}
      <AnimatePresence>
        {securityAction && (
          <div className="fixed inset-0 bg-slate-950/50 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl border border-slate-100"
            >
              <div className="p-5 border-b border-slate-100 bg-slate-50 text-center relative">
                <h3 className="text-sm font-extrabold text-slate-800 flex items-center justify-center gap-1.5">
                  <ShieldCheck className="w-4.5 h-4.5 text-emerald-600" />
                  <span>تأكيد الهوية للخطوة الأمنية</span>
                </h3>
                <button 
                  onClick={() => setSecurityAction(null)}
                  className="absolute left-4 top-4 text-xs font-bold text-slate-400 hover:text-slate-600"
                >
                  ✕
                </button>
              </div>

              <div className="p-6 space-y-4">
                <p className="text-xs text-slate-400 font-medium text-center leading-relaxed">
                  يرجى إدخال رمز دخول التطبيق المكون من 4 أرقام لتأكيد هذه العملية الحساسة بنجاح.
                </p>

                <div className="space-y-1">
                  <input
                    type="password"
                    maxLength={4}
                    value={securityPassAttempt}
                    onChange={(e) => {
                      setSecurityPassError(null);
                      setSecurityPassAttempt(e.target.value.replace(/\D/g, ''));
                    }}
                    className="w-48 mx-auto tracking-[0.4em] pr-[0.4em] text-center p-3 rounded-xl border border-slate-200 text-slate-800 font-mono font-black text-xl bg-slate-50 focus:border-emerald-500 focus:bg-white focus:outline-none block"
                    placeholder="••••"
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleConfirmSecurityCheck();
                    }}
                  />
                  {securityPassError && (
                    <p className="text-[11px] font-bold text-rose-500 text-center mt-1">
                      {securityPassError}
                    </p>
                  )}
                </div>
              </div>

              <div className="p-4 bg-slate-50 flex gap-2 justify-stretch border-t border-slate-100">
                <button
                  onClick={() => setSecurityAction(null)}
                  className="flex-1 py-2.5 bg-white border border-slate-200 text-xs font-bold text-slate-700 rounded-xl hover:bg-slate-50 transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleConfirmSecurityCheck}
                  className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer shadow-md shadow-emerald-600/10"
                >
                  تأكيد التحقق
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* FINAL FACTORY WIPE WARNING */}
      <AnimatePresence>
        {showWipeFinalConfirm && (
          <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-xs overflow-hidden shadow-2xl border border-slate-100"
            >
              <div className="p-6 text-center space-y-4">
                <div className="w-12 h-12 bg-rose-50 border border-rose-100 text-rose-600 rounded-full flex items-center justify-center mx-auto">
                  <AlertTriangle className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-sm font-extrabold text-slate-800">⚠️ تنبيه خطير! تأكيد نهائي</h3>
                  <p className="text-xs text-rose-600 font-bold leading-relaxed">
                    سيتم مسح كافة الجداول، التنبيهات، المهام والأسماء وإرجاع التطبيق لحالة وضع المصنع الأصلية!
                  </p>
                  <p className="text-[10px] text-slate-400 font-medium">
                    هل أنت متأكد تماماً من رغبتك في البدء من جديد؟
                  </p>
                </div>
              </div>

              <div className="p-4 bg-slate-50 flex gap-2 justify-stretch">
                <button
                  onClick={() => setShowWipeFinalConfirm(false)}
                  className="flex-1 py-2.5 bg-white border border-slate-200 text-xs font-bold text-slate-700 rounded-xl hover:bg-slate-50 transition-colors cursor-pointer"
                >
                  تراجع وإلغاء
                </button>
                <button
                  onClick={executeFactoryWipe}
                  className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer shadow-md shadow-rose-600/10"
                >
                  نعم، مسح كل شيء
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
