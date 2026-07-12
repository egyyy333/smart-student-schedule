package com.smartstudentschedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudentschedule.app.data.AppState
import com.smartstudentschedule.app.data.TaskItem
import java.util.UUID

@Composable
fun TasksScreen(
    state: AppState,
    onSaveState: (AppState) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") } // low, medium, high

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Task Input Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("إضافة واجب أو مهمة جديدة 📝", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("عنوان الواجب أو المهمة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الأهمية:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        listOf("low" to "عادي", "medium" to "متوسط", "high" to "عاجل").forEach { (valStr, label) ->
                            FilterChip(
                                selected = priority == valStr,
                                onClick = { priority = valStr },
                                label = { Text(label) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val newTask = TaskItem(
                                    id = UUID.randomUUID().toString(),
                                    title = newTaskTitle,
                                    priority = priority,
                                    completed = false
                                )
                                onSaveState(state.copy(tasks = listOf(newTask) + state.tasks))
                                newTaskTitle = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة")
                    }
                }
            }
        }

        // List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قائمة الواجبات والمهام الدراسيّة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${state.tasks.filter { !it.completed }.size} مهمة معلقة", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        // Tasks list
        if (state.tasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text("لا توجد مهام حالياً. أضف واجباً جديداً للبدء!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.tasks) { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = { isChecked ->
                                        val updatedTasks = state.tasks.map {
                                            if (it.id == task.id) it.copy(completed = isChecked) else it
                                        }
                                        onSaveState(state.copy(tasks = updatedTasks))
                                    }
                                )

                                Column {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.completed) Color.Gray else MaterialTheme.colorScheme.onBackground
                                    )

                                    // Custom priority tag
                                    val (color, text) = when (task.priority) {
                                        "high" -> Color(0xFFEF4444) to "عاجل"
                                        "low" -> Color(0xFF10B981) to "عادي"
                                        else -> Color(0xFFF59E0B) to "متوسط"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    onSaveState(state.copy(tasks = state.tasks.filter { it.id != task.id }))
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "مسح", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
