package com.smartstudentschedule.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudentschedule.app.data.AppState

@Composable
fun SettingsScreen(
    state: AppState,
    onSaveState: (AppState) -> Unit
) {
    val scrollState = rememberScrollState()

    var studentName by remember { mutableStateOf(state.studentName) }
    var enableNameCall by remember { mutableStateOf(state.enableNameCall) }
    var isAlarmActive by remember { mutableStateOf(state.isAlarmActive) }
    var passcode by remember { mutableStateOf(state.passcode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Personal Information
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("الملف الشخصي للطالب 👤", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("اسم الطالب ثلاثي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مناداة الطالب باسمه", style = MaterialTheme.typography.bodyLarge)
                        Text("التطبيق سيناديك باسمك أثناء التنبيهات والترحيب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = enableNameCall,
                        onCheckedChange = { enableNameCall = it }
                    )
                }
            }
        }

        // Section: Alarm Control Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("إعدادات المنبه والتنبيهات ⏰", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تشغيل المنبه التلقائي", style = MaterialTheme.typography.bodyLarge)
                        Text("إيقاف المنبه بالكامل لجميع الفترات والحصص", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = isAlarmActive,
                        onCheckedChange = { isAlarmActive = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = passcode,
                    onValueChange = { if (it.length <= 4) passcode = it },
                    label = { Text("رمز مرور حماية الجدول (٤ أرقام)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Actions
        Button(
            onClick = {
                onSaveState(
                    state.copy(
                        studentName = studentName,
                        enableNameCall = enableNameCall,
                        isAlarmActive = isAlarmActive,
                        passcode = passcode.ifBlank { "1234" }
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ جميع الإعدادات")
        }

        // Factory Reset Control
        OutlinedButton(
            onClick = {
                onSaveState(AppState())
                studentName = ""
                enableNameCall = true
                isAlarmActive = true
                passcode = "1234"
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حذف كافة البيانات والبدء من جديد")
        }
    }
}
