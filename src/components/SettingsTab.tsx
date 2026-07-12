import { useState, useEffect } from 'react';
import { Capacitor } from '@capacitor/core';
import { 
  User, Lock, Key, Database, Download, Upload, Trash2, RefreshCw, Check, AlertTriangle, ShieldCheck 
} from 'lucide-react';
import { AppState } from '../types';
import { INITIAL_STATE } from '../defaultData';
import { speakArabicText } from '../audioHelper';
import { motion, AnimatePresence } from 'motion/react';
import { AlarmPlugin } from '../utils/alarmSync';

interface SettingsTabProps {
  state: AppState;
  onSaveState: (newState: AppState) => void;
  onLockApp: () => void;
}

export default function SettingsTab({ state, onSaveState, onLockApp }: SettingsTabProps) {
  // 1. Name Profile States
  const [localName, setLocalName] = useState<string>(state.studentName);

  // Android Permissions & Testing States
  const [hasNotificationPermission, setHasNotificationPermission] = useState<boolean>(true);
  const [hasOverlayPermission, setHasOverlayPermission] = useState<boolean>(true);
  const [isAndroidPlatform, setIsAndroidPlatform] = useState<boolean>(false);
  const [testCountdown, setTestCountdown] = useState<number | null>(null);

  // Check Android permissions on mount or visibility change
  useEffect(() => {
    const checkPermissions = async () => {
      const isNative = Capacitor.isNativePlatform();
      setIsAndroidPlatform(isNative);
      if (isNative) {
        try {
          if (AlarmPlugin && typeof AlarmPlugin.checkOverlayPermission === 'function') {
            const overlayRes = await AlarmPlugin.checkOverlayPermission();
            setHasOverlayPermission(!!overlayRes.hasPermission);
          }
          if (AlarmPlugin && typeof AlarmPlugin.checkNotificationPermission === 'function') {
            const notifyRes = await AlarmPlugin.checkNotificationPermission();
            setHasNotificationPermission(!!notifyRes.hasPermission);
          }
        } catch (e) {
          console.warn("Error checking native permissions:", e);
        }
      }
    };

    checkPermissions();

    const handleFocus = () => {
      checkPermissions();
    };
    window.addEventListener('focus', handleFocus);
    window.addEventListener('visibilitychange', handleFocus);
    return () => {
      window.removeEventListener('focus', handleFocus);
      window.removeEventListener('visibilitychange', handleFocus);
    };
  }, []);

  const handleRequestNotificationPermission = async () => {
    try {
      if (AlarmPlugin && typeof AlarmPlugin.requestNotificationPermission === 'function') {
        await AlarmPlugin.requestNotificationPermission();
        speakArabicText("يرجى الموافقة على صلاحية الإشعارات");
        setTimeout(async () => {
          const notifyRes = await AlarmPlugin.checkNotificationPermission();
          setHasNotificationPermission(!!notifyRes.hasPermission);
        }, 1500);
      }
    } catch (e) {
      console.warn("Failed to request notification permission:", e);
    }
  };

  const handleRequestOverlayPermission = async () => {
    try {
      if (AlarmPlugin && typeof AlarmPlugin.requestOverlayPermission === 'function') {
        await AlarmPlugin.requestOverlayPermission();
        speakArabicText("يرجى تفعيل خيار الظهور فوق التطبيقات لجدول الطالب الذكي");
      }
    } catch (e) {
      console.warn("Failed to request overlay permission:", e);
    }
  };

  const handleTriggerTestAlarm = async () => {
    try {
      if (AlarmPlugin && typeof AlarmPlugin.triggerTestAlarm === 'function') {
        await AlarmPlugin.triggerTestAlarm();
        speakArabicText("جاري جدولة منبه تجريبي بعد خمس ثوان. قم بقفل شاشة هاتفك الآن لتجربته!");
        
        // Start a visual countdown
        setTestCountdown(5);
        const timer = setInterval(() => {
          setTestCountdown((prev) => {
            if (prev === null || prev <= 1) {
              clearInterval(timer);
              return null;
            }
            return prev - 1;
          });
        }, 1000);
      } else {
        // Fallback for non-native / web simulation
        speakArabicText("تجربة المنبه غير متاحة على المتصفح العادي");
        alert("⚠️ هذه الميزة متاحة فقط على الهواتف والأجهزة الذكية بنظام أندرويد.");
      }
    } catch (e) {
      console.warn("Failed to trigger test alarm:", e);
    }
  };

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
  const triggerRestoreUpload = async () => {
    let rawTextData = '';

    if (Capacitor.isNativePlatform()) {
      try {
        const { AlarmPlugin } = await import('../utils/alarmSync');
        const res = await AlarmPlugin.importBackup();
        if (res && res.success && res.data) {
          rawTextData = res.data;
        } else {
          return; // User cancelled or failed
        }
      } catch (err) {
        console.warn("Native import failed, trying fallback Web input:", err);
      }
    }

    if (rawTextData) {
      processImportedJson(rawTextData);
    } else {
      // Fallback: standard web file input
      const fileInput = document.createElement('input');
      fileInput.type = 'file';
      // Use both MIME types and file extensions to ensure maximum compatibility across older system WebViews
      fileInput.accept = 'application/json,text/plain,.json';
      fileInput.onchange = (e: any) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event: any) => {
          processImportedJson(event.target.result);
        };
        reader.readAsText(file);
      };
      fileInput.click();
    }
  };

  const processImportedJson = (jsonString: string) => {
    try {
      const importedData = JSON.parse(jsonString);
      
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

      {/* 1.5. Android Permissions & Alarm Testing (Native & Simulation Section) */}
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-5 text-white space-y-4 shadow-xl text-right">
        <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-950 text-emerald-400 flex items-center justify-center">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-black text-white">إعدادات صلاحيات المنبه وهواتف الأندرويد 📱</h3>
            <p className="text-[10px] text-slate-400 font-medium">لضمان عمل شاشة التنبيه ورنين الصوت بنجاح حتى لو كانت شاشة الهاتف مغلقة والتطبيق مغلقاً</p>
          </div>
        </div>

        {!isAndroidPlatform && (
          <div className="bg-amber-950/40 border border-amber-500/20 rounded-2xl p-3 text-[11px] font-bold text-amber-400 leading-relaxed text-center">
            ⚠️ وضع المحاكاة: الصلاحيات مفعلة تلقائياً في المتصفح. عند تثبيت التطبيق بصيغة APK على هاتف أندرويد، ستتمكن من التحكم الفعلي بالصلاحيات هنا.
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Status indicators */}
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-slate-950 rounded-2xl border border-slate-850">
              <span className="text-xs font-bold text-slate-300">صلاحية إرسال الإشعارات (رنين الصوت)</span>
              {(!isAndroidPlatform || hasNotificationPermission) ? (
                <span className="px-2.5 py-1 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-500/20 text-[10px] font-black">مفعلة ✅</span>
              ) : (
                <button
                  onClick={handleRequestNotificationPermission}
                  className="px-2.5 py-1 rounded-full bg-rose-950 text-rose-400 border border-rose-500/20 text-[10px] font-black cursor-pointer hover:bg-rose-900 transition-colors"
                >
                  غير مفعلة (اضغط للتفعيل ⚠️)
                </button>
              )}
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-950 rounded-2xl border border-slate-850">
              <span className="text-xs font-bold text-slate-300">صلاحية الظهور فوق التطبيقات (شاشة التنبيه)</span>
              {(!isAndroidPlatform || hasOverlayPermission) ? (
                <span className="px-2.5 py-1 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-500/20 text-[10px] font-black">مفعلة ✅</span>
              ) : (
                <button
                  onClick={handleRequestOverlayPermission}
                  className="px-2.5 py-1 rounded-full bg-rose-950 text-rose-400 border border-rose-500/20 text-[10px] font-black cursor-pointer hover:bg-rose-900 transition-colors"
                >
                  غير مفعلة (اضغط للتفعيل ⚠️)
                </button>
              )}
            </div>
          </div>

          {/* Test Alarm Trigger */}
          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-850 flex flex-col justify-between gap-3">
            <div>
              <h4 className="text-xs font-extrabold text-slate-200">تجربة واختبار نظام التنبيه الذكي ⚡</h4>
              <p className="text-[10px] text-slate-400 font-medium leading-relaxed mt-1">
                اضغط على الزر أدناه، ثم قم بقفل شاشة هاتفك فوراً. بعد 5 ثوانٍ، سيقوم الهاتف بالرنين وفتح شاشة التنبيه مباشرة لتجربتها والتأكد من نجاحها!
              </p>
            </div>

            <button
              disabled={testCountdown !== null}
              onClick={handleTriggerTestAlarm}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 text-white font-black text-xs rounded-xl transition-colors cursor-pointer flex items-center justify-center gap-1.5"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${testCountdown !== null ? 'animate-spin' : ''}`} />
              <span>
                {testCountdown !== null 
                  ? `جاري بدء التجربة خلال ${testCountdown} ثوانٍ... قفل الشاشة الآن! 🔒` 
                  : "بدء تجربة المنبه الذكي (خلال 5 ثوانٍ)"}
              </span>
            </button>
          </div>
        </div>

        {/* Device specific help block */}
        <div className="bg-emerald-950/20 border border-emerald-500/10 rounded-2xl p-4 text-xs font-medium text-emerald-400/90 leading-relaxed space-y-1.5">
          <p className="font-extrabold text-emerald-300">💡 تعليمات هامة لهواتف الأندرويد الخاصة بالشركات (شاومي، هواوي، سامسونج، أوبو):</p>
          <p className="text-[11px] leading-relaxed text-emerald-400/80">
            لأن بعض الهواتف تمنع التطبيقات من الاستيقاظ في الخلفية افتراضياً، يرجى القيام بالخطوات التالية لضمان عمل المنبه بنسبة 100%:
          </p>
          <ul className="list-disc list-inside text-[11px] space-y-1 text-slate-300 mr-2">
            <li>ادخل إلى <strong>إعدادات الهاتف</strong> ➜ <strong>التطبيقات</strong> ➜ <strong>إدارة التطبيقات</strong> ➜ اختر <strong>جدول الطالب الذكي</strong>.</li>
            <li>قم بتفعيل خيار <strong>التشغيل التلقائي (Auto-start)</strong>.</li>
            <li>ادخل إلى <strong>صلاحيات أخرى (Other permissions)</strong> وقم بتفعيل خيار <strong>عرض على شاشة القفل (Show on lock screen)</strong> وخيار <strong>عرض نوافذ منبثقة أثناء التشغيل في الخلفية (Display pop-up windows in background)</strong>.</li>
            <li>ادخل إلى <strong>موفر البطارية (Battery saver)</strong> واجعله <strong>بلا قيود (No restrictions)</strong> لمنع نظام أندرويد من قتل خدمة رنين المنبه الذكي.</li>
          </ul>
        </div>
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
                  inputMode="numeric"
                  maxLength={4}
                  value={currentPass}
                  onChange={(e) => setCurrentPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">الرمز الجديد:</label>
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  value={newPass}
                  onChange={(e) => setNewPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">تأكيد الجديد:</label>
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  value={confirmPass}
                  onChange={(e) => setConfirmPass(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-center text-slate-800 font-mono font-bold text-sm bg-slate-50 focus:bg-white focus:outline-none focus:border-emerald-500"
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
                    inputMode="numeric"
                    maxLength={4}
                    value={securityPassAttempt}
                    onChange={(e) => {
                      setSecurityPassError(null);
                      setSecurityPassAttempt(e.target.value.replace(/\D/g, ''));
                    }}
                    className="w-48 mx-auto tracking-[0.4em] pr-[0.4em] text-center p-3 rounded-xl border border-slate-200 text-slate-800 font-mono font-black text-xl bg-slate-50 focus:border-emerald-500 focus:bg-white focus:outline-none block"
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
