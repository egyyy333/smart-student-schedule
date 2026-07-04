import React, { useState, useEffect, useRef } from 'react';
import { 
  Maximize2, Minimize2, Trash2, Sparkles, Bell, Save, Undo, Check, X, Clock, HelpCircle, ArrowUpDown, ChevronUp, ChevronDown 
} from 'lucide-react';
import { ScheduleData, AppState } from '../types';
import { DAYS, getSchoolTemplate, getTutoringTemplate, getStudyTemplate } from '../defaultData';
import { motion, AnimatePresence } from 'motion/react';
import { speakArabicText } from '../audioHelper';

interface SchedulesTabProps {
  state: AppState;
  onSaveState: (newState: AppState) => void;
}

type ActiveScheduleType = 'school' | 'tutoring' | 'study';

export default function SchedulesTab({ state, onSaveState }: SchedulesTabProps) {
  const [activeTab, setActiveTab] = useState<ActiveScheduleType>('school');
  
  // Local scratchpad states to hold unsaved schedule modifications
  const [localSchool, setLocalSchool] = useState<ScheduleData>(JSON.parse(JSON.stringify(state.schoolSchedule)));
  const [localTutoring, setLocalTutoring] = useState<ScheduleData>(JSON.parse(JSON.stringify(state.tutoringSchedule)));
  const [localStudy, setLocalStudy] = useState<ScheduleData>(JSON.parse(JSON.stringify(state.studySchedule)));

  // Sync with main state if it updates from elsewhere (like backup restore)
  useEffect(() => {
    setLocalSchool(JSON.parse(JSON.stringify(state.schoolSchedule)));
    setLocalTutoring(JSON.parse(JSON.stringify(state.tutoringSchedule)));
    setLocalStudy(JSON.parse(JSON.stringify(state.studySchedule)));
  }, [state.schoolSchedule, state.tutoringSchedule, state.studySchedule]);

  // Keyboard navigation ref tracker
  const [focusedCell, setFocusedCell] = useState<{ day: string; header: string } | null>(null);

  // Active schedule state helpers
  const getActiveLocal = (): ScheduleData => {
    if (activeTab === 'school') return localSchool;
    if (activeTab === 'tutoring') return localTutoring;
    return localStudy;
  };

  const getOriginalLocal = (): ScheduleData => {
    if (activeTab === 'school') return state.schoolSchedule;
    if (activeTab === 'tutoring') return state.tutoringSchedule;
    return state.studySchedule;
  };

  const updateActiveLocal = (updater: (prev: ScheduleData) => ScheduleData) => {
    if (activeTab === 'school') setLocalSchool(updater);
    else if (activeTab === 'tutoring') setLocalTutoring(updater);
    else setLocalStudy(updater);
  };

  // Change tracking indicator
  const activeLocal = getActiveLocal();
  const originalLocal = getOriginalLocal();
  const hasChanges = JSON.stringify(activeLocal) !== JSON.stringify(originalLocal);

  // Modal and custom sheet states
  const [editCell, setEditCell] = useState<{ day: string; header: string; value: string } | null>(null);
  const [editHeaderIndex, setEditHeaderIndex] = useState<{ index: number; name: string } | null>(null);
  const [showClearConfirm, setShowClearConfirm] = useState<boolean>(false);
  const [showPasscodeModal, setShowPasscodeModal] = useState<boolean>(false);
  const [passcodeAttempt, setPasscodeAttempt] = useState<string>('');
  const [passcodeError, setPasscodeError] = useState<string | null>(null);

  // Alarms settings modal states
  const [showAlarmModal, setShowAlarmModal] = useState<boolean>(false);
  const [timepickerTarget, setTimepickerTarget] = useState<string | null>(null); // column header
  const [wheelHour, setWheelHour] = useState<number>(4);
  const [wheelMinute, setWheelMinute] = useState<number>(0);
  const [wheelAmPm, setWheelAmPm] = useState<'AM' | 'PM'>('PM');

  // Trigger default templates
  const handleLoadTemplate = () => {
    updateActiveLocal(prev => {
      const copy = { ...prev };
      if (activeTab === 'school') {
        copy.grid = getSchoolTemplate();
      } else if (activeTab === 'tutoring') {
        copy.grid = getTutoringTemplate();
      } else {
        copy.grid = getStudyTemplate();
      }
      return copy;
    });
  };

  // Zoom control
  const handleZoom = (direction: 'in' | 'out') => {
    updateActiveLocal(prev => {
      const copy = { ...prev };
      const currentZoom = copy.zoomLevel || 100;
      if (direction === 'in') {
        copy.zoomLevel = Math.min(150, currentZoom + 10);
      } else {
        copy.zoomLevel = Math.max(80, currentZoom - 10);
      }
      return copy;
    });
  };

  // Clear Grid
  const handleClearGrid = () => {
    updateActiveLocal(prev => {
      const copy = { ...prev };
      const clearedGrid: Record<string, Record<string, string>> = {};
      DAYS.forEach(day => {
        clearedGrid[day] = {};
        copy.headers.forEach(header => {
          clearedGrid[day][header] = "";
        });
      });
      copy.grid = clearedGrid;
      return copy;
    });
    setShowClearConfirm(false);
  };

  // Cell modify actions
  const handleOpenEditCell = (day: string, header: string) => {
    const currentValue = activeLocal.grid[day]?.[header] || "";
    setEditCell({ day, header, value: currentValue });
  };

  const handleSaveCell = () => {
    if (!editCell) return;
    updateActiveLocal(prev => {
      const copy = { ...prev };
      if (!copy.grid[editCell.day]) copy.grid[editCell.day] = {};
      copy.grid[editCell.day][editCell.header] = editCell.value.trim();
      return copy;
    });
    setEditCell(null);
  };

  const handleClearCellSingle = (day: string, header: string, e: React.MouseEvent) => {
    e.stopPropagation(); // Avoid triggering edit modal
    updateActiveLocal(prev => {
      const copy = { ...prev };
      if (copy.grid[day]) {
        copy.grid[day][header] = "";
      }
      return copy;
    });
  };

  // Header rename actions
  const handleOpenEditHeader = (index: number, name: string) => {
    setEditHeaderIndex({ index, name });
  };

  const handleSaveHeader = () => {
    if (!editHeaderIndex) return;
    const oldName = activeLocal.headers[editHeaderIndex.index];
    const newName = editHeaderIndex.name.trim() || oldName;

    if (oldName === newName) {
      setEditHeaderIndex(null);
      return;
    }

    updateActiveLocal(prev => {
      const copy = { ...prev };
      // Update headers list
      const updatedHeaders = [...copy.headers];
      updatedHeaders[editHeaderIndex.index] = newName;
      copy.headers = updatedHeaders;

      // Map old grid data keys to new headers
      DAYS.forEach(day => {
        if (copy.grid[day]) {
          const value = copy.grid[day][oldName] || "";
          copy.grid[day][newName] = value;
          delete copy.grid[day][oldName];
        }
      });

      // Update alarm configuration key
      if (copy.alarmConfig.times && copy.alarmConfig.times[oldName]) {
        const timeVal = copy.alarmConfig.times[oldName];
        copy.alarmConfig.times[newName] = timeVal;
        delete copy.alarmConfig.times[oldName];
      }

      return copy;
    });
    setEditHeaderIndex(null);
  };

  // Undo changes
  const handleUndo = () => {
    if (activeTab === 'school') {
      setLocalSchool(JSON.parse(JSON.stringify(state.schoolSchedule)));
    } else if (activeTab === 'tutoring') {
      setLocalTutoring(JSON.parse(JSON.stringify(state.tutoringSchedule)));
    } else {
      setLocalStudy(JSON.parse(JSON.stringify(state.studySchedule)));
    }
  };

  // Save changes & lock with passcode
  const handleTriggerSave = () => {
    setPasscodeAttempt('');
    setPasscodeError(null);
    setShowPasscodeModal(true);
  };

  const handleConfirmSaveWithPasscode = () => {
    if (passcodeAttempt === state.passcode) {
      const newState = { ...state };
      if (activeTab === 'school') {
        newState.schoolSchedule = localSchool;
      } else if (activeTab === 'tutoring') {
        newState.tutoringSchedule = localTutoring;
      } else {
        newState.studySchedule = localStudy;
      }
      onSaveState(newState);
      setShowPasscodeModal(false);
      speakArabicText("تم حفظ الجدول بنجاح");
    } else {
      setPasscodeError('⚠️ رمز الدخول خاطئ، يرجى المحاولة مرة أخرى.');
    }
  };

  // Keyboard Navigation Handling
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (editCell || editHeaderIndex || showClearConfirm || showPasscodeModal || showAlarmModal || timepickerTarget) {
        if (e.key === 'Escape') {
          setEditCell(null);
          setEditHeaderIndex(null);
          setShowClearConfirm(false);
          setShowPasscodeModal(false);
          setShowAlarmModal(false);
          setTimepickerTarget(null);
        }
        return;
      }

      if (!focusedCell) return;

      const currentDayIdx = DAYS.indexOf(focusedCell.day);
      const currentHeaderIdx = activeLocal.headers.indexOf(focusedCell.header);

      if (e.key === 'ArrowUp') {
        e.preventDefault();
        const nextDayIdx = (currentDayIdx - 1 + DAYS.length) % DAYS.length;
        setFocusedCell({ day: DAYS[nextDayIdx], header: focusedCell.header });
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        const nextDayIdx = (currentDayIdx + 1) % DAYS.length;
        setFocusedCell({ day: DAYS[nextDayIdx], header: focusedCell.header });
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        // Shift column left
        const nextHeaderIdx = (currentHeaderIdx + 1) % activeLocal.headers.length;
        setFocusedCell({ day: focusedCell.day, header: activeLocal.headers[nextHeaderIdx] });
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        // Shift column right
        const nextHeaderIdx = (currentHeaderIdx - 1 + activeLocal.headers.length) % activeLocal.headers.length;
        setFocusedCell({ day: focusedCell.day, header: activeLocal.headers[nextHeaderIdx] });
      } else if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleOpenEditCell(focusedCell.day, focusedCell.header);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [focusedCell, activeLocal, editCell, editHeaderIndex, showClearConfirm, showPasscodeModal, showAlarmModal, timepickerTarget]);

  // Alarm management helpers
  const handleToggleAlarmActive = () => {
    updateActiveLocal(prev => {
      const copy = { ...prev };
      copy.alarmConfig = {
        ...copy.alarmConfig,
        enabled: !copy.alarmConfig.enabled
      };
      return copy;
    });
  };

  const handleOpenTimepicker = (header: string) => {
    setTimepickerTarget(header);
    // Parse current time to pre-select wheels
    const currentTimeStr = activeLocal.alarmConfig.times[header] || "12:00";
    const [hours24, minutes] = currentTimeStr.split(":").map(Number);
    let h12 = hours24 % 12;
    if (h12 === 0) h12 = 12;
    const ampm = hours24 >= 12 ? "PM" : "AM";
    setWheelHour(h12);
    setWheelMinute(minutes || 0);
    setWheelAmPm(ampm);
  };

  const handleConfirmTimepicker = () => {
    if (!timepickerTarget) return;
    // Calculate 24-hour format string
    let finalHour = wheelHour;
    if (wheelAmPm === 'PM' && wheelHour < 12) finalHour += 12;
    if (wheelAmPm === 'AM' && wheelHour === 12) finalHour = 0;
    
    const formattedHour = String(finalHour).padStart(2, '0');
    const formattedMin = String(wheelMinute).padStart(2, '0');
    const time24Str = `${formattedHour}:${formattedMin}`;

    updateActiveLocal(prev => {
      const copy = { ...prev };
      copy.alarmConfig = {
        ...copy.alarmConfig,
        times: {
          ...copy.alarmConfig.times,
          [timepickerTarget]: time24Str
        }
      };
      return copy;
    });
    setTimepickerTarget(null);
  };

  // Map 24-hour time to beautiful localized 12-hour format string
  const formatTime12 = (time24: string): string => {
    if (!time24) return "غيّر مفعل";
    const [h, m] = time24.split(":").map(Number);
    const ampm = h >= 12 ? "م" : "ص";
    let h12 = h % 12;
    if (h12 === 0) h12 = 12;
    return `${h12}:${String(m).padStart(2, '0')} ${ampm}`;
  };

  // Render a cell's custom CSS styling depending on schedule type
  const getCellClassName = (hasContent: boolean, isFocused: boolean) => {
    const base = "p-3 h-16 min-w-[130px] border-b border-l border-slate-100 text-center relative group cursor-pointer transition-all flex items-center justify-center text-xs font-semibold select-none ";
    const focusRing = isFocused ? "ring-2 ring-emerald-500 ring-inset bg-emerald-50/50 " : "";
    
    if (activeTab === 'school') {
      return base + focusRing + (hasContent 
        ? "bg-cyan-50/70 text-cyan-900 border-cyan-100 hover:bg-cyan-100/50" 
        : "bg-white hover:bg-slate-50 text-slate-400");
    } else if (activeTab === 'tutoring') {
      return base + focusRing + (hasContent 
        ? "bg-emerald-50 text-emerald-900 border-emerald-100 hover:bg-emerald-100/60" 
        : "bg-white hover:bg-slate-50 text-slate-400");
    } else {
      return base + focusRing + (hasContent 
        ? "bg-indigo-50/80 text-indigo-900 border-indigo-100 hover:bg-indigo-100/50" 
        : "bg-white hover:bg-slate-50 text-slate-400");
    }
  };

  return (
    <div className="space-y-6 font-sans">
      
      {/* 1. Schedule Switching Tabs */}
      <div className="flex bg-white p-1 rounded-2xl border border-slate-100 shadow-sm max-w-xl mx-auto">
        <button
          onClick={() => setActiveTab('school')}
          className={`flex-1 py-3 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
            activeTab === 'school' 
              ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/15' 
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          }`}
        >
          جدول الحصص المدرسية 🏫
        </button>
        <button
          onClick={() => setActiveTab('tutoring')}
          className={`flex-1 py-3 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
            activeTab === 'tutoring' 
              ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/15' 
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          }`}
        >
          جدول الدروس الخصوصية 🎓
        </button>
        <button
          onClick={() => setActiveTab('study')}
          className={`flex-1 py-3 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
            activeTab === 'study' 
              ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/15' 
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          }`}
        >
          جدول المذاكرة والمراجعة ✍️
        </button>
      </div>

      {/* 2. Control Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-white border border-slate-100 rounded-2xl p-4 shadow-sm">
        
        {/* Left Side: Zoom Level and Instructions */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleZoom('out')}
            className="w-9 h-9 bg-slate-50 hover:bg-slate-100 active:bg-slate-200 text-slate-700 rounded-xl flex items-center justify-center border border-slate-100 transition-colors cursor-pointer"
            title="تصغير حجم الخط"
          >
            <Minimize2 className="w-4 h-4" />
          </button>
          <span className="text-xs font-mono font-bold text-slate-500 bg-slate-50 px-2.5 py-1 rounded-md border border-slate-100">
            {activeLocal.zoomLevel || 100}%
          </span>
          <button
            onClick={() => handleZoom('in')}
            className="w-9 h-9 bg-slate-50 hover:bg-slate-100 active:bg-slate-200 text-slate-700 rounded-xl flex items-center justify-center border border-slate-100 transition-colors cursor-pointer"
            title="تكبير حجم الخط"
          >
            <Maximize2 className="w-4 h-4" />
          </button>

          {/* Quick Keyboard Instruction Info */}
          <div className="hidden sm:flex items-center gap-1 text-[11px] text-slate-400 bg-slate-50/50 px-3 py-1.5 rounded-full border border-slate-100">
            <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
            <span>استخدم الأسهم 🧭 و Enter للتحرير السريع بالكيبورد</span>
          </div>
        </div>

        {/* Right Side: Actions (Templates, Clear, Alarm) */}
        <div className="flex items-center gap-2 flex-wrap">
          
          {/* Custom alarms config (Excluding School schedule) */}
          {(activeTab === 'tutoring' || activeTab === 'study') && (
            <button
              onClick={() => setShowAlarmModal(true)}
              className={`px-4 py-2 rounded-xl flex items-center gap-1.5 text-xs font-extrabold transition-all cursor-pointer ${
                activeLocal.alarmConfig.enabled 
                  ? 'bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 shadow-sm shadow-amber-500/5' 
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border border-slate-200'
              }`}
            >
              <Bell className={`w-4 h-4 ${activeLocal.alarmConfig.enabled ? 'text-amber-500 animate-swing' : ''}`} />
              <span>إعدادات التنبيه</span>
            </button>
          )}

          {/* Autofill Template */}
          <button
            onClick={handleLoadTemplate}
            className="px-4 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-xl flex items-center gap-1.5 text-xs font-extrabold transition-colors cursor-pointer"
          >
            <Sparkles className="w-4 h-4" />
            <span>تعبئة نموذج افتراضي</span>
          </button>

          {/* Clear Grid */}
          <button
            onClick={() => setShowClearConfirm(true)}
            className="w-9 h-9 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 rounded-xl flex items-center justify-center transition-colors cursor-pointer"
            title="مسح كامل الجدول"
          >
            <Trash2 className="w-4 h-4" />
          </button>

        </div>

      </div>

      {/* 3. The Responsive Grid Table */}
      <div className="bg-white border border-slate-100 rounded-3xl overflow-hidden shadow-sm">
        
        {/* Horizontal wrapping container to support Landscape beautifully */}
        <div className="overflow-x-auto w-full">
          <table 
            className="w-full border-collapse" 
            style={{ fontSize: `${(activeLocal.zoomLevel || 100) / 100}rem` }}
          >
            {/* Header: Emerald themed */}
            <thead>
              <tr className="bg-emerald-700 text-white">
                <th className="p-4 text-xs font-extrabold w-[90px] border-l border-emerald-800 text-right font-sans">
                  اليوم
                </th>
                {activeLocal.headers.map((header, idx) => (
                  <th 
                    key={idx} 
                    onClick={() => handleOpenEditHeader(idx, header)}
                    className="p-4 text-xs font-black tracking-wide border-l border-emerald-800 text-center cursor-pointer hover:bg-emerald-800/80 transition-colors select-none group min-w-[130px]"
                    title="انقر لتعديل اسم الفترة/الوقت"
                  >
                    <div className="flex items-center justify-center gap-1.5">
                      <span>{header}</span>
                      <span className="text-[9px] text-emerald-300 font-bold opacity-0 group-hover:opacity-100 transition-opacity">✏️</span>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>

            {/* Grid Body */}
            <tbody>
              {DAYS.map((day) => (
                <tr key={day} className="border-b border-slate-100 hover:bg-slate-50/30">
                  {/* Day Label column */}
                  <td className="p-3 text-xs font-extrabold text-slate-700 bg-slate-50/80 border-l border-slate-100 text-right select-none">
                    {day}
                  </td>
                  
                  {/* Cells mapping */}
                  {activeLocal.headers.map((header) => {
                    const value = activeLocal.grid[day]?.[header] || "";
                    const isFocused = focusedCell?.day === day && focusedCell?.header === header;
                    
                    return (
                      <td
                        key={header}
                        onClick={() => handleOpenEditCell(day, header)}
                        onFocus={() => setFocusedCell({ day, header })}
                        tabIndex={0}
                        className={getCellClassName(!!value, isFocused)}
                      >
                        {value ? (
                          <div className="flex flex-col items-center justify-center w-full px-1.5 leading-snug">
                            <span>{value}</span>
                            
                            {/* Small clear "X" button on hover */}
                            <button
                              onClick={(e) => handleClearCellSingle(day, header, e)}
                              className="absolute top-1 left-1 w-4 h-4 bg-slate-200/90 hover:bg-rose-500 hover:text-white rounded-full flex items-center justify-center text-[9px] opacity-0 group-hover:opacity-100 transition-opacity"
                              title="تفريغ الخلية"
                            >
                              ✕
                            </button>
                          </div>
                        ) : (
                          <span className="text-[10px] text-slate-300 font-normal opacity-0 group-hover:opacity-100 transition-opacity select-none">
                            + إضافة
                          </span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

      </div>

      {/* 4. Bottom Synchronization Status Bar */}
      <div className="flex items-center justify-between flex-wrap gap-4 bg-slate-900 text-white rounded-2xl p-4 shadow-lg">
        
        {/* Left Side: Change tracker status */}
        <div className="flex items-center gap-2">
          {hasChanges ? (
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-rose-500 animate-ping" />
              <span className="text-xs font-bold text-rose-400">التعديلات غير محفوظة</span>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-emerald-500" />
              <span className="text-xs font-bold text-emerald-400">الجدول محفوظ بالذاكرة</span>
            </div>
          )}
        </div>

        {/* Right Side: Undo / Save buttons */}
        <div className="flex items-center gap-2">
          {hasChanges && (
            <button
              onClick={handleUndo}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Undo className="w-4 h-4" />
              <span>تراجع</span>
            </button>
          )}
          
          <button
            onClick={handleTriggerSave}
            disabled={!hasChanges}
            className={`px-5 py-2.5 rounded-xl flex items-center gap-2 text-xs font-black transition-all cursor-pointer ${
              hasChanges 
                ? 'bg-emerald-500 hover:bg-emerald-400 text-white shadow-md shadow-emerald-500/25 active:scale-95' 
                : 'bg-slate-800 text-slate-500 border border-slate-800 cursor-not-allowed'
            }`}
          >
            <Save className="w-4 h-4" />
            <span>حفظ الجدول الآن</span>
          </button>
        </div>

      </div>

      {/* ================= MODALS & OVERLAYS ================= */}

      {/* Edit Cell Dialog */}
      <AnimatePresence>
        {editCell && (
          <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl border border-slate-100 max-h-[90vh] flex flex-col"
            >
              <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                <h3 className="text-sm font-extrabold text-slate-800 flex items-center gap-2">
                  <span>✏️ تعديل محتوى الخلية</span>
                </h3>
                <button 
                  onClick={() => setEditCell(null)}
                  className="w-7 h-7 rounded-full bg-slate-200 hover:bg-slate-300 flex items-center justify-center text-slate-500 transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="p-5 space-y-4 overflow-y-auto">
                <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl text-xs font-bold text-slate-600">
                  <span>اليوم: <span className="text-slate-900 font-black">{editCell.day}</span></span>
                  <span>الوقت/الفترة: <span className="text-slate-900 font-black">{editCell.header}</span></span>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">المادة أو عنوان النشاط الدراسي:</label>
                  <input
                    type="text"
                    value={editCell.value}
                    onChange={(e) => setEditCell({ ...editCell, value: e.target.value })}
                    className="w-full p-3 rounded-xl border border-slate-200 text-slate-800 font-bold text-sm bg-slate-50/50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all"
                    placeholder="مثال: رياضيات، كيمياء، مراجعة فيزياء..."
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleSaveCell();
                    }}
                  />
                </div>
              </div>

              <div className="p-5 border-t border-slate-50 bg-slate-50/50 flex gap-2 justify-end">
                <button
                  onClick={() => setEditCell(null)}
                  className="px-4 py-2 text-xs font-bold text-slate-500 hover:bg-slate-200 rounded-xl transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleSaveCell}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer shadow-md shadow-emerald-600/10"
                >
                  تأكيد الحفظ
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Rename Period/Column Header Dialog */}
      <AnimatePresence>
        {editHeaderIndex && (
          <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl border border-slate-100 max-h-[90vh] flex flex-col"
            >
              <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                <h3 className="text-sm font-extrabold text-slate-800">
                  🏷️ تعديل مسمى العمود / الوقت
                </h3>
                <button 
                  onClick={() => setEditHeaderIndex(null)}
                  className="w-7 h-7 rounded-full bg-slate-200 hover:bg-slate-300 flex items-center justify-center text-slate-500 transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="p-5 space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">مسمى العمود الحالي:</label>
                  <input
                    type="text"
                    value={editHeaderIndex.name}
                    onChange={(e) => setEditHeaderIndex({ ...editHeaderIndex, name: e.target.value })}
                    className="w-full p-3 rounded-xl border border-slate-200 text-slate-800 font-bold text-sm bg-slate-50/50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all"
                    placeholder="مثال: الحصة 1 (08:00 ص)..."
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleSaveHeader();
                    }}
                  />
                </div>
              </div>

              <div className="p-5 border-t border-slate-50 bg-slate-50/50 flex gap-2 justify-end">
                <button
                  onClick={() => setEditHeaderIndex(null)}
                  className="px-4 py-2 text-xs font-bold text-slate-500 hover:bg-slate-200 rounded-xl transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleSaveHeader}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer"
                >
                  تأكيد التعديل
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Clear Grid Confirmation Dialog */}
      <AnimatePresence>
        {showClearConfirm && (
          <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-xs overflow-hidden shadow-2xl border border-slate-100"
            >
              <div className="p-5 text-center space-y-4">
                <div className="w-12 h-12 bg-rose-50 border border-rose-100 text-rose-600 rounded-full flex items-center justify-center mx-auto">
                  <Trash2 className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-sm font-extrabold text-slate-800">هل أنت متأكد من مسح الجدول؟</h3>
                  <p className="text-xs text-slate-400 font-medium leading-relaxed">
                    سيتم إفراغ كافة الخلايا في هذا الجدول بالكامل. هذه الخطوة لا يمكن التراجع عنها إلا في حالة عدم الحفظ.
                  </p>
                </div>
              </div>

              <div className="p-4 bg-slate-50 flex gap-2 justify-stretch">
                <button
                  onClick={() => setShowClearConfirm(false)}
                  className="flex-1 py-2.5 bg-white border border-slate-200 text-xs font-bold text-slate-700 rounded-xl hover:bg-slate-50 transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleClearGrid}
                  className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer shadow-md shadow-rose-600/10"
                >
                  تأكيد المسح
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Passcode Lock Input Dialog for Saving */}
      <AnimatePresence>
        {showPasscodeModal && (
          <div className="fixed inset-0 bg-slate-950/50 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl border border-slate-100"
            >
              <div className="p-5 border-b border-slate-100 bg-slate-50 text-center relative">
                <h3 className="text-sm font-extrabold text-slate-800">🔒 تأكيد الهوية لحفظ الجدول</h3>
                <button 
                  onClick={() => setShowPasscodeModal(false)}
                  className="absolute left-4 top-4 text-xs font-bold text-slate-400 hover:text-slate-600"
                >
                  ✕
                </button>
              </div>

              <div className="p-6 space-y-4">
                <p className="text-xs text-slate-400 font-medium text-center leading-relaxed">
                  لحماية جدولك من تطفل الآخرين، يرجى كتابة رمز دخول التطبيق المكون من 4 أرقام لتأكيد الحفظ بالذاكرة.
                </p>

                <div className="space-y-1">
                  <input
                    type="password"
                    maxLength={4}
                    value={passcodeAttempt}
                    onChange={(e) => {
                      setPasscodeError(null);
                      setPasscodeAttempt(e.target.value.replace(/\D/g, ''));
                    }}
                    className="w-32 mx-auto tracking-[1.5em] text-center p-3 rounded-xl border border-slate-200 text-slate-800 font-mono font-black text-xl bg-slate-50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all block"
                    placeholder="••••"
                    autoFocus
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleConfirmSaveWithPasscode();
                    }}
                  />
                  {passcodeError && (
                    <p className="text-[11px] font-bold text-rose-500 text-center mt-1">
                      {passcodeError}
                    </p>
                  )}
                </div>
              </div>

              <div className="p-4 bg-slate-50 flex gap-2 justify-stretch border-t border-slate-100">
                <button
                  onClick={() => setShowPasscodeModal(false)}
                  className="flex-1 py-2.5 bg-white border border-slate-200 text-xs font-bold text-slate-700 rounded-xl hover:bg-slate-50 transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleConfirmSaveWithPasscode}
                  className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer shadow-md shadow-emerald-600/10"
                >
                  تأكيد الحفظ
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Alarms Settings Modal (Android High-Fidelity details) */}
      <AnimatePresence>
        {showAlarmModal && (
          <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-lg overflow-hidden shadow-2xl border border-slate-100 max-h-[85vh] flex flex-col"
            >
              {/* Header */}
              <div className="p-5 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 bg-amber-50 text-amber-600 rounded-xl flex items-center justify-center border border-amber-100 shadow-sm">
                    <Bell className="w-4.5 h-4.5 text-amber-500 animate-swing" />
                  </div>
                  <h3 className="text-sm font-extrabold text-slate-800">إعدادات التنبيه الذكية</h3>
                </div>
                <button 
                  onClick={() => setShowAlarmModal(false)}
                  className="w-7 h-7 rounded-full bg-slate-200 hover:bg-slate-300 flex items-center justify-center text-slate-500 transition-colors"
                >
                  ✕
                </button>
              </div>

              {/* Scrollable content */}
              <div className="p-6 space-y-6 overflow-y-auto flex-1">
                
                {/* 1. Toggle Alarm Activator */}
                <div className="flex items-center justify-between p-4 bg-amber-50/40 border border-amber-100 rounded-2xl">
                  <div className="space-y-1">
                    <span className="text-xs font-extrabold text-amber-900 block">تفعيل الإشعارات والتنبيهات الموقوتة</span>
                    <span className="text-[10px] text-amber-700/80 font-medium block">
                      سيقوم التطبيق بالرنين صوتياً عند حلول أوقات الحصص ومذاكرة المواد المحددة بالجدول.
                    </span>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer select-none">
                    <input 
                      type="checkbox" 
                      checked={activeLocal.alarmConfig.enabled}
                      onChange={handleToggleAlarmActive}
                      className="sr-only peer" 
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:-translate-x-full rtl:peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600"></div>
                    <span className="mr-2.5 text-xs font-black text-slate-700">تفعيل</span>
                  </label>
                </div>

                {/* 2. Audio Ringtone Selector */}
                <div className="space-y-2">
                  <h4 className="text-xs font-extrabold text-slate-600">إدارة نغمة الرنين</h4>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      onClick={() => {
                        updateActiveLocal(prev => {
                          const copy = { ...prev };
                          copy.alarmConfig.ringtoneName = "افتراضي";
                          return copy;
                        });
                        speakArabicText("نغمة رنين افتراضية");
                      }}
                      className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                        activeLocal.alarmConfig.ringtoneName === "افتراضي"
                          ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-bold'
                          : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <span className="text-xs block">نغمة الهاتف الافتراضية</span>
                    </button>
                    <button
                      onClick={() => {
                        updateActiveLocal(prev => {
                          const copy = { ...prev };
                          copy.alarmConfig.ringtoneName = "نغمة السنتر الهادئة";
                          return copy;
                        });
                        speakArabicText("نغمة مخصصة مفعلة");
                      }}
                      className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                        activeLocal.alarmConfig.ringtoneName !== "افتراضي"
                          ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-bold'
                          : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <span className="text-xs block">نغمة رنين مخصصة</span>
                    </button>
                  </div>
                </div>

                {/* 3. Periods mapped to alarms list */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between pb-1 border-b border-slate-100">
                    <h4 className="text-xs font-extrabold text-slate-600">أوقات الرنين والمزامنة اليومية</h4>
                    <span className="text-[10px] text-slate-400">انقر لتعديل التوقيت</span>
                  </div>

                  <div className="space-y-2.5">
                    {activeLocal.headers.map((header) => {
                      const alarmTime = activeLocal.alarmConfig.times[header] || "12:00";
                      
                      // Gather subject summary of this slot across weekdays
                      const weekdaysWithSubjects = DAYS.map(day => {
                        const subj = activeLocal.grid[day]?.[header] || "";
                        return subj ? `${day}: ${subj}` : null;
                      }).filter(Boolean);

                      const subjectSummaryText = weekdaysWithSubjects.length > 0 
                        ? weekdaysWithSubjects.slice(0, 3).join(" | ") + (weekdaysWithSubjects.length > 3 ? "..." : "")
                        : "لا توجد مواد دراسية مجدولة في هذا العمود";

                      return (
                        <div 
                          key={header}
                          onClick={() => handleOpenTimepicker(header)}
                          className="flex items-center justify-between p-3.5 bg-slate-50 hover:bg-slate-100 border border-slate-150 rounded-2xl cursor-pointer transition-colors"
                        >
                          {/* Right: Alarm time indicator */}
                          <div className="flex items-center gap-2">
                            <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center">
                              <Clock className="w-4 h-4" />
                            </div>
                            <div className="space-y-0.5">
                              <span className="text-xs font-black text-slate-800 block">
                                {header}
                              </span>
                              <span className="text-[10px] text-slate-400 font-medium block">
                                {subjectSummaryText}
                              </span>
                            </div>
                          </div>

                          {/* Left: Beautiful 12-hour display */}
                          <span className="text-xs font-mono font-bold bg-white text-slate-700 px-3 py-1.5 rounded-lg border border-slate-200">
                            {formatTime12(alarmTime)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>

              </div>

              {/* Footer */}
              <div className="p-4 bg-slate-50 border-t border-slate-100 flex gap-2 justify-stretch">
                <button
                  onClick={() => setShowAlarmModal(false)}
                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer text-center"
                >
                  تمّ، إغلاق الإعدادات والعودة للجدول
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Android High-Fidelity Wheel Timepicker Simulator */}
      <AnimatePresence>
        {timepickerTarget && (
          <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-xs overflow-hidden shadow-2xl border border-slate-150"
            >
              <div className="p-4 bg-slate-50 border-b border-slate-100 text-center">
                <h4 className="text-xs font-extrabold text-slate-700">
                  ضبط وقت المنبه لـ: <span className="text-emerald-700">{timepickerTarget}</span>
                </h4>
              </div>

              {/* Spinning rolls simulation */}
              <div className="p-6 flex items-center justify-center gap-6 select-none bg-slate-50/50">
                
                {/* 1. Hour roll */}
                <div className="flex flex-col items-center">
                  <span className="text-[10px] font-bold text-slate-400 mb-1">ساعة</span>
                  <div className="flex flex-col items-center gap-1">
                    <button 
                      onClick={() => setWheelHour(prev => prev === 12 ? 1 : prev + 1)}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronUp className="w-5 h-5" />
                    </button>
                    <span className="text-2xl font-mono font-black text-slate-800 bg-white border border-slate-200 px-3 py-1.5 rounded-xl shadow-xs w-12 text-center">
                      {wheelHour}
                    </span>
                    <button 
                      onClick={() => setWheelHour(prev => prev === 1 ? 12 : prev - 1)}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronDown className="w-5 h-5" />
                    </button>
                  </div>
                </div>

                <span className="text-2xl font-bold text-slate-400 self-center mt-3">:</span>

                {/* 2. Minute roll */}
                <div className="flex flex-col items-center">
                  <span className="text-[10px] font-bold text-slate-400 mb-1">دقيقة</span>
                  <div className="flex flex-col items-center gap-1">
                    <button 
                      onClick={() => setWheelMinute(prev => (prev + 5) % 60)}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronUp className="w-5 h-5" />
                    </button>
                    <span className="text-2xl font-mono font-black text-slate-800 bg-white border border-slate-200 px-3 py-1.5 rounded-xl shadow-xs w-12 text-center">
                      {String(wheelMinute).padStart(2, '0')}
                    </span>
                    <button 
                      onClick={() => setWheelMinute(prev => (prev - 5 + 60) % 60)}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronDown className="w-5 h-5" />
                    </button>
                  </div>
                </div>

                {/* 3. AM/PM roll */}
                <div className="flex flex-col items-center">
                  <span className="text-[10px] font-bold text-slate-400 mb-1">فترة</span>
                  <div className="flex flex-col items-center gap-1">
                    <button 
                      onClick={() => setWheelAmPm(prev => prev === 'AM' ? 'PM' : 'AM')}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronUp className="w-5 h-5" />
                    </button>
                    <span className="text-sm font-bold text-slate-800 bg-white border border-slate-200 px-2.5 py-2.5 rounded-xl shadow-xs w-12 text-center">
                      {wheelAmPm === 'AM' ? 'ص' : 'م'}
                    </span>
                    <button 
                      onClick={() => setWheelAmPm(prev => prev === 'AM' ? 'PM' : 'AM')}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronDown className="w-5 h-5" />
                    </button>
                  </div>
                </div>

              </div>

              {/* Trigger confirm action */}
              <div className="p-3 bg-slate-50 flex gap-2 justify-stretch border-t border-slate-150">
                <button
                  onClick={() => setTimepickerTarget(null)}
                  className="flex-1 py-2 bg-white border border-slate-200 text-xs font-bold text-slate-700 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleConfirmTimepicker}
                  className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer"
                >
                  موافق
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
