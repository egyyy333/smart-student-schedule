package com.smartstudentschedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudentschedule.app.data.AppState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    state: AppState,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    // Retrieve today's day name in Arabic
    val sdf = SimpleDateFormat("EEEE", Locale("ar"))
    val arabicDayName = sdf.format(Date())

    val schoolToday = state.schoolSchedule.grid[arabicDayName] ?: emptyMap()
    val tutoringToday = state.tutoringSchedule.grid[arabicDayName] ?: emptyMap()
    val studyToday = state.studySchedule.grid[arabicDayName] ?: emptyMap()

    val timelineItems = mutableListOf<Triple<String, String, String>>() // (Time, Subject, Type)
    
    schoolToday.forEach { (k, v) -> if (v.isNotBlank()) timelineItems.add(Triple(k, v, "مدرسة")) }
    tutoringToday.forEach { (k, v) -> if (v.isNotBlank()) timelineItems.add(Triple(k, v, "درس خصوصي")) }
    studyToday.forEach { (k, v) -> if (v.isNotBlank()) timelineItems.add(Triple(k, v, "مذاكرة منزلية")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dynamic Card Banner
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "لوحة التحكم اليومية 🗓️",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Text("${state.streakCount} يوم نشاط متتالي", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quran verse center
                    Text(
                        "( رَبِّ زِدْنِي عِلْمًا )",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    val nameLabel = if (state.studentName.isNotBlank() && state.enableNameCall) state.studentName else "يا بطل"
                    Text(
                        "أهلاً بك $nameLabel 👋",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        "اليوم هو $arabicDayName، ولديك اليوم ${timelineItems.size} فترات ومواعيد مسجلة.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Today's Timeline list header
        Text(
            text = "جدولك لليوم ($arabicDayName) ⏰",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (timelineItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "لا يوجد حصص أو فترات مذاكرة مسجلة لليوم!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "استمتع بوقتك أو أضف مواعيد جديدة من علامة تبويب الجداول",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                timelineItems.forEach { (time, subject, type) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when (type) {
                                        "مدرسة" -> MaterialTheme.colorScheme.primaryContainer
                                        "درس خصوصي" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color(0xFFFEF3C7)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (type) {
                                                "مدرسة" -> Icons.Default.MenuBook
                                                "درس خصوصي" -> Icons.Default.Event
                                                else -> Icons.Default.Bookmark
                                            },
                                            contentDescription = null,
                                            tint = when (type) {
                                                "مدرسة" -> MaterialTheme.colorScheme.primary
                                                "درس خصوصي" -> MaterialTheme.colorScheme.secondary
                                                else -> Color(0xFFD97706)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(type, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }

                            Text(
                                time,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
