package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.R
import com.example.data.StudentEntity
import com.example.ui.theme.*
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.MediumTeal
import com.example.ui.theme.LightTeal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.DeepGold
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.LightGreenSuccess
import com.example.ui.theme.MintGreen80
import com.example.ui.theme.DarkTeal80
import com.example.ui.theme.GoldAccent80
import com.example.ui.theme.DeepPineNight
import com.example.ui.theme.PineSurface
import com.example.ui.theme.SlateDarkBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.LightText
import com.example.ui.theme.LightTextSecondary
import kotlinx.coroutines.flow.collectLatest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Intent

enum class StudentSortType {
    ALPHA_ASC,
    ALPHA_DESC,
    DATE_DESC,
    DATE_ASC
}

// Arabic days of the week starting Saturday
val WEEK_DAYS = listOf(
    "السبت",
    "الأحد",
    "الإثنين",
    "الثلاثاء",
    "الأربعاء",
    "الخميس",
    "الجمعة"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAppUi(viewModel: QuranViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to ViewModel events for Toasts/Snackbars
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!viewModel.isInitialized) {
                // Splash / Seeding State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (!viewModel.isAuthenticated) {
                // Authenticate Screen
                LoginScreen(viewModel = viewModel)
            } else {
                // Primary App Interface
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Modern Login Screen with spiritual/Islamic styling
 */
@Composable
fun LoginScreen(viewModel: QuranViewModel) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        LightTeal.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quran golden emblem
            Card(
                modifier = Modifier
                    .size(130.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else DarkTeal)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_quran_logo),
                    contentDescription = "شعار المواعيد",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "بَرْنَامَج المَوَاعِيد",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "لتنظيم مواعيد حلقات القرآن الكريم والاشتراكات",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else MediumTeal,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Password Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تسجيل الدخول الآمن",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("أدخل كلمة المرور") },
                        placeholder = { Text("الافتراضية: 1234") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            cursorColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "رؤية كلمة السر",
                                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "قفل",
                                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal
                            )
                        }
                    )

                    viewModel.authError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.login(passwordInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Login,
                            contentDescription = "دخول",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "دخول",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Copyright Credit Footer
            Text(
                text = "بواسطة الشيخ أحمد النمس",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DeepGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else LightTeal.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Main application interface with tab navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: QuranViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf(
        "جدول المواعيد" to Icons.Filled.CalendarMonth,
        "شؤون الطلاب" to Icons.Filled.People,
        "كلمة السر" to Icons.Filled.Lock
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "تأكيد تسجيل الخروج",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في تسجيل الخروج من البرنامج؟",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    border = BorderStroke(1.dp, if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else MediumTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_quran_logo),
                        contentDescription = "شعار",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .padding(2.dp)
                    )
                }
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationRailItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            selectedTextColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            indicatorColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else LightTeal,
                            unselectedIconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MediumTeal.copy(alpha = 0.7f),
                            unselectedTextColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MediumTeal.copy(alpha = 0.7f)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = "خروج",
                        tint = Color.Red
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> AppointmentsTab(viewModel = viewModel)
                        1 -> StudentsTab(viewModel = viewModel)
                        2 -> PasswordTab(viewModel = viewModel)
                    }
                }

                // Dedicated Bottom Copyright Credit
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else LightTeal.copy(alpha = 0.4f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بواسطة الشيخ أحمد النمس غفر الله له",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Header - Centered to avoid any empty spaces or offset alignment
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_quran_logo),
                            contentDescription = "شعار",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "برنامج المواعيد",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else DarkTeal
                ),
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "خروج",
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                        )
                    }
                }
            )

            // Contents Based on Selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> AppointmentsTab(viewModel = viewModel)
                    1 -> StudentsTab(viewModel = viewModel)
                    2 -> PasswordTab(viewModel = viewModel)
                }
            }

            // Dedicated Bottom Copyright Credit & App Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column {
                    // Mini Credit Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else LightTeal.copy(alpha = 0.4f))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بواسطة الشيخ أحمد النمس غفر الله له",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEachIndexed { index, (label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    selectedTextColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    indicatorColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else LightTeal,
                                    unselectedIconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MediumTeal.copy(alpha = 0.7f),
                                    unselectedTextColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MediumTeal.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. APPOINTMENTS TAB (MAWA'EED)
// ==========================================

@Composable
fun AppointmentsTab(viewModel: QuranViewModel) {
    val draftHourHeaders = viewModel.draftHourHeaders
    val draftCells = viewModel.draftCells
    val scale = viewModel.tableZoomScale

    // State for editing dialogs
    var editingHeaderIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingHeaderCurrentValue by rememberSaveable { mutableStateOf("") }

    var editingCellDayIdx by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingCellHourIdx by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingCellCurrentValue by rememberSaveable { mutableStateOf("") }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // Check if drafts differ from actual DB values to show a helpful unsaved hint
    val dbHours = viewModel.hourHeaders.collectAsState().value
    val dbCells = viewModel.appointmentCells.collectAsState().value

    val hasUnsavedChanges = remember(
        draftHourHeaders,
        draftCells,
        dbHours,
        dbCells,
        viewModel.draftAlarmEnabled,
        viewModel.alarmEnabled,
        viewModel.draftAlarmRingtoneUri,
        viewModel.alarmRingtoneUri,
        viewModel.draftAlarmTimes,
        viewModel.alarmTimes
    ) {
        val dbHoursMap = dbHours.associate { it.hourIndex to it.name }
        val dbCellsMap = dbCells.associate { (it.dayIndex to it.hourIndex) to it.content }
        draftHourHeaders != dbHoursMap ||
                draftCells != dbCellsMap ||
                viewModel.draftAlarmEnabled != viewModel.alarmEnabled ||
                viewModel.draftAlarmRingtoneUri != viewModel.alarmRingtoneUri ||
                viewModel.draftAlarmTimes != viewModel.alarmTimes
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instructions / Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else LightTeal.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "تنبيه",
                        tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اضغط على الساعات في الأعلى لتعديل توقيتها، واضغط على أي مربع لتعديل المجموعات أو الطلاب. عند الانتهاء اضغط زر التثبيت.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSystemInDarkTheme()) LightText else DarkTeal,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ZoomControls(viewModel = viewModel)

            // Outer Schedule Layout (RTL-Native structure) with Sticky Headers & Days Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Top section: Top right Corner cell + Hour Headers (scrollable horizontally in sync)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Top right corner cell (Fixed, doesn't scroll)
                    Box(
                        modifier = Modifier
                            .size(width = (85 * scale).dp, height = (55 * scale).dp)
                            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else DarkTeal)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اليوم",
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scale).sp
                        )
                    }

                    // Hour Headers Row (Horizontally scrollable only)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        for (hourIdx in 0..7) {
                            val hourLabel = draftHourHeaders[hourIdx] ?: "${hourIdx + 12}"
                            Box(
                                modifier = Modifier
                                    .width((135 * scale).dp)
                                    .height((55 * scale).dp)
                                    .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondaryContainer else MediumTeal)
                                    .clickable {
                                        editingHeaderIndex = hourIdx
                                        editingHeaderCurrentValue = hourLabel
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = hourLabel,
                                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (13 * scale).sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "تعديل الساعة",
                                        tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.tertiary else GoldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom section: Days Column + Content Grid (scroll synchronized)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    // Days Column (Vertically scrollable only)
                    Column(
                        modifier = Modifier
                            .width((85 * scale).dp)
                            .verticalScroll(verticalScrollState)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        WEEK_DAYS.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier
                                    .size(width = (85 * scale).dp, height = (70 * scale).dp)
                                    .background(
                                        if (isSystemInDarkTheme()) {
                                            if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                        } else {
                                            if (index % 2 == 0) LightTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal,
                                    fontSize = (14 * scale).sp
                                )
                            }
                        }
                    }

                    // Content cells (Both Vertically and Horizontally scrollable in sync)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(verticalScrollState)
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            for (hourIdx in 0..7) {
                                Column(
                                    modifier = Modifier
                                        .width((135 * scale).dp)
                                ) {
                                    for (dayIdx in 0..6) {
                                        val cellContent = draftCells[dayIdx to hourIdx] ?: ""
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((70 * scale).dp)
                                                .background(
                                                    if (isSystemInDarkTheme()) {
                                                        if (cellContent.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                        else if (dayIdx % 2 == 0) MaterialTheme.colorScheme.surface
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                    } else {
                                                        if (cellContent.isNotEmpty()) LightTeal.copy(alpha = 0.4f)
                                                        else if (dayIdx % 2 == 0) MaterialTheme.colorScheme.surface
                                                        else LightTeal.copy(alpha = 0.1f)
                                                    }
                                                )
                                                .clickable {
                                                    editingCellDayIdx = dayIdx
                                                    editingCellHourIdx = hourIdx
                                                    editingCellCurrentValue = cellContent
                                                }
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cellContent.isNotEmpty()) {
                                                Text(
                                                    text = cellContent,
                                                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (12 * scale).sp,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "+",
                                                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MediumTeal.copy(alpha = 0.4f),
                                                    fontSize = (16 * scale).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save / Save Changes Indicator Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Draft status text without enclosing badge box, keeping standard text and red/green colors
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (hasUnsavedChanges) Color(0xFFEF5350) else GreenSuccess, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasUnsavedChanges) "التعديلات غير مثبتة" else "التعديلات مثبتة",
                        color = if (hasUnsavedChanges) Color(0xFFEF5350) else GreenSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                 // Save / Revert buttons
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     if (hasUnsavedChanges) {
                         val undoColor = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFD32F2F)
                         OutlinedButton(
                             onClick = { viewModel.revertAppointments() },
                             colors = ButtonDefaults.outlinedButtonColors(
                                 contentColor = undoColor
                             ),
                             border = BorderStroke(1.dp, undoColor),
                             shape = RoundedCornerShape(10.dp),
                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                             modifier = Modifier.height(38.dp)
                         ) {
                             Icon(Icons.Filled.Undo, contentDescription = "تراجع", modifier = Modifier.size(16.dp))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text("تراجع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                         }
                     }

                     Button(
                         onClick = { viewModel.saveAppointments() },
                         colors = ButtonDefaults.buttonColors(
                             containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                             contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                         ),
                         shape = RoundedCornerShape(10.dp),
                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                         modifier = Modifier.height(38.dp)
                     ) {
                         Icon(Icons.Filled.Save, contentDescription = "تثبيت", modifier = Modifier.size(16.dp))
                         Spacer(modifier = Modifier.width(4.dp))
                         Text("تثبيت", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                     }
                 }
            }
        }

        // --- DIALOGS FOR EDITING ---

        // 1. Edit Header (Hour Label) Dialog
        editingHeaderIndex?.let { index ->
            Dialog(
                onDismissRequest = { editingHeaderIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(16.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعديل تسمية الساعة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editingHeaderCurrentValue,
                            onValueChange = { editingHeaderCurrentValue = it },
                            label = { Text("اسم الساعة / التوقيت") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { editingHeaderIndex = null }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    viewModel.updateDraftHourHeader(index, editingHeaderCurrentValue)
                                    editingHeaderIndex = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            ) {
                                Text("حفظ مؤقت")
                            }
                        }
                    }
                }
            }
        }

        // 2. Edit Cell Content Dialog
        if (editingCellDayIdx != null && editingCellHourIdx != null) {
            val dayIdx = editingCellDayIdx!!
            val hourIdx = editingCellHourIdx!!
            val dayName = WEEK_DAYS[dayIdx]
            val hourName = draftHourHeaders[hourIdx] ?: "${hourIdx + 12}"

            Dialog(
                onDismissRequest = {
                    editingCellDayIdx = null
                    editingCellHourIdx = null
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(16.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعديل المجموعات والحلقات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Text(
                            text = "اليوم: $dayName | الساعة: $hourName",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MediumTeal),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editingCellCurrentValue,
                            onValueChange = { editingCellCurrentValue = it },
                            label = { Text("مجموعة التحفيظ / اسم الطالب") },
                            placeholder = { Text("مثال: مجموعة المائدة، حلقة الحفظ") },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Clear option
                            TextButton(
                                onClick = {
                                    editingCellCurrentValue = ""
                                },
                                colors = ButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.Red,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = Color.LightGray
                                )
                            ) {
                                Text("مسح الخانة")
                            }

                            Row {
                                TextButton(onClick = {
                                    editingCellDayIdx = null
                                    editingCellHourIdx = null
                                }) {
                                    Text("إلغاء")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateDraftCell(dayIdx, hourIdx, editingCellCurrentValue)
                                        editingCellDayIdx = null
                                        editingCellHourIdx = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                        contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                ) {
                                    Text("حفظ مؤقت")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. STUDENTS & PAYMENTS TAB (ASMA'A)
// ==========================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsTab(viewModel: QuranViewModel) {
    val studentList by viewModel.students.collectAsState()
    val draftMonthHeaders = viewModel.draftMonthHeaders
    val draftPayments = viewModel.draftPayments
    val draftStudents = viewModel.draftStudents
    val scale = viewModel.tableZoomScale

    // Search and Sort states
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortType by rememberSaveable { mutableStateOf(StudentSortType.ALPHA_ASC) }

    // Filtered and sorted student list
    val filteredAndSortedStudents = remember(draftStudents, searchQuery, sortType) {
        draftStudents
            .filter { student ->
                student.fullName.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith { s1, s2 ->
                when (sortType) {
                    StudentSortType.ALPHA_ASC -> s1.fullName.compareTo(s2.fullName)
                    StudentSortType.ALPHA_DESC -> s2.fullName.compareTo(s1.fullName)
                    StudentSortType.DATE_DESC -> s2.lastModified.compareTo(s1.lastModified)
                    StudentSortType.DATE_ASC -> s1.lastModified.compareTo(s2.lastModified)
                }
            }
    }

    // Edit/Add dialog states
    var isAddingStudent by rememberSaveable { mutableStateOf(false) }
    var addStudentInput by rememberSaveable { mutableStateOf("") }

    var editingStudentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val editingStudentEntity = remember(editingStudentId, draftStudents) {
        draftStudents.find { it.id == editingStudentId }
    }
    var editingStudentInput by rememberSaveable { mutableStateOf("") }

    var editingMonthIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingMonthCurrentValue by rememberSaveable { mutableStateOf("") }

    // Password verification states for saving payments
    var isConfirmingPaymentsWithPassword by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // Check if drafts differ from DB to show warning
    val dbMonths = viewModel.monthHeaders.collectAsState().value
    val dbPayments = viewModel.payments.collectAsState().value

    val hasUnsavedChanges = remember(draftMonthHeaders, draftPayments, dbMonths, dbPayments, studentList, viewModel.draftStudents, viewModel.deletedStudentIds) {
        val dbMonthsMap = dbMonths.associate { it.monthIndex to it.name }
        val dbPaymentsMap = dbPayments.associate { (it.studentId to it.monthIndex) to it.paid }
        draftMonthHeaders != dbMonthsMap || 
                draftPayments != dbPaymentsMap || 
                viewModel.draftStudents != studentList || 
                viewModel.deletedStudentIds.isNotEmpty()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search and Sort Row (Requirement 7 & 8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Instant Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    placeholder = { 
                        Text(
                            "البحث الفوري عن طالب...", 
                            fontSize = 13.sp,
                            color = if (isSystemInDarkTheme()) LightTextSecondary else MediumTeal.copy(alpha = 0.7f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "بحث",
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else MediumTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "مسح",
                                    tint = if (isSystemInDarkTheme()) LightTextSecondary else MediumTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                        unfocusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else MediumTeal.copy(alpha = 0.3f),
                        focusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White,
                        unfocusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else Color.White
                    )
                )

                // Sort Dropdown Box
                var sortMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else LightTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else MediumTeal.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "فرز",
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("الاسم (أ-ي)", fontSize = 13.sp, fontWeight = if (sortType == StudentSortType.ALPHA_ASC) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                sortType = StudentSortType.ALPHA_ASC
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortType == StudentSortType.ALPHA_ASC) Icons.Filled.Check else Icons.Filled.SortByAlpha,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الاسم (ي-أ)", fontSize = 13.sp, fontWeight = if (sortType == StudentSortType.ALPHA_DESC) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                sortType = StudentSortType.ALPHA_DESC
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortType == StudentSortType.ALPHA_DESC) Icons.Filled.Check else Icons.Filled.SortByAlpha,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الأحدث تعديلاً", fontSize = 13.sp, fontWeight = if (sortType == StudentSortType.DATE_DESC) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                sortType = StudentSortType.DATE_DESC
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortType == StudentSortType.DATE_DESC) Icons.Filled.Check else Icons.Filled.History,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الأقدم تعديلاً", fontSize = 13.sp, fontWeight = if (sortType == StudentSortType.DATE_ASC) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                sortType = StudentSortType.DATE_ASC
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortType == StudentSortType.DATE_ASC) Icons.Filled.Check else Icons.Filled.History,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (draftStudents.isEmpty()) {
                // Empty State illustration / helper (No students registered yet)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = "لا يوجد طلاب",
                            modifier = Modifier.size(80.dp),
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MediumTeal.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا يوجد طلاب مسجلين حالياً",
                            color = if (isSystemInDarkTheme()) LightText else DarkTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "انقر على زر 'إضافة طالب جديد' لبدء التدوين",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSystemInDarkTheme()) LightTextSecondary else MediumTeal.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else if (filteredAndSortedStudents.isEmpty()) {
                // No search results found state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "لا توجد نتائج",
                            modifier = Modifier.size(80.dp),
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MediumTeal.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد نتائج بحث مطابقة",
                            color = if (isSystemInDarkTheme()) LightText else DarkTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تأكد من كتابة الاسم بشكل صحيح أو جرب كلمة أخرى",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSystemInDarkTheme()) LightTextSecondary else MediumTeal.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                ZoomControls(viewModel = viewModel, showAlarmButton = false)

                // Unified bidirectional scroll wrapper with Sticky Headers & Student Names Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Top section: Corner cell + Month Headers (horizontally scrollable in sync)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        // Header student corner (Fixed, doesn't scroll) - Increased 20% horizontally & scaled
                        Box(
                            modifier = Modifier
                                .size(width = (158 * scale).dp, height = (40 * scale).dp)
                                .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else DarkTeal)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "اسم الطالب ثلاثي",
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (13 * scale).sp
                            )
                        }

                        // Month Headers row (Horizontally scrollable only)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            for (monthIdx in 0..5) {
                                val monthLabel = draftMonthHeaders[monthIdx] ?: "الشهر ${monthIdx + 1}"
                                Box(
                                    modifier = Modifier
                                        .width((75 * scale).dp)
                                        .height((40 * scale).dp)
                                        .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondaryContainer else MediumTeal)
                                        .clickable {
                                            editingMonthIndex = monthIdx
                                            editingMonthCurrentValue = monthLabel
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = monthLabel,
                                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (13 * scale).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "تعديل الشهر",
                                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else GoldAccent,
                                            modifier = Modifier.size((11 * scale).dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom section: Student Names Column + Content Grid (scroll synchronized)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        // Student Names Column (Vertically scrollable only) - Increased 20% horizontally & scaled
                        Column(
                            modifier = Modifier
                                .width((158 * scale).dp)
                                .verticalScroll(verticalScrollState)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            filteredAndSortedStudents.forEachIndexed { sIdx, student ->
                                Box(
                                    modifier = Modifier
                                        .size(width = (158 * scale).dp, height = (56 * scale).dp)
                                        .background(
                                            if (sIdx % 2 == 0) {
                                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else LightTeal.copy(alpha = 0.2f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                editingStudentId = student.id
                                                editingStudentInput = student.fullName
                                            },
                                            onLongClick = {
                                                editingStudentId = student.id
                                                editingStudentInput = student.fullName
                                            }
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = student.fullName,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSystemInDarkTheme()) LightText else DarkTeal,
                                            fontSize = (13 * scale).sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "تعديل",
                                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MediumTeal.copy(alpha = 0.5f),
                                            modifier = Modifier.size((14 * scale).dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Content cells (Both Vertically and Horizontally scrollable in sync)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(verticalScrollState)
                        ) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                for (monthIdx in 0..5) {
                                    Column(
                                        modifier = Modifier.width((75 * scale).dp)
                                    ) {
                                        filteredAndSortedStudents.forEachIndexed { sIdx, student ->
                                            val isPaid = draftPayments[student.id to monthIdx] ?: false
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height((56 * scale).dp)
                                                    .background(
                                                        if (sIdx % 2 == 0) {
                                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else LightTeal.copy(alpha = 0.1f)
                                                        } else {
                                                            MaterialTheme.colorScheme.surface
                                                        }
                                                    )
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.toggleDraftPayment(student.id, monthIdx) },
                                                    modifier = Modifier
                                                        .size(width = (30 * scale).dp, height = (26 * scale).dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isPaid) {
                                                            GreenSuccess
                                                        } else {
                                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else LightTeal.copy(alpha = 0.5f)
                                                        }
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isPaid) {
                                                            GreenSuccess
                                                        } else {
                                                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else MediumTeal.copy(alpha = 0.5f)
                                                        },
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                ) {
                                                    if (isPaid) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "تم الدفع",
                                                            tint = Color.White,
                                                            modifier = Modifier.size((16 * scale).dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Save, Revert, and Add Student Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side: + Add button & Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Small compact "+ إضافة" button
                    Button(
                        onClick = { isAddingStudent = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة طالب", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Status indicator without enclosing badge box, keeping standard text and red/green colors
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (hasUnsavedChanges) Color(0xFFEF5350) else GreenSuccess, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasUnsavedChanges) "التعديلات غير مثبتة" else "التعديلات مثبتة",
                            color = if (hasUnsavedChanges) Color(0xFFEF5350) else GreenSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Left side: Revert & Commit buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasUnsavedChanges) {
                        val undoColor = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFD32F2F)
                        OutlinedButton(
                            onClick = { viewModel.revertNamesAndPayments() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = undoColor
                            ),
                            border = BorderStroke(1.dp, undoColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "تراجع", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تراجع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            confirmPasswordInput = ""
                            confirmPasswordError = null
                            isConfirmingPaymentsWithPassword = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "تثبيت", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تثبيت", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- DIALOGS FOR TAB 2 ---

        // 1. Add Student Dialog
        if (isAddingStudent) {
            Dialog(
                onDismissRequest = { isAddingStudent = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(16.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إضافة طالب جديد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = addStudentInput,
                            onValueChange = { addStudentInput = it },
                            label = { Text("اسم الطالب ثلاثي") },
                            placeholder = { Text("مثال: عبد الله أحمد محمد") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                isAddingStudent = false
                                addStudentInput = ""
                            }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    if (addStudentInput.trim().isNotEmpty()) {
                                        viewModel.addStudent(addStudentInput)
                                        isAddingStudent = false
                                        addStudentInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            ) {
                                Text("إضافة")
                            }
                        }
                    }
                }
            }
        }

        // 2. Edit Student Dialog
        editingStudentEntity?.let { student ->
            Dialog(onDismissRequest = { editingStudentId = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعديل بيانات الطالب",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editingStudentInput,
                            onValueChange = { editingStudentInput = it },
                            label = { Text("اسم الطالب ثلاثي") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Delete student
                            IconButton(
                                onClick = {
                                    viewModel.deleteStudent(student.id, student.fullName)
                                    editingStudentId = null
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "حذف الطالب",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Row {
                                TextButton(onClick = { editingStudentId = null }) {
                                    Text("إلغاء")
                                }
                                Button(
                                    onClick = {
                                        if (editingStudentInput.trim().isNotEmpty()) {
                                            viewModel.updateStudentName(student.id, editingStudentInput)
                                            editingStudentId = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                        contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                ) {
                                    Text("حفظ")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Edit Month Header Dialog
        editingMonthIndex?.let { index ->
            Dialog(onDismissRequest = { editingMonthIndex = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعديل اسم العمود المالي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editingMonthCurrentValue,
                            onValueChange = { editingMonthCurrentValue = it },
                            label = { Text("اسم الشهر أو البند") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { editingMonthIndex = null }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    viewModel.updateDraftMonthHeader(index, editingMonthCurrentValue)
                                    editingMonthIndex = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            ) {
                                Text("تعديل مؤقت")
                            }
                        }
                    }
                }
            }
        }

        // 4. Confirm Payments with Password Dialog
        if (isConfirmingPaymentsWithPassword) {
            Dialog(
                onDismissRequest = { isConfirmingPaymentsWithPassword = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(16.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "قفل الأمان",
                            tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تأكيد كلمة المرور للتثبيت",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "يرجى إدخال كلمة سر البرنامج لتثبيت تغييرات الحسابات والمدفوعات ومنع التلاعب.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { 
                                confirmPasswordInput = it
                                confirmPasswordError = null
                            },
                            label = { Text("كلمة السر") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = confirmPasswordError != null,
                            supportingText = {
                                if (confirmPasswordError != null) {
                                    Text(
                                        text = confirmPasswordError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                isConfirmingPaymentsWithPassword = false 
                                confirmPasswordInput = ""
                                confirmPasswordError = null
                            }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    if (confirmPasswordInput.isEmpty()) {
                                        confirmPasswordError = "يرجى إدخال كلمة السر أولاً"
                                    } else {
                                        viewModel.verifyPasswordAndSave(
                                            password = confirmPasswordInput,
                                            onSuccess = {
                                                isConfirmingPaymentsWithPassword = false
                                                confirmPasswordInput = ""
                                                confirmPasswordError = null
                                            },
                                            onFailure = { errorMsg ->
                                                confirmPasswordError = errorMsg
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            ) {
                                Text("تأكيد وحفظ")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PASSWORD MANAGEMENT TAB
// ==========================================

@Composable
fun PasswordTab(viewModel: QuranViewModel) {
    val context = LocalContext.current

    // States for backup and restore password protection
    var pendingBackupAction by remember { mutableStateOf<String?>(null) } // "EXPORT" or "IMPORT"
    var confirmPasswordInputBackup by remember { mutableStateOf("") }
    var confirmPasswordErrorBackup by remember { mutableStateOf<String?>(null) }

    // Launchers for Backup and Restore
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = viewModel.getBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في حفظ النسخة الاحتياطية: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val jsonString = String(bytes, Charsets.UTF_8)
                    viewModel.importBackupJsonString(
                        jsonString = jsonString,
                        onSuccess = {
                            Toast.makeText(context, "تم استعادة النسخة الاحتياطية بنجاح!", Toast.LENGTH_LONG).show()
                        },
                        onFailure = { errorMsg ->
                            Toast.makeText(context, "خطأ في استعادة النسخة الاحتياطية: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في فتح الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Password Confirmation Dialog for Export / Import Protection
    if (pendingBackupAction != null) {
        var isVerifyingBackup by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                pendingBackupAction = null
                confirmPasswordInputBackup = ""
                confirmPasswordErrorBackup = null
            },
            title = {
                Text(
                    text = if (pendingBackupAction == "EXPORT") "تأكيد كلمة المرور للتصدير" else "تأكيد كلمة المرور للاستيراد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "يرجى إدخال كلمة سر البرنامج للتأكيد ومتابعة العملية:",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPasswordInputBackup,
                        onValueChange = {
                            confirmPasswordInputBackup = it
                            confirmPasswordErrorBackup = null
                        },
                        label = { Text("كلمة المرور", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "كلمة المرور",
                                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    if (confirmPasswordErrorBackup != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = confirmPasswordErrorBackup ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isVerifyingBackup) return@Button
                        isVerifyingBackup = true
                        viewModel.verifyPassword(
                            password = confirmPasswordInputBackup,
                            onSuccess = {
                                isVerifyingBackup = false
                                val action = pendingBackupAction
                                pendingBackupAction = null
                                confirmPasswordInputBackup = ""
                                confirmPasswordErrorBackup = null
                                if (action == "EXPORT") {
                                    createBackupLauncher.launch("quran_app_backup_${System.currentTimeMillis() / 1000}.json")
                                } else if (action == "IMPORT") {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                }
                            },
                            onFailure = { errorMsg ->
                                isVerifyingBackup = false
                                confirmPasswordErrorBackup = errorMsg
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingBackupAction = null
                        confirmPasswordInputBackup = ""
                        confirmPasswordErrorBackup = null
                    },
                    border = BorderStroke(1.dp, if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else MediumTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }

    // Clear alerts when this tab is loaded
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPasswordChangeMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        LightTeal.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = "إدارة الأمان",
                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.tertiary else DeepGold,
                modifier = Modifier.size(45.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "إدارة كلمة المرور وحماية التطبيق",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "يرجى الاحتفاظ بكلمة المرور الجديدة لتتمكن من فتح التطبيق لاحقاً بأمان",
                style = MaterialTheme.typography.bodySmall.copy(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MediumTeal),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Old Password Input
                    OutlinedTextField(
                        value = viewModel.oldPasswordInput,
                        onValueChange = { viewModel.oldPasswordInput = it },
                        label = { Text("كلمة المرور القديمة الحالية", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = "الحالية", tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal, modifier = Modifier.size(18.dp))
                        }
                    )

                    Spacer(modifier = Modifier.height(11.dp))

                    // New Password Input
                    OutlinedTextField(
                        value = viewModel.newPasswordInput,
                        onValueChange = { viewModel.newPasswordInput = it },
                        label = { Text("كلمة المرور الجديدة", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = "الجديدة", tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal, modifier = Modifier.size(18.dp))
                        }
                    )

                    Spacer(modifier = Modifier.height(11.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = viewModel.confirmPasswordInput,
                        onValueChange = { viewModel.confirmPasswordInput = it },
                        label = { Text("تأكيد كلمة المرور الجديدة", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            focusedLabelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = "تأكيد الجديدة", tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.secondary else MediumTeal, modifier = Modifier.size(18.dp))
                        }
                    )

                    // Message alerts if any
                    viewModel.passwordChangeMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            color = if (viewModel.passwordChangeSuccess) GreenSuccess else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save (Tathbeet) button for password change
                    Button(
                        onClick = { viewModel.changePassword() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "تثبيت كلمة السر الجديدة",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تثبيت كلمة السر الجديدة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup and Restore Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Backup,
                        contentDescription = "النسخ الاحتياطي",
                        tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "النسخ الاحتياطي والاستعادة",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                        )
                    )

                    Text(
                        text = "احفظ نسخة من جدول المواعيد والأسماء والمدفوعات على هاتفك لاستعادتها في أي وقت عند حذف البرنامج أو تهيئة الجهاز.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Backup button (Export)
                        Button(
                            onClick = {
                                pendingBackupAction = "EXPORT"
                                confirmPasswordInputBackup = ""
                                confirmPasswordErrorBackup = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                                contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = "تصدير", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Restore button (Import)
                        OutlinedButton(
                            onClick = {
                                pendingBackupAction = "IMPORT"
                                confirmPasswordInputBackup = ""
                                confirmPasswordErrorBackup = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSystemInDarkTheme()) Color.White else DarkTeal
                            ),
                            border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color.White else DarkTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = "استيراد", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomControls(viewModel: QuranViewModel, showAlarmButton: Boolean = true) {
    var showAlarmSettings by rememberSaveable { mutableStateOf(false) }

    if (showAlarmSettings && showAlarmButton) {
        AlarmSettingsDialog(viewModel = viewModel, onDismiss = { showAlarmSettings = false })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else LightTeal.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "🔍 تكبير وتصغير لمساعدة ضعاف النظر:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) LightText else DarkTeal
        )
        
        Spacer(modifier = Modifier.width(4.dp))

        // Alarm Settings Trigger Button
        if (showAlarmButton) {
            IconButton(
                onClick = { showAlarmSettings = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "إعدادات المنبه",
                    tint = if (viewModel.alarmEnabled) GoldAccent else (if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // Minus Button
        IconButton(
            onClick = {
                if (viewModel.tableZoomScale > 0.85f) {
                    viewModel.tableZoomScale -= 0.1f
                }
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "تصغير",
                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Percent Text
        Text(
            text = "${(viewModel.tableZoomScale * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal
        )
        
        // Plus Button
        IconButton(
            onClick = {
                if (viewModel.tableZoomScale < 1.45f) {
                    viewModel.tableZoomScale += 0.1f
                }
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "تكبير",
                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Reset Button
        if (viewModel.tableZoomScale != 1.0f) {
            TextButton(
                onClick = { viewModel.tableZoomScale = 1.0f },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("إعادة ضبط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AlarmSettingsDialog(
    viewModel: QuranViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editingTimeIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    if (editingTimeIndex != null) {
        val timeIndex = editingTimeIndex!!
        val timeStr = viewModel.draftAlarmTimes[timeIndex] ?: "12:00"
        ComposeTimePickerDialog(
            initialTimeStr = timeStr,
            onDismiss = { editingTimeIndex = null },
            onConfirm = { selectedTime ->
                viewModel.updateAlarmTime(context, timeIndex, selectedTime)
                editingTimeIndex = null
            }
        )
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                viewModel.setAlarmRingtone(context, uri.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                )
                Text(
                    text = "إعدادات منبه الحصص",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Global Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else LightTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Switch(
                        checked = viewModel.draftAlarmEnabled,
                        onCheckedChange = { viewModel.toggleAlarmEnabled(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal,
                            checkedTrackColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else LightTeal
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "تفعيل المنبه الصوتي للحصص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal
                    )
                }

                // Draw over other apps permission check
                val hasOverlayPermission = remember(context) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.provider.Settings.canDrawOverlays(context)
                    } else {
                        true
                    }
                }
                
                var overlayGranted by remember { mutableStateOf(hasOverlayPermission) }
                
                // Re-check periodically when the composable updates
                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        overlayGranted = android.provider.Settings.canDrawOverlays(context)
                    }
                }

                if (viewModel.draftAlarmEnabled && !overlayGranted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSystemInDarkTheme()) Color(0xFF3E2723) else Color(0xFFFFF9C4),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSystemInDarkTheme()) Color(0xFFFFD54F) else Color(0xFFFBC02D),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "⚠️ تنبيه هام لمنع عدم ظهور المنبه:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSystemInDarkTheme()) Color(0xFFFFE082) else Color(0xFF5D4037),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "نظام الأندرويد يتطلب تفعيل صلاحية 'الظهور فوق التطبيقات الأخرى' لتتمكن شاشة المنبه من الظهور والعمل فوراً عندما تكون شاشة الهاتف مغلقة أو خارج التطبيق.",
                            fontSize = 12.sp,
                            color = if (isSystemInDarkTheme()) Color(0xFFD7CCC8) else Color(0xFF5D4037),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    try {
                                        val intent = Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                            context.startActivity(intent)
                                        } catch (ex: Exception) {}
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSystemInDarkTheme()) Color(0xFFFFD54F) else Color(0xFFFBC02D),
                                contentColor = if (isSystemInDarkTheme()) Color(0xFF3E2723) else Color(0xFF5D4037)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text("منح الصلاحية الآن 🔐", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Custom Ringtone Selector
                if (viewModel.draftAlarmEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else LightTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "نغمة المنبه:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MediumTeal
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة التنبيه للحصص")
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, viewModel.draftAlarmRingtoneUri?.let { Uri.parse(it) })
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtoneLauncher.launch(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else DarkTeal,
                                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تغيير النغمة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = if (viewModel.draftAlarmRingtoneUri != null) "تم اختيار نغمة مخصصة 🎵" else "النغمة الافتراضية للجهاز 🔔",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal
                            )
                        }
                    }

                    // Times configuration for the 8 slots
                    Text(
                        text = "ضبط أوقات المنبه للأعمدة الثمانية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MediumTeal,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (h in 0..7) {
                            val colLabel = viewModel.draftHourHeaders[h] ?: "العمود ${h + 1}"
                            val timeStr = viewModel.draftAlarmTimes[h] ?: "12:00"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else Color.White,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else LightTeal.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Time selector button
                                TextButton(
                                    onClick = {
                                        editingTimeIndex = h
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                                    )
                                ) {
                                    val parts = timeStr.split(":")
                                    val hInt = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                    val mInt = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    val amPm = if (hInt >= 12) "م" else "ص"
                                    val friendlyHour = when {
                                        hInt == 0 -> 12
                                        hInt > 12 -> hInt - 12
                                        else -> hInt
                                    }
                                    val friendlyTime = String.format("%d:%02d %s", friendlyHour, mInt, amPm)

                                    Text(
                                        text = "⏰ $friendlyTime",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = colLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DarkTeal
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "قم بتفعيل المنبه في الأعلى ليقوم التطبيق بتنبيهك تلقائياً عند مواعيد الحصص المثبتة.",
                            fontSize = 12.sp,
                            color = if (isSystemInDarkTheme()) LightText else MediumTeal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else DarkTeal
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ComposeTimePickerDialog(
    initialTimeStr: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTimeStr.split(":")
    val initialHour24 = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val initialIsAm = initialHour24 < 12
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }
    
    var hour12 by remember { mutableStateOf(initialHour12) }
    var minute by remember { mutableStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialIsAm) }
    
    val isDark = isSystemInDarkTheme()
    // Elegant colors from the palette:
    // Hour Color: Gold
    val hourColor = if (isDark) GoldAccent80 else DeepGold
    // Minute Color: Cyan/Teal
    val minuteColor = if (isDark) CyanPrimary else MediumTeal
    // AM/PM Color: Warm/Contrast Mint
    val periodColor = if (isDark) MintGreen80 else DarkTeal
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) PineSurface else Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "ضبط توقيت التنبيه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) MaterialTheme.colorScheme.primary else DarkTeal
                )
                
                // Digital Wheels display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AM/PM Picker
                    SwipePeriodPicker(
                        isAm = isAm,
                        onValueChange = { isAm = it },
                        color = periodColor
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Minutes Picker
                    WheelPicker(
                        value = minute,
                        range = 0..59,
                        onValueChange = { minute = it },
                        color = minuteColor,
                        labelFormatter = { String.format("%02d", it) }
                    )
                    
                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Hours Picker
                    WheelPicker(
                        value = hour12,
                        range = 1..12,
                        onValueChange = { hour12 = it },
                        color = hourColor,
                        labelFormatter = { it.toString() }
                    )
                }
                
                // Dialog Actions (Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "إلغاء",
                            color = if (isDark) Color.LightGray else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = {
                            // Convert 12h and am/pm back to 24h
                            val h24 = when {
                                isAm && hour12 == 12 -> 0
                                !isAm && hour12 < 12 -> hour12 + 12
                                else -> hour12
                            }
                            val formattedTime = String.format("%02d:%02d", h24, minute)
                            onConfirm(formattedTime)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else DarkTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "موافق",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    color: Color,
    labelFormatter: (Int) -> String = { String.format("%02d", it) },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rangeList = range.toList()
    val rangeSize = rangeList.size
    val virtualCount = Int.MAX_VALUE
    val middleOffset = virtualCount / 2
    // We want the initial value to be centered. Since the center item is index firstVisibleItemIndex + 1,
    // we want firstVisibleItemIndex to be startPosition - 1.
    val startPosition = middleOffset - (middleOffset % rangeSize) + rangeList.indexOf(value)
    
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startPosition - 1)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState)
    val itemHeight = 45.dp
    
    // Smoothly update the parent value only when the scroll has fully stopped!
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex + 1
            val mappedIndex = centerIndex % rangeSize
            val selectedVal = rangeList[mappedIndex]
            if (selectedVal != value) {
                onValueChange(selectedVal)
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {}
            }
        }
    }
    
    // Also, if the parent value changes externally, scroll to it (if not currently scrolling)
    LaunchedEffect(value) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex + 1
            val mappedIndex = centerIndex % rangeSize
            val selectedVal = rangeList[mappedIndex]
            if (selectedVal != value) {
                val diff = rangeList.indexOf(value) - mappedIndex
                val shortestDiff = when {
                    diff > rangeSize / 2 -> diff - rangeSize
                    diff < -rangeSize / 2 -> diff + rangeSize
                    else -> diff
                }
                val targetIndex = lazyListState.firstVisibleItemIndex + shortestDiff
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }
    
    Box(
        modifier = modifier
            .width(70.dp)
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = color.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                )
        )
        
        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            items(
                count = virtualCount,
                key = { it }
            ) { globalIndex ->
                val localIndex = globalIndex % rangeSize
                val itemVal = rangeList[localIndex]
                
                // Real-time local highlight based on currently scrolled center item
                val isSelected = globalIndex == lazyListState.firstVisibleItemIndex + 1
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFormatter(itemVal),
                        color = if (isSelected) color else color.copy(alpha = 0.35f),
                        fontSize = if (isSelected) 24.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        style = TextStyle(textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun SwipePeriodPicker(
    isAm: Boolean,
    onValueChange: (Boolean) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val options = listOf("م", "ص")
    val selectedIndex = if (isAm) 1 else 0
    val virtualCount = Int.MAX_VALUE
    val middleOffset = virtualCount / 2
    val startPosition = middleOffset - (middleOffset % 2) + selectedIndex
    
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startPosition - 1)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState)
    val itemHeight = 45.dp
    
    // Smoothly update the parent value only when the scroll has fully stopped!
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex + 1
            val mappedIndex = centerIndex % 2
            val selectedAm = mappedIndex == 1
            if (selectedAm != isAm) {
                onValueChange(selectedAm)
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {}
            }
        }
    }
    
    // Also, if the parent value changes externally, scroll to it (if not currently scrolling)
    LaunchedEffect(isAm) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex + 1
            val mappedIndex = centerIndex % 2
            val selectedAm = mappedIndex == 1
            if (selectedAm != isAm) {
                val targetIndex = if (isAm) {
                    if (mappedIndex == 0) lazyListState.firstVisibleItemIndex + 1 else lazyListState.firstVisibleItemIndex - 1
                } else {
                    if (mappedIndex == 1) lazyListState.firstVisibleItemIndex + 1 else lazyListState.firstVisibleItemIndex - 1
                }
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }
    
    Box(
        modifier = modifier
            .width(60.dp)
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = color.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                )
        )
        
        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = virtualCount,
                key = { it }
            ) { globalIndex ->
                val localIndex = globalIndex % 2
                val itemVal = options[localIndex]
                
                // Real-time local highlight based on currently scrolled center item
                val isSelected = globalIndex == lazyListState.firstVisibleItemIndex + 1
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemVal,
                        color = if (isSelected) color else color.copy(alpha = 0.35f),
                        fontSize = if (isSelected) 24.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        style = TextStyle(textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogClockPicker(
    hour12: Int,
    minute: Int,
    isAm: Boolean,
    onTimeChange: (Int, Int) -> Unit,
    hourColor: Color,
    minuteColor: Color,
    modifier: Modifier = Modifier
) {
    var isDraggingHour by remember { mutableStateOf<Boolean?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val isDark = isSystemInDarkTheme()
    
    Canvas(
        modifier = modifier
            .size(240.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val maxRadius = size.width / 2f
                        isDraggingHour = distance < maxRadius * 0.65f
                    },
                    onDragEnd = { isDraggingHour = null },
                    onDragCancel = { isDraggingHour = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val position = change.position
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = position.x - cx
                        val dy = position.y - cy
                        
                        var angleDegrees = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angleDegrees < 0) {
                            angleDegrees += 360f
                        }
                        
                        if (isDraggingHour == true) {
                            val rawHour = (angleDegrees + 15f) / 30f
                            var newHour = rawHour.toInt()
                            if (newHour == 0) newHour = 12
                            newHour = newHour.coerceIn(1, 12)
                            onTimeChange(newHour, minute)
                        } else if (isDraggingHour == false) {
                            val rawMinute = (angleDegrees + 3f) / 6f
                            val newMinute = (rawMinute.toInt()) % 60
                            onTimeChange(hour12, newMinute)
                        }
                    }
                )
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width / 2f
        
        drawCircle(
            color = if (isDark) Color(0xFF1F2937) else Color(0xFFECEFF1),
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isDark) Color(0xFF374151) else Color(0xFFCFD8DC),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawCircle(
            color = if (isDark) Color.White else Color(0xFF374151),
            radius = 6.dp.toPx(),
            center = Offset(cx, cy)
        )
        
        val arabicHours = listOf("١٢", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "١٠", "١١")
        for (i in 0..11) {
            val angleRad = Math.toRadians((i * 30 - 90).toDouble())
            val numRadius = radius * 0.75f
            val nx = cx + (numRadius * Math.cos(angleRad)).toFloat()
            val ny = cy + (numRadius * Math.sin(angleRad)).toFloat()
            
            val textLayoutResult = textMeasurer.measure(
                text = arabicHours[i],
                style = TextStyle(
                    color = hourColor.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(nx - textLayoutResult.size.width / 2f, ny - textLayoutResult.size.height / 2f)
            )
        }
        
        for (m in 0..59) {
            if (m % 5 != 0) {
                val angleRad = Math.toRadians((m * 6 - 90).toDouble())
                val dotRadius = radius * 0.9f
                val dx = cx + (dotRadius * Math.cos(angleRad)).toFloat()
                val dy = cy + (dotRadius * Math.sin(angleRad)).toFloat()
                drawCircle(
                    color = minuteColor.copy(alpha = 0.4f),
                    radius = 2.dp.toPx(),
                    center = Offset(dx, dy)
                )
            } else {
                val angleRad = Math.toRadians((m * 6 - 90).toDouble())
                val dotRadius = radius * 0.9f
                val dx = cx + (dotRadius * Math.cos(angleRad)).toFloat()
                val dy = cy + (dotRadius * Math.sin(angleRad)).toFloat()
                drawCircle(
                    color = minuteColor.copy(alpha = 0.8f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(dx, dy)
                )
            }
        }
        
        val hourAngleRad = Math.toRadians(((hour12 % 12) * 30 + (minute * 0.5) - 90))
        val hourHandLength = radius * 0.5f
        val hx = cx + (hourHandLength * Math.cos(hourAngleRad)).toFloat()
        val hy = cy + (hourHandLength * Math.sin(hourAngleRad)).toFloat()
        
        drawLine(
            color = hourColor,
            start = Offset(cx, cy),
            end = Offset(hx, hy),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        val minuteAngleRad = Math.toRadians((minute * 6 - 90).toDouble())
        val minuteHandLength = radius * 0.8f
        val mx = cx + (minuteHandLength * Math.cos(minuteAngleRad)).toFloat()
        val my = cy + (minuteHandLength * Math.sin(minuteAngleRad)).toFloat()
        
        drawLine(
            color = minuteColor,
            start = Offset(cx, cy),
            end = Offset(mx, my),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
