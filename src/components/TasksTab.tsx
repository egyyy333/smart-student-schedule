import { useState } from 'react';
import { 
  Plus, CheckSquare, Square, Calendar, AlertCircle, Edit3, Trash2, ArrowUpDown, CheckCircle, Clock 
} from 'lucide-react';
import { Task, AppState } from '../types';
import { motion, AnimatePresence } from 'motion/react';
import { speakArabicText } from '../audioHelper';

interface TasksTabProps {
  state: AppState;
  onSaveState: (newState: AppState) => void;
}

export default function TasksTab({ state, onSaveState }: TasksTabProps) {
  // Filters & sorting states
  const [filter, setFilter] = useState<'all' | 'pending' | 'completed'>('all');
  const [sortBy, setSortBy] = useState<'priority' | 'date'>('priority');

  // Task form modal states
  const [showFormModal, setShowFormModal] = useState<boolean>(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  
  // Form fields
  const [formTitle, setFormTitle] = useState<string>('');
  const [formNotes, setFormNotes] = useState<string>('');
  const [formPriority, setFormPriority] = useState<'high' | 'medium' | 'low'>('medium');
  const [formDueDate, setFormDueDate] = useState<string>(new Date().toISOString().split('T')[0]);

  const handleOpenAdd = () => {
    setEditingTask(null);
    setFormTitle('');
    setFormNotes('');
    setFormPriority('medium');
    setFormDueDate(new Date().toISOString().split('T')[0]);
    setShowFormModal(true);
  };

  const handleOpenEdit = (task: Task) => {
    setEditingTask(task);
    setFormTitle(task.title);
    setFormNotes(task.notes);
    setFormPriority(task.priority);
    setFormDueDate(task.dueDate);
    setShowFormModal(true);
  };

  const handleSaveTask = () => {
    if (!formTitle.trim()) return;

    let updatedTasks = [...state.tasks];

    if (editingTask) {
      // Edit mode
      updatedTasks = updatedTasks.map(t => t.id === editingTask.id ? {
        ...t,
        title: formTitle.trim(),
        notes: formNotes.trim(),
        priority: formPriority,
        dueDate: formDueDate
      } : t);
      speakArabicText("تم تعديل المهمة");
    } else {
      // Add mode
      const newTask: Task = {
        id: String(Date.now()),
        title: formTitle.trim(),
        notes: formNotes.trim(),
        priority: formPriority,
        dueDate: formDueDate,
        completed: false
      };
      updatedTasks.unshift(newTask);
      speakArabicText("تم إضافة مهمة جديدة");
    }

    onSaveState({
      ...state,
      tasks: updatedTasks
    });
    setShowFormModal(false);
  };

  const handleDeleteTask = (id: string) => {
    const updatedTasks = state.tasks.filter(t => t.id !== id);
    onSaveState({
      ...state,
      tasks: updatedTasks
    });
    speakArabicText("تم حذف المهمة");
  };

  const handleToggleComplete = (task: Task) => {
    const isNowCompleted = !task.completed;
    
    // Increment active streak on finishing a task as positive reinforcement!
    let newStreak = state.streakCount;
    if (isNowCompleted) {
      newStreak += 1;
      speakArabicText("أحسنت! عمل رائع بطل");
    }

    const updatedTasks = state.tasks.map(t => t.id === task.id ? { ...t, completed: isNowCompleted } : t);
    onSaveState({
      ...state,
      tasks: updatedTasks,
      streakCount: newStreak
    });
  };

  // Filter tasks
  const filteredTasks = state.tasks.filter(t => {
    if (filter === 'pending') return !t.completed;
    if (filter === 'completed') return t.completed;
    return true;
  });

  // Sort tasks
  const sortedTasks = [...filteredTasks].sort((a, b) => {
    if (sortBy === 'priority') {
      const priorityWeights = { high: 3, medium: 2, low: 1 };
      return priorityWeights[b.priority] - priorityWeights[a.priority];
    } else {
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    }
  });

  // Productivity status numbers
  const completedCount = state.tasks.filter(t => t.completed).length;
  const totalCount = state.tasks.length;
  const completionPercentage = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

  return (
    <div className="space-y-6 font-sans">
      
      {/* 1. Header Information */}
      <div className="text-right space-y-1.5">
        <h1 className="text-xl md:text-2xl font-extrabold text-slate-800">
          قائمة المهام والواجبات المنزلية ✍️
        </h1>
        <p className="text-xs text-slate-400 font-medium leading-relaxed">
          سجل واجباتك ومشاريعك الدراسية ورتبها حسب الأولوية
        </p>
      </div>

      {/* 2. Narrow Styled Add Task Button */}
      {/* Reduced width to 50% horizontally as requested */}
      <div className="flex justify-center">
        <button
          onClick={handleOpenAdd}
          className="w-1/2 min-w-[150px] max-w-xs py-3 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-black text-xs md:text-sm rounded-2xl flex items-center justify-center gap-2 shadow-lg shadow-emerald-600/10 transition-colors cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة مهمة جديدة</span>
        </button>
      </div>

      {/* 3. Gradient Progress Indicator Panel */}
      <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm space-y-3">
        <div className="flex justify-between items-center text-xs font-extrabold">
          <span className="text-slate-700">مستوى إنجاز المهام الكلي</span>
          <span className="text-emerald-600 font-mono">
            {completionPercentage}% (تم إنجاز {completedCount} من {totalCount} مهام)
          </span>
        </div>

        {/* The beautiful gradient bar container */}
        <div className="w-full bg-slate-100 h-3 rounded-full overflow-hidden border border-slate-50">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: `${completionPercentage}%` }}
            transition={{ duration: 0.6, ease: "easeOut" }}
            className="h-full bg-gradient-to-r from-emerald-500 to-teal-500 rounded-full"
          />
        </div>
      </div>

      {/* 4. Filters and Sorting controls */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white border border-slate-100 rounded-2xl p-3.5 shadow-sm">
        
        {/* Left: Filters - High contrast state design */}
        <div className="flex bg-slate-50 p-1 rounded-xl border border-slate-150 gap-1 select-none">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1.5 rounded-lg text-xs font-black transition-colors cursor-pointer ${
              filter === 'all'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-500 hover:text-slate-800 hover:bg-slate-150'
            }`}
          >
            الكل
          </button>
          <button
            onClick={() => setFilter('pending')}
            className={`px-3 py-1.5 rounded-lg text-xs font-black transition-colors cursor-pointer ${
              filter === 'pending'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-500 hover:text-slate-800 hover:bg-slate-150'
            }`}
          >
            قيد الإنجاز
          </button>
          <button
            onClick={() => setFilter('completed')}
            className={`px-3 py-1.5 rounded-lg text-xs font-black transition-colors cursor-pointer ${
              filter === 'completed'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-500 hover:text-slate-800 hover:bg-slate-150'
            }`}
          >
            المكتملة
          </button>
        </div>

        {/* Right: Dropdown Sorting */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 font-bold flex items-center gap-1">
            <ArrowUpDown className="w-3.5 h-3.5 text-slate-400" />
            <span>ترتيب حسب:</span>
          </span>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'priority' | 'date')}
            className="bg-slate-50 border border-slate-150 rounded-xl p-2 text-xs font-extrabold text-slate-700 focus:outline-none focus:border-emerald-500 transition-colors cursor-pointer"
          >
            <option value="priority">مستوى الأهمية ⭐</option>
            <option value="date">تاريخ التسليم 📅</option>
          </select>
        </div>

      </div>

      {/* 5. Tasks Cards Lists */}
      <div className="space-y-3">
        <AnimatePresence mode="popLayout">
          {sortedTasks.length === 0 ? (
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="py-12 bg-white border border-slate-100 rounded-3xl text-center flex flex-col items-center justify-center space-y-2 shadow-sm"
            >
              <div className="w-12 h-12 bg-slate-50 rounded-full flex items-center justify-center border border-slate-100 mb-2">
                <CheckCircle className="w-6 h-6 text-slate-300" />
              </div>
              <h3 className="text-xs font-extrabold text-slate-600">لا توجد أي مهام مطابقة</h3>
              <p className="text-[11px] text-slate-400 font-medium">
                بطل، سجل مهاماً دراسية أو واجبات لتتبعها بكل فخر هنا!
              </p>
            </motion.div>
          ) : (
            sortedTasks.map((task) => {
              // Priority label helper
              const priorityTags = {
                high: { label: 'مهمة جداً', bg: 'bg-rose-50 border-rose-200 text-rose-700' },
                medium: { label: 'متوسطة', bg: 'bg-amber-50 border-amber-200 text-amber-700' },
                low: { label: 'منخفضة', bg: 'bg-slate-50 border-slate-200 text-slate-600' }
              };

              const currentTag = priorityTags[task.priority];

              return (
                <motion.div
                  key={task.id}
                  layoutId={task.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  className={`bg-white border border-slate-150 rounded-2xl p-4 shadow-xs transition-all flex items-start justify-between gap-4 ${
                    task.completed ? 'opacity-65 border-slate-100' : 'hover:border-slate-200'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    {/* Checkbox trigger button */}
                    <button
                      onClick={() => handleToggleComplete(task)}
                      className="mt-1 text-slate-400 hover:text-emerald-600 transition-colors cursor-pointer select-none"
                    >
                      {task.completed ? (
                        <CheckSquare className="w-5.5 h-5.5 text-emerald-600 fill-emerald-50" />
                      ) : (
                        <Square className="w-5.5 h-5.5 text-slate-300" />
                      )}
                    </button>

                    {/* Task Title & details */}
                    <div className="space-y-1">
                      <h3 className={`text-xs font-extrabold text-slate-800 leading-snug ${
                        task.completed ? 'line-through text-slate-400' : ''
                      }`}>
                        {task.title}
                      </h3>
                      {task.notes && (
                        <p className={`text-[11px] text-slate-400 leading-relaxed font-medium ${
                          task.completed ? 'line-through text-slate-300' : ''
                        }`}>
                          {task.notes}
                        </p>
                      )}

                      {/* Small inline meta badges */}
                      <div className="flex flex-wrap gap-2 pt-1.5 items-center">
                        {/* Priority level label */}
                        <span className={`text-[9px] font-black px-2 py-0.5 rounded border ${currentTag.bg}`}>
                          {currentTag.label}
                        </span>

                        {/* Due date */}
                        <span className="text-[9px] font-bold text-slate-400 bg-slate-50 border border-slate-150 px-2 py-0.5 rounded flex items-center gap-1 font-mono">
                          <Calendar className="w-3 h-3 text-slate-400" />
                          <span>{task.dueDate}</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Actions buttons: Edit, Delete */}
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => handleOpenEdit(task)}
                      className="w-8 h-8 rounded-lg bg-slate-50 hover:bg-slate-100 text-slate-600 flex items-center justify-center transition-colors cursor-pointer"
                      title="تعديل المهمة"
                    >
                      <Edit3 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteTask(task.id)}
                      className="w-8 h-8 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-600 flex items-center justify-center transition-colors cursor-pointer"
                      title="حذف المهمة"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </motion.div>
              );
            })
          )}
        </AnimatePresence>
      </div>

      {/* ================= EDIT / ADD FORM DIALOG ================= */}
      <AnimatePresence>
        {showFormModal && (
          <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 font-sans">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl border border-slate-100 max-h-[90vh] flex flex-col"
            >
              <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                <h3 className="text-sm font-extrabold text-slate-800">
                  {editingTask ? '✏️ تعديل تفاصيل المهمة الدراسية' : '✍️ إضافة مهمة دراسية جديدة'}
                </h3>
                <button 
                  onClick={() => setShowFormModal(false)}
                  className="w-7 h-7 rounded-full bg-slate-200 hover:bg-slate-300 flex items-center justify-center text-slate-500 transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="p-5 space-y-4 overflow-y-auto flex-1">
                {/* Title */}
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">عنوان المهمة (مطلوب):</label>
                  <input
                    type="text"
                    required
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    className="w-full p-3 rounded-xl border border-slate-200 text-slate-800 font-bold text-sm bg-slate-50/50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all"
                    placeholder="مثال: حل واجب كتاب الرياضيات صفحة 50"
                    autoFocus
                  />
                </div>

                {/* Notes details */}
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">شرح أو تفاصيل إضافية:</label>
                  <textarea
                    value={formNotes}
                    onChange={(e) => setFormNotes(e.target.value)}
                    rows={3}
                    className="w-full p-3 rounded-xl border border-slate-200 text-slate-800 font-medium text-xs bg-slate-50/50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all resize-none"
                    placeholder="تفاصيل إضافية أو مراجع لمساعدتك أثناء المذاكرة والتحصيل..."
                  />
                </div>

                {/* Priority Level */}
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">مستوى الأهمية والأولوية:</label>
                  <div className="grid grid-cols-3 gap-2">
                    <button
                      type="button"
                      onClick={() => setFormPriority('high')}
                      className={`p-2 rounded-xl text-xs font-black text-center border transition-all cursor-pointer ${
                        formPriority === 'high'
                          ? 'bg-rose-50 border-rose-300 text-rose-700 font-bold shadow-xs'
                          : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      مهمة جداً
                    </button>
                    <button
                      type="button"
                      onClick={() => setFormPriority('medium')}
                      className={`p-2 rounded-xl text-xs font-black text-center border transition-all cursor-pointer ${
                        formPriority === 'medium'
                          ? 'bg-amber-50 border-amber-300 text-amber-700 font-bold shadow-xs'
                          : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      متوسطة
                    </button>
                    <button
                      type="button"
                      onClick={() => setFormPriority('low')}
                      className={`p-2 rounded-xl text-xs font-black text-center border transition-all cursor-pointer ${
                        formPriority === 'low'
                          ? 'bg-slate-50 border-slate-300 text-slate-700 font-bold shadow-xs'
                          : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      منخفضة
                    </button>
                  </div>
                </div>

                {/* Due Date */}
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 block">تاريخ التسليم المطلوب:</label>
                  <input
                    type="date"
                    value={formDueDate}
                    onChange={(e) => setFormDueDate(e.target.value)}
                    className="w-full p-3 rounded-xl border border-slate-200 text-slate-800 font-bold text-sm bg-slate-50/50 focus:border-emerald-500 focus:bg-white focus:outline-none transition-all font-mono"
                  />
                </div>
              </div>

              <div className="p-5 border-t border-slate-50 bg-slate-50/50 flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => setShowFormModal(false)}
                  className="px-4 py-2 text-xs font-bold text-slate-500 hover:bg-slate-200 rounded-xl transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  onClick={handleSaveTask}
                  disabled={!formTitle.trim()}
                  className={`px-5 py-2 text-xs font-extrabold rounded-xl transition-all cursor-pointer ${
                    formTitle.trim()
                      ? 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-600/10'
                      : 'bg-slate-200 text-slate-400 border border-slate-200 cursor-not-allowed'
                  }`}
                >
                  تأكيد المهمة
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
