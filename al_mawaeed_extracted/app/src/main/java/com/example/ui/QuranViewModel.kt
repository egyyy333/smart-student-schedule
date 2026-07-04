package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuranRepository

    // Database flows turned into StateFlows
    val students: StateFlow<List<StudentEntity>>
    val hourHeaders: StateFlow<List<HourHeaderEntity>>
    val monthHeaders: StateFlow<List<MonthHeaderEntity>>
    val appointmentCells: StateFlow<List<AppointmentCellEntity>>
    val payments: StateFlow<List<PaymentEntity>>

    // Authentication State
    var isAuthenticated by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)

    // Splash / Loading State
    var isInitialized by mutableStateOf(false)
        private set

    // Draft State for Appointments Tab (Mawa'eed)
    var draftHourHeaders by mutableStateOf<Map<Int, String>>(emptyMap())
        private set
    var draftCells by mutableStateOf<Map<Pair<Int, Int>, String>>(emptyMap())
        private set

    // Draft State for Students & Payments Tab (Asma'a)
    var draftMonthHeaders by mutableStateOf<Map<Int, String>>(emptyMap())
        private set
    var draftPayments by mutableStateOf<Map<Pair<Int, Int>, Boolean>>(emptyMap())
        private set
    var draftStudents by mutableStateOf<List<StudentEntity>>(emptyList())
    var deletedStudentIds by mutableStateOf<Set<Int>>(emptySet())
    var tableZoomScale by mutableStateOf(1.0f)

    // Alarm Configuration State
    var alarmEnabled by mutableStateOf(false)
        private set
    var alarmRingtoneUri by mutableStateOf<String?>(null)
        private set
    var alarmTimes by mutableStateOf<Map<Int, String>>(emptyMap())
        private set

    var draftAlarmEnabled by mutableStateOf(false)
    var draftAlarmRingtoneUri by mutableStateOf<String?>(null)
    var draftAlarmTimes by mutableStateOf<Map<Int, String>>(emptyMap())

    // Password Management Fields
    var oldPasswordInput by mutableStateOf("")
    var newPasswordInput by mutableStateOf("")
    var confirmPasswordInput by mutableStateOf("")
    var passwordChangeMessage by mutableStateOf<String?>(null)
    var passwordChangeSuccess by mutableStateOf(false)

    // UI Toast/Snack Message Trigger
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        val database = QuranDatabase.getDatabase(application)
        val dao = database.quranDao()
        repository = QuranRepository(dao)

        students = repository.students.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        hourHeaders = repository.hourHeaders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        monthHeaders = repository.monthHeaders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        appointmentCells = repository.appointmentCells.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        payments = repository.payments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed database and set up drafts
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()

            // Load Alarm Settings
            alarmEnabled = repository.getConfig("alarm_enabled", "false").toBoolean()
            draftAlarmEnabled = alarmEnabled

            alarmRingtoneUri = repository.getConfig("alarm_ringtone_uri", "")
            if (alarmRingtoneUri == "") alarmRingtoneUri = null
            draftAlarmRingtoneUri = alarmRingtoneUri

            val tMap = mutableMapOf<Int, String>()
            for (h in 0..7) {
                val defaultTime = when (h) {
                    0 -> "12:00"
                    1 -> "13:00"
                    2 -> "14:00"
                    3 -> "15:00"
                    4 -> "16:00"
                    5 -> "17:00"
                    6 -> "18:00"
                    7 -> "19:00"
                    else -> "12:00"
                }
                val time = repository.getConfig("alarm_time_$h", defaultTime)
                tMap[h] = time
            }
            alarmTimes = tMap
            draftAlarmTimes = tMap

            isInitialized = true

            // Gather and setup initial drafts from db
            launch {
                hourHeaders.collect { list ->
                    if (draftHourHeaders.isEmpty() && list.isNotEmpty()) {
                        draftHourHeaders = list.associate { it.hourIndex to it.name }
                    }
                }
            }
            launch {
                appointmentCells.collect { list ->
                    if (draftCells.isEmpty() && list.isNotEmpty()) {
                        draftCells = list.associate { (it.dayIndex to it.hourIndex) to it.content }
                        if (alarmEnabled) {
                            AlarmScheduler.scheduleAlarms(
                                getApplication(),
                                alarmEnabled,
                                alarmTimes,
                                alarmRingtoneUri ?: "",
                                list
                            )
                        }
                    }
                }
            }
            launch {
                monthHeaders.collect { list ->
                    if (draftMonthHeaders.isEmpty() && list.isNotEmpty()) {
                        draftMonthHeaders = list.associate { it.monthIndex to it.name }
                    }
                }
            }
            launch {
                payments.collect { list ->
                    if (draftPayments.isEmpty() && list.isNotEmpty()) {
                        draftPayments = list.associate { (it.studentId to it.monthIndex) to it.paid }
                    }
                }
            }
            launch {
                students.collect { list ->
                    if (draftStudents.isEmpty() && list.isNotEmpty()) {
                        draftStudents = list
                    }
                }
            }
        }
    }

    /**
     * Authenticate with entered password
     */
    fun login(password: String) {
        viewModelScope.launch {
            val correctPassword = repository.getPassword()
            if (password == correctPassword) {
                isAuthenticated = true
                authError = null
                // Force sync drafts from database when authenticating
                syncDraftsFromDb()
            } else {
                authError = "كلمة السر غير صحيحة، يرجى المحاولة مرة أخرى"
            }
        }
    }

    fun logout() {
        isAuthenticated = false
    }

    /**
     * Resets/Synchronizes drafts with database states.
     */
    fun syncDraftsFromDb() {
        draftHourHeaders = hourHeaders.value.associate { it.hourIndex to it.name }
        draftCells = appointmentCells.value.associate { (it.dayIndex to it.hourIndex) to it.content }
        draftMonthHeaders = monthHeaders.value.associate { it.monthIndex to it.name }
        draftPayments = payments.value.associate { (it.studentId to it.monthIndex) to it.paid }
        draftStudents = students.value
        deletedStudentIds = emptySet()
    }

    // --- APPOINTMENTS (MAWA'EED) MUTATORS ---

    fun updateDraftHourHeader(hourIndex: Int, newName: String) {
        val updated = draftHourHeaders.toMutableMap()
        updated[hourIndex] = newName
        draftHourHeaders = updated
    }

    fun updateDraftCell(dayIndex: Int, hourIndex: Int, newContent: String) {
        val updated = draftCells.toMutableMap()
        updated[dayIndex to hourIndex] = newContent
        draftCells = updated
    }

    /**
     * COMMITS APPOINTMENT DRAFTS (Hour headers & cells) to DB.
     */
    fun saveAppointments() {
        viewModelScope.launch {
            try {
                // Save hour headers
                val headerEntities = draftHourHeaders.map { (index, name) ->
                    HourHeaderEntity(index, name)
                }
                repository.saveHourHeaders(headerEntities)

                // Save cells
                val cellEntities = draftCells.map { (key, content) ->
                    AppointmentCellEntity(key.first, key.second, content)
                }
                repository.saveAppointmentCells(cellEntities)

                // Commit alarm configuration from draft state
                alarmEnabled = draftAlarmEnabled
                alarmRingtoneUri = draftAlarmRingtoneUri
                alarmTimes = draftAlarmTimes

                repository.saveConfig("alarm_enabled", alarmEnabled.toString())
                repository.saveConfig("alarm_ringtone_uri", alarmRingtoneUri ?: "")
                alarmTimes.forEach { (h, timeStr) ->
                    repository.saveConfig("alarm_time_$h", timeStr)
                }

                // Re-schedule alarms with the new saved configuration and cells
                AlarmScheduler.scheduleAlarms(
                    getApplication(),
                    alarmEnabled,
                    alarmTimes,
                    alarmRingtoneUri ?: "",
                    cellEntities
                )

                // Success toast removed per user request
            } catch (e: Exception) {
                _uiEvent.emit("حدث خطأ أثناء حفظ المواعيد: ${e.localizedMessage}")
            }
        }
    }

    // --- STUDENTS & PAYMENTS (ASMA'A) MUTATORS ---

    fun addStudent(fullName: String) {
        if (fullName.trim().isEmpty()) return
        val newTempId = (draftStudents.minOfOrNull { it.id } ?: 0).let { if (it < 0) it - 1 else -1 }
        val newStudent = StudentEntity(id = newTempId, fullName = fullName.trim(), lastModified = System.currentTimeMillis())
        draftStudents = draftStudents + newStudent
        viewModelScope.launch {
            _uiEvent.emit("تم إضافة الطالب مؤقتاً: ${fullName.trim()}. اضغط تثبيت لحفظ التغييرات!")
        }
    }

    fun deleteStudent(studentId: Int, studentName: String) {
        draftStudents = draftStudents.filter { it.id != studentId }
        
        // Clean from draft payments too
        val updatedPayments = draftPayments.toMutableMap()
        val keysToRemove = updatedPayments.keys.filter { it.first == studentId }
        keysToRemove.forEach { updatedPayments.remove(it) }
        draftPayments = updatedPayments

        if (studentId > 0) {
            deletedStudentIds = deletedStudentIds + studentId
        }
        viewModelScope.launch {
            _uiEvent.emit("تم حذف الطالب $studentName مؤقتاً. اضغط تثبيت لحفظ التغييرات!")
        }
    }

    fun updateStudentName(studentId: Int, newName: String) {
        if (newName.trim().isEmpty()) return
        draftStudents = draftStudents.map {
            if (it.id == studentId) {
                it.copy(fullName = newName.trim(), lastModified = System.currentTimeMillis())
            } else {
                it
            }
        }
        viewModelScope.launch {
            _uiEvent.emit("تم تعديل الاسم مؤقتاً. اضغط تثبيت لحفظ التغييرات!")
        }
    }

    fun updateDraftMonthHeader(monthIndex: Int, newName: String) {
        val updated = draftMonthHeaders.toMutableMap()
        updated[monthIndex] = newName
        draftMonthHeaders = updated
    }

    fun toggleDraftPayment(studentId: Int, monthIndex: Int) {
        val updated = draftPayments.toMutableMap()
        val currentStatus = updated[studentId to monthIndex] ?: false
        updated[studentId to monthIndex] = !currentStatus
        draftPayments = updated
    }

    /**
     * COMMITS NAMES & PAYMENTS DRAFTS to DB.
     */
    fun saveNamesAndPayments() {
        viewModelScope.launch {
            try {
                // 1. Save month headers
                val monthEntities = draftMonthHeaders.map { (index, name) ->
                    MonthHeaderEntity(index, name)
                }
                repository.saveMonthHeaders(monthEntities)

                // 2. Delete removed students
                deletedStudentIds.forEach { id ->
                    repository.deleteStudent(id)
                }

                // 3. Save draft students (and map temp negative IDs to new autogenerated IDs)
                val idMapping = mutableMapOf<Int, Int>()
                draftStudents.forEach { student ->
                    if (student.id < 0) {
                        // New student, save with 0 so Room autogenerates ID
                        val savedId = repository.saveStudent(student.copy(id = 0))
                        idMapping[student.id] = savedId.toInt()
                    } else {
                        // Existing student, update/save directly
                        repository.saveStudent(student)
                    }
                }

                // 4. Map draft payments to real IDs and save
                val paymentEntities = draftPayments.mapNotNull { (key, paid) ->
                    val (tempStudentId, monthIndex) = key
                    // If student was deleted, skip
                    if (tempStudentId in deletedStudentIds) return@mapNotNull null
                    
                    val realStudentId = if (tempStudentId < 0) {
                        idMapping[tempStudentId] ?: return@mapNotNull null
                    } else {
                        tempStudentId
                    }
                    PaymentEntity(realStudentId, monthIndex, paid)
                }
                repository.savePayments(paymentEntities)

                // 5. Update draft state instantly with actual database IDs
                val updatedDraftStudents = draftStudents.map { student ->
                    if (student.id < 0) {
                        student.copy(id = idMapping[student.id] ?: 0)
                    } else {
                        student
                    }
                }.filter { it.id > 0 }
                
                draftStudents = updatedDraftStudents
                deletedStudentIds = emptySet()

                // Update draft payments with real IDs
                val updatedDraftPayments = draftPayments.mapKeys { (key, _) ->
                    val (tempId, monthIndex) = key
                    val realId = if (tempId < 0) idMapping[tempId] ?: tempId else tempId
                    realId to monthIndex
                }.filterKeys { it.first > 0 }
                draftPayments = updatedDraftPayments

                // Success snackbar removed per user request
            } catch (e: Exception) {
                _uiEvent.emit("حدث خطأ أثناء حفظ البيانات: ${e.localizedMessage}")
            }
        }
    }

    // --- PASSWORD MANAGEMENT ---

    fun changePassword() {
        val oldPass = oldPasswordInput
        val newPass = newPasswordInput
        val confirmPass = confirmPasswordInput

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passwordChangeMessage = "يرجى ملء جميع الحقول المطلوبة"
            passwordChangeSuccess = false
            return
        }

        viewModelScope.launch {
            val correctPassword = repository.getPassword()
            if (oldPass != correctPassword) {
                passwordChangeMessage = "كلمة السر القديمة غير صحيحة"
                passwordChangeSuccess = false
                return@launch
            }

            if (newPass != confirmPass) {
                passwordChangeMessage = "كلمة السر الجديدة غير متطابقة"
                passwordChangeSuccess = false
                return@launch
            }

            if (newPass.length < 4) {
                passwordChangeMessage = "كلمة السر الجديدة يجب ألا تقل عن 4 رموز"
                passwordChangeSuccess = false
                return@launch
            }

            // Save new password
            repository.setPassword(newPass)
            passwordChangeMessage = "تم تغيير كلمة السر بنجاح!"
            passwordChangeSuccess = true

            // Clear inputs
            oldPasswordInput = ""
            newPasswordInput = ""
            confirmPasswordInput = ""

            // Success snackbar removed per user request
        }
    }

    fun clearPasswordChangeMessages() {
        passwordChangeMessage = null
        passwordChangeSuccess = false
    }

    /**
     * Verifies the password and saves the draft names and payments.
     */
    fun verifyPassword(
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val correctPassword = repository.getPassword()
            if (password == correctPassword) {
                onSuccess()
            } else {
                onFailure("كلمة السر غير صحيحة، يرجى المحاولة مرة أخرى")
            }
        }
    }

    fun verifyPasswordAndSave(
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val correctPassword = repository.getPassword()
            if (password == correctPassword) {
                saveNamesAndPayments()
                onSuccess()
            } else {
                onFailure("كلمة السر غير صحيحة، يرجى المحاولة مرة أخرى")
            }
        }
    }

    fun revertAppointments() {
        draftHourHeaders = hourHeaders.value.associate { it.hourIndex to it.name }
        draftCells = appointmentCells.value.associate { (it.dayIndex to it.hourIndex) to it.content }
        draftAlarmEnabled = alarmEnabled
        draftAlarmRingtoneUri = alarmRingtoneUri
        draftAlarmTimes = alarmTimes
        // Revert message removed per user request
    }

    fun revertNamesAndPayments() {
        draftMonthHeaders = monthHeaders.value.associate { it.monthIndex to it.name }
        draftPayments = payments.value.associate { (it.studentId to it.monthIndex) to it.paid }
        draftStudents = students.value
        deletedStudentIds = emptySet()
        viewModelScope.launch {
            // Revert message removed per user request
        }
    }

    fun getBackupJsonString(): String {
        val root = org.json.JSONObject()
        root.put("backup_version", 1)

        val studentsArray = org.json.JSONArray()
        students.value.forEach {
            val obj = org.json.JSONObject()
            obj.put("id", it.id)
            obj.put("fullName", it.fullName)
            obj.put("lastModified", it.lastModified)
            studentsArray.put(obj)
        }
        root.put("students", studentsArray)

        val hourHeadersArray = org.json.JSONArray()
        hourHeaders.value.forEach {
            val obj = org.json.JSONObject()
            obj.put("hourIndex", it.hourIndex)
            obj.put("name", it.name)
            hourHeadersArray.put(obj)
        }
        root.put("hour_headers", hourHeadersArray)

        val monthHeadersArray = org.json.JSONArray()
        monthHeaders.value.forEach {
            val obj = org.json.JSONObject()
            obj.put("monthIndex", it.monthIndex)
            obj.put("name", it.name)
            monthHeadersArray.put(obj)
        }
        root.put("month_headers", monthHeadersArray)

        val appointmentCellsArray = org.json.JSONArray()
        appointmentCells.value.forEach {
            val obj = org.json.JSONObject()
            obj.put("dayIndex", it.dayIndex)
            obj.put("hourIndex", it.hourIndex)
            obj.put("content", it.content)
            appointmentCellsArray.put(obj)
        }
        root.put("appointment_cells", appointmentCellsArray)

        val paymentsArray = org.json.JSONArray()
        payments.value.forEach {
            val obj = org.json.JSONObject()
            obj.put("studentId", it.studentId)
            obj.put("monthIndex", it.monthIndex)
            obj.put("paid", it.paid)
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        return root.toString(2)
    }

    fun importBackupJsonString(jsonString: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)
                
                val studentsList = mutableListOf<StudentEntity>()
                if (root.has("students")) {
                    val arr = root.getJSONArray("students")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        studentsList.add(
                            StudentEntity(
                                id = obj.getInt("id"),
                                fullName = obj.getString("fullName"),
                                lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val hourHeadersList = mutableListOf<HourHeaderEntity>()
                if (root.has("hour_headers")) {
                    val arr = root.getJSONArray("hour_headers")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        hourHeadersList.add(
                            HourHeaderEntity(
                                hourIndex = obj.getInt("hourIndex"),
                                name = obj.getString("name")
                            )
                        )
                    }
                }

                val monthHeadersList = mutableListOf<MonthHeaderEntity>()
                if (root.has("month_headers")) {
                    val arr = root.getJSONArray("month_headers")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        monthHeadersList.add(
                            MonthHeaderEntity(
                                monthIndex = obj.getInt("monthIndex"),
                                name = obj.getString("name")
                            )
                        )
                    }
                }

                val appointmentCellsList = mutableListOf<AppointmentCellEntity>()
                if (root.has("appointment_cells")) {
                    val arr = root.getJSONArray("appointment_cells")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        appointmentCellsList.add(
                            AppointmentCellEntity(
                                dayIndex = obj.getInt("dayIndex"),
                                hourIndex = obj.getInt("hourIndex"),
                                content = obj.getString("content")
                            )
                        )
                    }
                }

                val paymentsList = mutableListOf<PaymentEntity>()
                if (root.has("payments")) {
                    val arr = root.getJSONArray("payments")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        paymentsList.add(
                            PaymentEntity(
                                studentId = obj.getInt("studentId"),
                                monthIndex = obj.getInt("monthIndex"),
                                paid = obj.getBoolean("paid")
                            )
                        )
                    }
                }

                repository.importBackup(
                    studentsList,
                    hourHeadersList,
                    appointmentCellsList,
                    monthHeadersList,
                    paymentsList
                )

                // Resync drafts so the UI updates immediately
                syncDraftsFromDb()
                _uiEvent.emit("تم استيراد البيانات بنجاح!")
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "خطأ غير معروف في قراءة ملف النسخة الاحتياطية")
            }
        }
    }

    // --- ALARM MANAGEMENT ---

    fun toggleAlarmEnabled(context: Context, enabled: Boolean) {
        draftAlarmEnabled = enabled
    }

    fun setAlarmRingtone(context: Context, uri: String) {
        draftAlarmRingtoneUri = uri
    }

    fun updateAlarmTime(context: Context, hourIndex: Int, timeStr: String) {
        val updatedMap = draftAlarmTimes.toMutableMap()
        updatedMap[hourIndex] = timeStr
        draftAlarmTimes = updatedMap
    }
}
