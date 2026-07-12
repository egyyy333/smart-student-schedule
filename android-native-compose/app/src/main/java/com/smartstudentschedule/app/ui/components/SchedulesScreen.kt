package com.smartstudentschedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudentschedule.app.data.AppState
import com.smartstudentschedule.app.data.Schedule

@Composable
fun SchedulesScreen(
    state: AppState,
    onSaveState: (AppState) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("الدراسة المدرسية", "الدروس الخصوصية", "المذاكرة المنزلية")

    val days = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

    val currentSchedule = when (selectedTab) {
        0 -> state.schoolSchedule
        1 -> state.tutoringSchedule
        else -> state.studySchedule
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingDay by remember { mutableStateOf("") }
    var editingHeader by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        val vertScroll = rememberScrollState()
        val horizScroll = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(vertScroll)
                .horizontalScroll(horizScroll)
                .padding(16.dp)
        ) {
            Column {
                // Table Headers Row
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    // Day column header space
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(50.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("اليوم", fontWeight = FontWeight.Black)
                    }

                    // Schedule period headers
                    currentSchedule.headers.forEach { header ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(50.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(header, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Table Rows
                days.forEach { day ->
                    Row {
                        // Day name label cell
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day, fontWeight = FontWeight.Bold)
                        }

                        // Grid cell values
                        currentSchedule.headers.forEach { header ->
                            val value = currentSchedule.grid[day]?.get(header) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(56.dp)
                                    .clickable {
                                        editingDay = day
                                        editingHeader = header
                                        editingValue = value
                                        showEditDialog = true
                                    }
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (value.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value.ifBlank { "—" },
                                    color = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("تعديل الحصة / المذاكرة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت تعدل الحصة لليوم: $editingDay - الفترة: $editingHeader")
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = { editingValue = it },
                        label = { Text("اسم المادة / الحصة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gridCopy = currentSchedule.grid.toMutableMap()
                        val dayCopy = (gridCopy[editingDay] ?: emptyMap()).toMutableMap()
                        dayCopy[editingHeader] = editingValue
                        gridCopy[editingDay] = dayCopy

                        val updatedSchedule = currentSchedule.copy(grid = gridCopy)
                        val newState = when (selectedTab) {
                            0 -> state.copy(schoolSchedule = updatedSchedule)
                            1 -> state.copy(tutoringSchedule = updatedSchedule)
                            else -> state.copy(studySchedule = updatedSchedule)
                        }

                        onSaveState(newState)
                        showEditDialog = false
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
