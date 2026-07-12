package com.smartstudentschedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasscodeScreen(
    correctPasscode: String,
    onSuccess: () -> Unit
) {
    var codeState by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "تأمين جدول الطالب الذكي",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "برجاء كتابة رمز المرور لمتابعة يومك بنجاح",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            for (i in 0 until 4) {
                val filled = i < codeState.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) Color.Red
                            else if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        // Numerical Keyboard Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("clear", "0", "delete")
            )

            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (key in row) {
                        if (key.isBlank()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (key == "clear" || key == "delete") Color.Transparent
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "clear" -> codeState = ""
                                            "delete" -> {
                                                if (codeState.isNotEmpty()) {
                                                    codeState = codeState.substring(0, codeState.length - 1)
                                                }
                                            }
                                            else -> {
                                                if (codeState.length < 4) {
                                                    codeState += key
                                                    if (codeState.length == 4) {
                                                        if (codeState == correctPasscode) {
                                                            onSuccess()
                                                        } else {
                                                            isError = true
                                                            codeState = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                when (key) {
                                    "clear" -> Text("إعادة", color = Color.Red, fontSize = 14.sp)
                                    "delete" -> Icon(Icons.Default.Delete, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onBackground)
                                    else -> Text(key, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
