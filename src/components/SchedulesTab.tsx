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
  
  // Local scratchpad states to hold unsaved schedule modifications with sessionStorage caching to persist across tabs
  const [localSchool, setLocalSchool] = useState<ScheduleData>(() => {
    const cached = sessionStorage.getItem('unsaved_school_schedule');
    return cached ? JSON.parse(cached) : JSON.parse(JSON.stringify(state.schoolSchedule));
  });
  const [localTutoring, setLocalTutoring] = useState<ScheduleData>(() => {
    const cached = sessionStorage.getItem('unsaved_tutoring_schedule');
    return cached ? JSON.parse(cached) : JSON.parse(JSON.stringify(state.tutoringSchedule));
  });
  const [localStudy, setLocalStudy] = useState<ScheduleData>(() => {
    const cached = sessionStorage.getItem('unsaved_study_schedule');
    return cached ? JSON.parse(cached) : JSON.parse(JSON.stringify(state.studySchedule));
  });

  // Sync with main state ONLY if there are no unsaved changes cached in sessionStorage (e.g. on initial loading or backup restore)
  useEffect(() => {
    if (!sessionStorage.getItem('unsaved_school_schedule')) {
      setLocalSchool(JSON.parse(JSON.stringify(state.schoolSchedule)));
    }
    if (!sessionStorage.getItem('unsaved_tutoring_schedule')) {
      setLocalTutoring(JSON.parse(JSON.stringify(state.tutoringSchedule)));
    }
    if (!sessionStorage.getItem('unsaved_study_schedule')) {
      setLocalStudy(JSON.parse(JSON.stringify(state.studySchedule)));
    }
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
    if (activeTab === 'school') {
      setLocalSchool(prev => {
        const next = updater(prev);
        sessionStorage.setItem('unsaved_school_schedule', JSON.stringify(next));
        return next;
      });
    } else if (activeTab === 'tutoring') {
      setLocalTutoring(prev => {
        const next = updater(prev);
        sessionStorage.setItem('unsaved_tutoring_schedule', JSON.stringify(next));
        return next;
      });
    } else {
      setLocalStudy(prev => {
        const next = updater(prev);
        sessionStorage.setItem('unsaved_study_schedule', JSON.stringify(next));
        return next;
      });
    }
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
  const [hasOverlayPermission, setHasOverlayPermission] = useState<boolean>(true);

  useEffect(() => {
    if (showAlarmModal) {
      checkNativeOverlayPermission();
    }
  }, [showAlarmModal]);

  const checkNativeOverlayPermission = async () => {
    try {
      const { AlarmPlugin } = await import('../utils/alarmSync');
      if (AlarmPlugin && typeof AlarmPlugin.checkOverlayPermission === 'function') {
        const res = await AlarmPlugin.checkOverlayPermission();
        setHasOverlayPermission(!!res.hasPermission);
      }
    } catch (err) {
      console.warn("Could not check overlay permission natively:", err);
    }
  };

  const handleRequestOverlayPermission = async () => {
    try {
      const { AlarmPlugin } = await import('../utils/alarmSync');
      if (AlarmPlugin && typeof AlarmPlugin.requestOverlayPermission === 'function') {
        await AlarmPlugin.requestOverlayPermission();
        // Wait and re-check shortly after returning
        setTimeout(checkNativeOverlayPermission, 3000);
      }
    } catch (err) {
      console.warn("Could not request overlay permission natively:", err);
    }
  };

  const [timepickerTarget, setTimepickerTarget] = useState<string | null>(null); // column header
  const [wheelHour, setWheelHour] = useState<number>(4);
  const [wheelMinute, setWheelMinute] = useState<number>(0);
  const [wheelAmPm, setWheelAmPm] = useState<'AM' | 'PM'>('PM');

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleCustomRingtoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = async (event) => {
        const base64Data = event.target?.result as string;
        if (base64Data) {
          localStorage.setItem('custom_ringtone_base64', base64Data);
          try {
            const { AlarmPlugin } = await import('../utils/alarmSync');
            if (AlarmPlugin && typeof AlarmPlugin.saveCustomRingtone === 'function') {
              await AlarmPlugin.saveCustomRingtone({ base64Data });
            }
          } catch (err) {
            console.warn("Native saveCustomRingtone not available:", err);
          }
        }
      };
      reader.readAsDataURL(file);

      updateActiveLocal(prev => {
        const copy = { ...prev };
        copy.alarmConfig.ringtoneName = file.name;
        return copy;
      });
      speakArabicText("تم اختيار وحفظ نغمة مخصصة بنجاح");
    }
  };

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

  // Undo changes and clear sessionStorage cache for the specific tab
  const handleUndo = () => {
    if (activeTab === 'school') {
      sessionStorage.removeItem('unsaved_school_schedule');
      setLocalSchool(JSON.parse(JSON.stringify(state.schoolSchedule)));
    } else if (activeTab === 'tutoring') {
      sessionStorage.removeItem('unsaved_tutoring_schedule');
      setLocalTutoring(JSON.parse(JSON.stringify(state.tutoringSchedule)));
    } else {
      sessionStorage.removeItem('unsaved_study_schedule');
      setLocalStudy(JSON.parse(JSON.stringify(state.studySchedule)));
    }
  };

  // Save changes directly (passcode protection removed for convenience as requested)
  const handleTriggerSave = () => {
    const newState = { ...state };
    if (activeTab === 'school') {
      newState.schoolSchedule = localSchool;
      sessionStorage.removeItem('unsaved_school_schedule');
    } else if (activeTab === 'tutoring') {
      newState.tutoringSchedule = localTutoring;
      sessionStorage.removeItem('unsaved_tutoring_schedule');
    } else {
      newState.studySchedule = localStudy;
      sessionStorage.removeItem('unsaved_study_schedule');
    }
    onSaveState(newState);
    speakArabicText("تم حفظ الجدول بنجاح");
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
    const base = "p-1 h-16 min-w-[84px] w-[84px] max-w-[84px] border-b border-l border-slate-100 text-center relative group cursor-pointer transition-all text-[10px] font-semibold select-none align-middle ";
    const focusRing = "";
    
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
    <div className="space-y-1.5 font-sans">
      
      {/* 1. Unified Sticky Header Block (Tabs + Compact Control Toolbar) */}
      <div className="sticky top-[59px] md:top-[67px] z-40 bg-slate-50 pt-1.5 pb-2.5 space-y-2.5 max-w-xl mx-auto w-full select-none">
        
        {/* Switching Tabs - Slightly Larger and spacious vertically */}
        <div className="flex bg-white p-1 rounded-2xl border border-slate-200 shadow-xs">
          <button
            onClick={() => setActiveTab('school')}
            className={`flex-1 py-2 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
              activeTab === 'school' 
                ? 'bg-emerald-600 text-white shadow-xs' 
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            جدول المدرسة 🏫
          </button>
          <button
            onClick={() => setActiveTab('tutoring')}
            className={`flex-1 py-2 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
              activeTab === 'tutoring' 
                ? 'bg-emerald-600 text-white shadow-xs' 
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            جدول الدروس 🎓
          </button>
          <button
            onClick={() => setActiveTab('study')}
            className={`flex-1 py-2 text-xs md:text-sm font-extrabold rounded-xl transition-all cursor-pointer ${
              activeTab === 'study' 
                ? 'bg-emerald-600 text-white shadow-xs' 
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            جدول المذاكرة ✍️
          </button>
        </div>

        {/* 2. Control Toolbar - Enlarged vertically, wider zoom controls, and beautifully spaced */}
        <div className="grid grid-cols-3 items-center gap-3 bg-white border border-slate-200 rounded-2xl p-2 px-3 shadow-xs">
          
          {/* Column 1: Alarm config (Left) */}
          <div className="flex justify-start">
            {(activeTab === 'school' || activeTab === 'tutoring' || activeTab === 'study') && (
              <button
                onClick={() => setShowAlarmModal(true)}
                className={`px-3 py-1.5 rounded-xl flex items-center gap-1.5 text-xs font-extrabold transition-all cursor-pointer whitespace-nowrap ${
                  activeLocal.alarmConfig.enabled 
                    ? 'bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 shadow-xs' 
                    : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border border-slate-200'
                }`}
              >
                <Bell className={`w-4 h-4 ${activeLocal.alarmConfig.enabled ? 'text-amber-500 animate-swing' : ''}`} />
                <span>إعدادات</span>
              </button>
            )}
          </div>

          {/* Column 2: Zoom Controls (Centered perfectly, widened horizontally) */}
          <div className="flex justify-center">
            <div className="flex items-center gap-1 bg-slate-50 border border-slate-200 rounded-xl p-1 font-sans text-[10px]">
              <button
                onClick={() => handleZoom('out')}
                className="w-8 h-7 bg-white hover:bg-slate-100 border border-slate-150 rounded-lg flex items-center justify-center text-slate-700 font-extrabold transition-colors cursor-pointer text-xs"
                title="تصغير"
              >
                -
              </button>
              <span className="px-1.5 font-bold text-slate-700 min-w-[32px] text-center select-none text-xs">
                %{activeLocal.zoomLevel || 100}
              </span>
              <button
                onClick={() => handleZoom('in')}
                className="w-8 h-7 bg-white hover:bg-slate-100 border border-slate-150 rounded-lg flex items-center justify-center text-slate-700 font-extrabold transition-colors cursor-pointer text-xs"
                title="تكبير"
              >
                +
              </button>
            </div>
          </div>

          {/* Column 3: Clear Grid (Right) */}
          <div className="flex justify-end">
            <button
              onClick={() => setShowClearConfirm(true)}
              className="w-8 h-7 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 rounded-lg flex items-center justify-center transition-colors cursor-pointer"
              title="مسح كامل الجدول"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          </div>

        </div>

      </div>

      {/* 3. The Responsive Grid Table with Sticky Headers and Sticky Day Labels */}
      <div className="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-sm relative z-10">
        
        {/* Horizontal & Vertical Self-Contained Scroll Container to prevent alignment glitches (increased max-h to display all days) */}
        <div id="schedule-scroll-viewport" className="overflow-x-auto overflow-y-auto max-h-[580px] w-full relative">
          <table 
            className="min-w-max w-full border-separate border-spacing-0 text-sm origin-top-right transition-all duration-150" 
            dir="rtl"
            style={{ zoom: `${activeLocal.zoomLevel || 100}%` }}
          >
            {/* Header: Emerald themed */}
            <thead>
              <tr className="bg-emerald-700 text-white">
                {/* Sticky top-right cell (both horizontal and vertical sticky relative to the card container) */}
                <th className="p-1 text-[10px] font-black border-l border-b border-emerald-800 text-center sticky right-0 top-0 z-25 bg-emerald-700 min-w-[60px] w-[60px] max-w-[60px] h-14 shadow-sm select-none">
                  اليوم
                </th>
                {activeLocal.headers.map((header, idx) => (
                  <th 
                    key={idx} 
                    onClick={() => handleOpenEditHeader(idx, header)}
                    className="p-1 text-[9px] font-black tracking-tight border-l border-b border-emerald-800 text-center cursor-pointer hover:bg-emerald-800/80 transition-colors select-none group min-w-[84px] w-[84px] max-w-[84px] h-14 sticky top-0 z-15 bg-emerald-700 shadow-sm"
                    title="انقر لتعديل اسم الفترة/الوقت"
                  >
                    <div className="flex flex-col items-center justify-center h-full w-full leading-tight text-center px-0.5 gap-0.5">
                      <span className="whitespace-pre-line line-clamp-2 font-black text-[10px]">{header}</span>
                      <span className="text-[9px] text-emerald-300 font-bold opacity-0 group-hover:opacity-100 transition-opacity">✏️</span>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
 
            {/* Grid Body */}
            <tbody>
              {DAYS.map((day) => (
                <tr key={day} className="hover:bg-slate-50/30">
                  {/* Day Label column: Sticky on the right */}
                  <td className="p-1 text-[10px] font-extrabold text-slate-800 bg-slate-100 border-l border-b border-slate-200 text-center sticky right-0 z-10 select-none min-w-[60px] w-[60px] max-w-[60px] h-16 shadow-xs align-middle">
                    <div className="flex items-center justify-center w-full h-full font-bold leading-tight">
                      {day}
                    </div>
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
                        <div className="flex flex-col items-center justify-center w-full h-full min-h-[48px] relative">
                          {value ? (
                            <div className="flex flex-col items-center justify-center w-full px-1 leading-tight text-center break-words max-h-full overflow-hidden">
                              <span className="font-extrabold text-[10px] leading-tight line-clamp-2">{value}</span>
                              
                              {/* Small clear "X" button on hover */}
                              <button
                                onClick={(e) => handleClearCellSingle(day, header, e)}
                                className="absolute top-0.5 left-0.5 w-3.5 h-3.5 bg-slate-200/90 hover:bg-rose-500 hover:text-white rounded-full flex items-center justify-center text-[8px] opacity-0 group-hover:opacity-100 transition-opacity"
                                title="تفريغ الخلية"
                              >
                                ✕
                              </button>
                            </div>
                          ) : (
                            <span className="text-sm font-bold text-emerald-600/30 group-hover:text-emerald-600 transition-colors select-none">
                              +
                            </span>
                          )}
                        </div>
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
                  </div>
                  <div className="flex items-center gap-3 select-none">
                    <span className="text-xs font-black text-slate-700">تفعيل</span>
                    <div 
                      onClick={handleToggleAlarmActive}
                      className={`relative w-14 h-8 rounded-full p-1 cursor-pointer transition-colors duration-300 flex items-center shadow-inner ${
                        activeLocal.alarmConfig.enabled ? 'bg-emerald-600 justify-start' : 'bg-slate-300 justify-end'
                      }`}
                    >
                      <motion.div
                        layout
                        className="w-6 h-6 bg-white rounded-full shadow-lg border border-slate-100"
                        transition={{
                          type: "spring",
                          stiffness: 700,
                          damping: 35
                        }}
                      />
                    </div>
                  </div>
                </div>

                <AnimatePresence initial={false}>
                  {activeLocal.alarmConfig.enabled && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      transition={{ duration: 0.3 }}
                      className="space-y-6 overflow-hidden"
                    >
                      {/* 1.5. Overlay Permission Request */}
                      <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl flex flex-col gap-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="space-y-1">
                            <span className="text-xs font-extrabold text-slate-800 block">صلاحية الظهور فوق التطبيقات وشاشة القفل 🛡️</span>
                            <span className="text-[10px] text-slate-500 block leading-relaxed">
                              هذه الصلاحية ضرورية جداً ليتمكن المنبه من إيقاظ الشاشة وعرض صفحة الرنين بالكامل عند قفل الهاتف أو غلق التطبيق.
                            </span>
                          </div>
                          {hasOverlayPermission ? (
                            <span className="shrink-0 px-2 py-0.5 bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-full text-[9px] font-black leading-none">
                              مفعّلة ✓
                            </span>
                          ) : (
                            <span className="shrink-0 px-2 py-0.5 bg-rose-100 text-rose-800 border border-rose-200 rounded-full text-[9px] font-black leading-none">
                              غير مفعّلة !
                            </span>
                          )}
                        </div>
                        
                        {!hasOverlayPermission && (
                          <button
                            onClick={handleRequestOverlayPermission}
                            className="w-full py-2 bg-amber-600 hover:bg-amber-500 active:bg-amber-700 text-white text-xs font-black rounded-xl transition-all shadow-xs cursor-pointer"
                          >
                            تفعيل صلاحية الظهور فوق التطبيقات الآن ⚙️
                          </button>
                        )}
                        
                        <button
                          onClick={checkNativeOverlayPermission}
                          className="text-[9px] text-slate-400 hover:text-slate-600 underline text-right font-medium self-end"
                        >
                          إعادة التحقق من حالة الصلاحية 🔄
                        </button>
                      </div>

                      {/* 1.8. Test Alarm Trigger */}
                      <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-2xl flex items-center justify-between gap-4">
                        <div className="space-y-1">
                          <span className="text-xs font-extrabold text-emerald-900 block">اختبار وفحص منبه التطبيق 🔔</span>
                        </div>
                        <button
                          onClick={() => {
                            // Trigger custom window event which is handled in App.tsx
                            const testEvent = new CustomEvent("alarmTriggered", {
                              detail: {
                                subject: "حصة تجريبية لاختبار المنبه الذكي ⏰",
                                time: "الوقت الحالي للرنين",
                                scheduleType: activeTab,
                                isTest: true
                              }
                            });
                            window.dispatchEvent(testEvent);
                            setShowAlarmModal(false); // Hide settings modal to let them see the full-screen overlay!
                            speakArabicText("بدء اختبار المنبه التجريبي");
                          }}
                          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black rounded-xl shadow-md transition-all shrink-0 cursor-pointer"
                        >
                          تجربة رنين المنبه
                        </button>
                      </div>

                      {/* 2. Audio Ringtone Selector */}
                      <div className="space-y-2">
                        <h4 className="text-xs font-extrabold text-slate-600">إدارة نغمة الرنين</h4>
                        <div className="grid grid-cols-2 gap-3">
                          <button
                            onClick={async () => {
                              localStorage.removeItem('custom_ringtone_base64');
                              try {
                                const { AlarmPlugin } = await import('../utils/alarmSync');
                                if (AlarmPlugin && typeof AlarmPlugin.deleteCustomRingtone === 'function') {
                                  await AlarmPlugin.deleteCustomRingtone();
                                }
                              } catch (err) {
                                console.warn("Native deleteCustomRingtone not available:", err);
                              }
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
                              fileInputRef.current?.click();
                            }}
                            className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                              activeLocal.alarmConfig.ringtoneName !== "افتراضي"
                                ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-bold'
                                : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                            }`}
                          >
                            <span className="text-xs block">نغمة رنين مخصصة</span>
                            {activeLocal.alarmConfig.ringtoneName !== "افتراضي" && (
                              <span className="text-[9px] text-emerald-600 font-mono mt-1 block truncate max-w-[150px]">
                                {activeLocal.alarmConfig.ringtoneName}
                              </span>
                            )}
                          </button>
                        </div>
                        
                        {/* Hidden File Input for Device Ringtone Storage access */}
                        <input 
                          type="file" 
                          ref={fileInputRef} 
                          onChange={handleCustomRingtoneChange} 
                          accept="audio/*" 
                          className="hidden" 
                        />
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
                            
                            // Get today's Arabic name to show subject for current day only
                            const todayArabic = (() => {
                              const dayIndex = new Date().getDay();
                              const map: Record<number, string> = {
                                0: "الأحد",
                                1: "الاثنين",
                                2: "الثلاثاء",
                                3: "الأربعاء",
                                4: "الخميس",
                                5: "الجمعة",
                                6: "السبت"
                              };
                              return map[dayIndex] || "الأحد";
                            })();

                            const todaySubject = activeLocal.grid[todayArabic]?.[header] || "";
                            const subjectSummaryText = todaySubject 
                              ? `${todayArabic}: ${todaySubject}`
                              : `لا توجد مواد مجدولة اليوم (${todayArabic})`;

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
                    </motion.div>
                  )}
                </AnimatePresence>

              </div>

              {/* Footer */}
              <div className="p-4 bg-slate-50 border-t border-slate-100 flex gap-2 justify-stretch">
                <button
                  onClick={() => setShowAlarmModal(false)}
                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold rounded-xl transition-colors cursor-pointer text-center"
                >
                  غلق
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
                      onClick={() => setWheelMinute(prev => (prev + 1) % 60)}
                      className="p-1 text-slate-400 hover:text-slate-800 hover:bg-slate-200 rounded-full"
                    >
                      <ChevronUp className="w-5 h-5" />
                    </button>
                    <span className="text-2xl font-mono font-black text-slate-800 bg-white border border-slate-200 px-3 py-1.5 rounded-xl shadow-xs w-12 text-center">
                      {String(wheelMinute).padStart(2, '0')}
                    </span>
                    <button 
                      onClick={() => setWheelMinute(prev => (prev - 1 + 60) % 60)}
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
