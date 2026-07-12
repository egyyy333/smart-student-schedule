package com.smartstudentschedule.app

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckSquare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartstudentschedule.app.alarm.AlarmScheduler
import com.smartstudentschedule.app.alarm.AlarmService
import com.smartstudentschedule.app.data.AppRepository
import com.smartstudentschedule.app.data.AppState
import com.smartstudentschedule.app.ui.components.*
import com.smartstudentschedule.app.ui.theme.SmartStudentTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private var appState = mutableStateOf(AppState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Repository
        appRepository = AppRepository(applicationContext)

        // Observe State changes to load the UI dynamically
        lifecycleScope.launch {
            appRepository.appStateFlow.collect { state ->
                appState.value = state
            }
        }

        // Enable edge-to-edge content drawing under system bars (Android 15 styling)
        enableEdgeToEdge()

        // Handle possible lockscreen bypass if intent requests it
        handleBypassFlags(intent)

        setContent {
            SmartStudentTheme {
                val state by appState
                val context = LocalContext.current
                
                // Track Lockscreen unlock state
                var isUnlocked by remember { mutableStateOf(false) }

                // Track Alarm Overlay trigger state
                var activeAlarmSubject by remember { mutableStateOf<String?>(null) }
                var activeAlarmTime by remember { mutableStateOf("") }

                // Sync Intent launches
                LaunchedEffect(intent) {
                    val triggerOverlay = intent.getStringExtra("trigger_alarm_overlay")
                    if ("true" == triggerOverlay) {
                        activeAlarmSubject = intent.getStringExtra("appointment_text") ?: "مذاكرة / حصة"
                        activeAlarmTime = intent.getStringExtra("appointment_time") ?: ""
                    }
                }

                if (activeAlarmSubject != null) {
                    // Modern fullscreen active alarm screen
                    AlarmOverlayScreen(
                        subject = activeAlarmSubject!!,
                        time = activeAlarmTime,
                        onDismiss = {
                            // Turn off current ringing service
                            val serviceIntent = Intent(context, AlarmService::class.java)
                            context.stopService(serviceIntent)
                            activeAlarmSubject = null
                        },
                        onSnooze = {
                            // Stop current sound, start snooze and close overlay
                            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                                action = "ACTION_SNOOZE_ALARM"
                            }
                            context.startService(serviceIntent)
                            activeAlarmSubject = null
                        }
                    )
                } else if (!isUnlocked && state.passcode.isNotBlank()) {
                    // Lock Screen Pin Pad
                    PasscodeScreen(
                        correctPasscode = state.passcode,
                        onSuccess = { isUnlocked = true }
                    )
                } else {
                    // Primary Navigation Scaffold
                    MainAppScaffold(
                        state = state,
                        onSaveState = { newState ->
                            lifecycleScope.launch {
                                appRepository.saveState(newState)
                                AlarmScheduler.syncAlarms(context, newState)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun handleBypassFlags(intent: Intent?) {
        if (intent != null && "true" == intent.getStringExtra("trigger_alarm_overlay")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )

            // Dismiss Keyguard
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.requestDismissKeyguard(this, null)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleBypassFlags(intent)
    }
}

@Composable
fun MainAppScaffold(
    state: AppState,
    onSaveState: (AppState) -> Unit
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("dashboard") }

    val navItems = listOf(
        NavigationItem("dashboard", "الرئيسية", Icons.Default.Home),
        NavigationItem("schedules", "الجداول", Icons.Default.CalendarMonth),
        NavigationItem("tasks", "الواجبات", Icons.Default.CheckSquare),
        NavigationItem("settings", "الإعدادات", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            currentRoute = item.route
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(state = state, onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route)
                })
            }
            composable("schedules") {
                SchedulesScreen(state = state, onSaveState = onSaveState)
            }
            composable("tasks") {
                TasksScreen(state = state, onSaveState = onSaveState)
            }
            composable("settings") {
                SettingsScreen(state = state, onSaveState = onSaveState)
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
