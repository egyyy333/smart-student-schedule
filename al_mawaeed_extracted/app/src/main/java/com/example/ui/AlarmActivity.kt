package com.example.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LightTeal
import com.example.ui.theme.MediumTeal
import com.example.ui.theme.MyApplicationTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lockscreen and wake screen up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        
        enableEdgeToEdge()

        val appointmentText = intent.getStringExtra("appointment_text") ?: "حصة قرآنية"
        val appointmentDay = intent.getStringExtra("appointment_day") ?: ""
        val appointmentTime = intent.getStringExtra("appointment_time") ?: ""
        val ringtoneUri = intent.getStringExtra("ringtone_uri") ?: ""

        setContent {
            MyApplicationTheme {
                AlarmScreen(
                    text = appointmentText,
                    day = appointmentDay,
                    time = appointmentTime,
                    onStop = {
                        AlarmService.activeService?.stopAlarmService()
                        finish()
                    },
                    onSnooze = {
                        AlarmService.activeService?.snoozeAlarmService(
                            this@AlarmActivity,
                            appointmentText,
                            appointmentDay,
                            appointmentTime,
                            ringtoneUri
                        )
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Clicking back stops the alarm service and closes it
        AlarmService.activeService?.stopAlarmService()
        super.onBackPressed()
    }
}

@Composable
fun AlarmScreen(
    text: String,
    day: String,
    time: String,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val formattedTime = remember(time) {
        try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 12
                val m = parts[1].toIntOrNull() ?: 0
                val amPm = if (h >= 12) "م" else "ص"
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                String.format("%d:%02d %s", h12, m, amPm)
            } else {
                time
            }
        } catch (e: Exception) {
            time
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF031412), // Extremely deep pine night
                        Color(0xFF092C28), // Deep rich pine teal
                        Color(0xFF031412)  // Back to extremely deep pine
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Alarm icon container with pulsing gold aura
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .background(GoldAccent.copy(alpha = 0.25f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(GoldAccent, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = "المنبه",
                        tint = Color(0xFF031412),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "تَنْبِيه مَوْعِد الحِصَّة",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Elegant Card showing detail of slot - styled with high contrast deep background and enlarged text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF051C1A).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.5.dp, GoldAccent)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "يوم $day • الساعة $formattedTime",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            color = Color(0xFFFFD54F), // High-visibility bright yellow-gold
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Dismiss Button
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color(0xFF031412)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = "إِيقَاف التَّنْبِيه",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Button
            Button(
                onClick = onSnooze,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF80CBC4), // Beautiful mint green
                    contentColor = Color(0xFF031412)  // Contrast text color
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = "تَأْجِيل (10 دَقَائِق)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Rights Credit Label
            Text(
                text = "بواسطة الشيخ أحمد النمس غفر الله له",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFFFECB3), // Warm light gold
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
