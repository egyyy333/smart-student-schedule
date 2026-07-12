package com.smartstudentschedule.app.data

data class AppState(
    val passcode: String = "1234",
    val studentName: String = "",
    val enableNameCall: Boolean = true,
    val streakCount: Int = 0,
    val lastActiveDate: String = "",
    val isAlarmActive: Boolean = true,
    val schoolSchedule: Schedule = Schedule(
        headers = listOf("الأولى", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة", "السابعة"),
        grid = createInitialGrid(listOf("الأولى", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة", "السابعة"))
    ),
    val tutoringSchedule: Schedule = Schedule(
        headers = listOf("م١", "م٢", "م٣", "م٤"),
        grid = createInitialGrid(listOf("م١", "م٢", "م٣", "م٤"))
    ),
    val studySchedule: Schedule = Schedule(
        headers = listOf("الفترة ١", "الفترة ٢", "الفترة ٣"),
        grid = createInitialGrid(listOf("الفترة ١", "الفترة ٢", "الفترة ٣"))
    ),
    val tasks: List<TaskItem> = emptyList()
)

data class Schedule(
    val headers: List<String> = emptyList(),
    val grid: Map<String, Map<String, String>> = emptyMap() // Day -> (Header -> Subject)
)

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val priority: String = "medium", // low, medium, high
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun createInitialGrid(headers: List<String>): Map<String, Map<String, String>> {
    val days = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val grid = mutableMapOf<String, Map<String, String>>()
    for (day in days) {
        val dayMap = mutableMapOf<String, String>()
        for (header in headers) {
            dayMap[header] = ""
        }
        grid[day] = dayMap
    }
    return grid
}
