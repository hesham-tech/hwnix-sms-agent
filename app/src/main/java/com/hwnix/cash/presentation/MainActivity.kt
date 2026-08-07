package com.hwnix.cash.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.hwnix.cash.core.di.ViewModelFactory
import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.data.local.SyncEngine
import com.hwnix.cash.data.service.AgentForegroundService
import com.hwnix.cash.presentation.components.AppDrawer
import com.hwnix.cash.presentation.components.SimSetupDialog
import com.hwnix.cash.presentation.screens.LoginScreen
import com.hwnix.cash.presentation.screens.RegisterScreen
import com.hwnix.cash.presentation.screens.StatusScreen
import com.hwnix.cash.presentation.auth.login.LoginViewModel
import com.hwnix.cash.presentation.auth.register.RegisterViewModel
import com.hwnix.cash.presentation.status.StatusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// الكلاس الأساسي للتطبيق - يحتوي فقط على تهيئة المكونات وتوجيه دورة حياة Compose
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isSessionReady by remember { mutableStateOf(false) }
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var isRegistering by remember { mutableStateOf(false) }
                    var isStarting by remember { mutableStateOf(true) }

                    val context = this@MainActivity
                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(DrawerValue.Closed)

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val smsGranted = permissions[android.Manifest.permission.RECEIVE_SMS] == true
                        if (!smsGranted) {
                            Toast.makeText(context, "⚠️ التطبيق يحتاج لصلاحية الرسائل ليعمل بشكل صحيح.", Toast.LENGTH_LONG).show()
                        }
                    }

                    // 1. تهيئة الكائنات الثقيلة في الخلفية لتفادي الشاشة البيضاء عند النقر على الأيقونة
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.hwnix.cash.core.di.ServiceLocator.sessionManager
                            com.hwnix.cash.core.di.ServiceLocator.syncEngine
                        }
                        
                        val sessionManager = com.hwnix.cash.core.di.ServiceLocator.sessionManager
                        isLoggedIn = sessionManager.getAuthToken() != null
                        isSessionReady = true

                        // تشغيل الخدمة الخلفية المستمرة بعد تهيئة الجلسة
                        val serviceIntent = Intent(context, AgentForegroundService::class.java).apply {
                            putExtra("launcher_source", "MAIN_ACTIVITY")
                        }
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to start foreground service: ${e.message}", e)
                        }

                        // طلب الصلاحيات (الرسائل الواردة فقط والإشعارات)
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.RECEIVE_SMS,
                                android.Manifest.permission.READ_SMS,
                                android.Manifest.permission.READ_PHONE_STATE,
                                android.Manifest.permission.READ_CONTACTS,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                        delay(200)
                        isStarting = false
                    }

                    if (!isSessionReady) {
                        // شاشة ترحيبية / تهيئة سريعة لحين تحميل الكود بأمان بالخلفية
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "جاري تهيئة النظام الآمن...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    } else {
                        val sessionManager = com.hwnix.cash.core.di.ServiceLocator.sessionManager
                        val syncEngine = com.hwnix.cash.core.di.ServiceLocator.syncEngine
                        
                        val factory = remember { ViewModelFactory() }
                        val loginViewModel: LoginViewModel = ViewModelProvider(this@MainActivity, factory)[LoginViewModel::class.java]
                        val registerViewModel: RegisterViewModel = ViewModelProvider(this@MainActivity, factory)[RegisterViewModel::class.java]
                        val statusViewModel: StatusViewModel = ViewModelProvider(this@MainActivity, factory)[StatusViewModel::class.java]

                        val loginState by loginViewModel.uiState.collectAsState()
                        val registerState by registerViewModel.uiState.collectAsState()
                        val statusState by statusViewModel.uiState.collectAsState()

                        // فحص حالة تحسين البطارية والتحديثات تلقائياً
                        LaunchedEffect(isStarting) {
                            if (!isStarting) {
                                statusViewModel.checkBatteryOptimization()
                                
                                // طلب استثناء تحسين استهلاك البطارية تلقائياً فوراً إذا كان مقيداً
                                if (statusViewModel.uiState.value.isBatteryOptimized) {
                                    statusViewModel.disableBatteryOptimization()
                                }

                                // التحقق من وجود تحديث جديد وتنشيط المزامنة الأولية عند فتح التطبيق
                                try {
                                    if (sessionManager.getAuthToken() != null) {
                                        sessionManager.saveLastSyncSuccessTime(System.currentTimeMillis())
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            syncEngine.performFullSync()
                                        }
                                    }
                                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        packageInfo.longVersionCode.toInt()
                                    } else {
                                        @Suppress("DEPRECATION")
                                        packageInfo.versionCode
                                    }
                                    statusViewModel.checkForUpdate(currentVersionCode)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to check update on start: ${e.message}")
                                }
                            }
                        }

                        // مراقبة أحداث الخروج التلقائي وانتهائه عبر الشبكة
                        LaunchedEffect(Unit) {
                            com.hwnix.cash.core.auth.AuthEventManager.sessionExpiredEvents.collect { reason ->
                                sessionManager.clearAuthToken()
                                isLoggedIn = false
                                Toast.makeText(context, "⚠️ $reason", Toast.LENGTH_LONG).show()
                            }
                        }

                        // مزامنة تلقائية وفحص الجلسة عند عودة التطبيق للواجهة (onResume)
                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner, isLoggedIn) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                    if (isLoggedIn) {
                                        if (sessionManager.getAuthToken() == null) {
                                            isLoggedIn = false
                                            Toast.makeText(context, "⚠️ انتهت صلاحية جلسة الحساب.", Toast.LENGTH_LONG).show()
                                        } else {
                                            statusViewModel.performFullSync(syncEngine)
                                        }
                                    }
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                            }
                        }

                        // متابعة نجاح تسجيل الدخول / إنشاء الحساب
                        LaunchedEffect(loginState.isSuccess) {
                            if (loginState.isSuccess) {
                                isLoggedIn = true
                                statusViewModel.refreshDeviceInfo()
                                statusViewModel.performFullSync(syncEngine)
                            }
                        }

                        LaunchedEffect(registerState.isSuccess) {
                            if (registerState.isSuccess) {
                                isLoggedIn = true
                                statusViewModel.refreshDeviceInfo()
                                statusViewModel.performFullSync(syncEngine)
                            }
                        }

                    if (isStarting) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (!isLoggedIn) {
                        if (isRegistering) {
                            RegisterScreen(
                                state = registerState,
                                onCompanyNameChange = { registerViewModel.onCompanyNameChange(it) },
                                onFullNameChange = { registerViewModel.onFullNameChange(it) },
                                onNicknameChange = { registerViewModel.onNicknameChange(it) },
                                onPhoneChange = { registerViewModel.onPhoneChange(it) },
                                onEmailChange = { registerViewModel.onEmailChange(it) },
                                onPasswordChange = { registerViewModel.onPasswordChange(it) },
                                onTogglePassword = { registerViewModel.togglePasswordVisibility() },
                                onRegisterClick = { registerViewModel.register() },
                                onLoginToggleClick = { isRegistering = false }
                            )
                        } else {
                            LoginScreen(
                                state = loginState,
                                onLoginChange = { loginViewModel.onLoginInputChange(it) },
                                onPasswordChange = { loginViewModel.onPasswordInputChange(it) },
                                onTogglePassword = { loginViewModel.togglePasswordVisibility() },
                                onLoginClick = { loginViewModel.login() },
                                onRegisterToggleClick = { isRegistering = true }
                            )
                        }
                    } else {
                        val currentVersionName = remember {
                            try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "1.0.11" }
                        }
                        val currentVersionCode = remember {
                            try {
                                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
                            } catch (e: Exception) { 11 }
                        }

                        LaunchedEffect(drawerState.isOpen) {
                            if (drawerState.isOpen) {
                                statusViewModel.scanLocalUpdates(currentVersionCode)
                            }
                        }

                        var currentScreen by remember { mutableStateOf("status") }
                        val screenStack = remember { mutableStateListOf<String>() }

                        fun navigateTo(screen: String) {
                            if (currentScreen != screen) {
                                screenStack.add(currentScreen)
                                currentScreen = screen
                            }
                        }

                        fun popBackStack(): Boolean {
                            if (screenStack.isNotEmpty()) {
                                currentScreen = screenStack.removeAt(screenStack.size - 1)
                                return true
                            }
                            return false
                        }

                        androidx.activity.compose.BackHandler(enabled = currentScreen != "status") {
                            if (!popBackStack()) {
                                currentScreen = "status"
                            }
                        }

                        // Check if we need to force company selection or onboarding explanation
                        LaunchedEffect(isLoggedIn) {
                            if (isLoggedIn && sessionManager.getCompanyId() == -1L) {
                                navigateTo("company_selection")
                            } else if (isLoggedIn) {
                                if (!sessionManager.isExplanationOnboardingSeen()) {
                                    navigateTo("onboarding_explanation")
                                } else {
                                    statusViewModel.checkWallets { hasWallets ->
                                        if (hasWallets) {
                                            currentScreen = "status"
                                        } else {
                                            navigateTo("onboarding_wizard")
                                        }
                                    }
                                }
                            }
                        }

                        AppDrawer(
                            drawerState = drawerState,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            localUpdateVersionName = statusState.localUpdateVersionName,
                            localUpdateApk = statusState.localUpdateApk,
                            isCheckingUpdate = statusState.isCheckingUpdate,
                            updateStatusMessage = statusState.updateStatusMessage,
                            currentScreen = currentScreen,
                            onNavigateToScreen = { screen ->
                                navigateTo(screen)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onInstallLocalApk = { statusViewModel.installLocalApk(it) },
                            onCheckForUpdate = { statusViewModel.checkForUpdate(currentVersionCode) },
                             onLogoutClick = { statusViewModel.logout { isLoggedIn = false; screenStack.clear(); currentScreen = "status" } },
                             onDecoupleClick = { statusViewModel.logout { isLoggedIn = false; screenStack.clear(); currentScreen = "status" } }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (currentScreen == "diagnostics") {
                                    com.hwnix.cash.presentation.screens.DiagnosticsScreen()
                                } else if (currentScreen == "onboarding_explanation") {
                                    com.hwnix.cash.presentation.onboarding.OnboardingExplanationScreen(
                                        onStartSetupClick = {
                                            sessionManager.markExplanationOnboardingSeen(true)
                                            navigateTo("onboarding_wizard")
                                        },
                                        onReadLaterClick = {
                                            sessionManager.markExplanationOnboardingSeen(true)
                                            if (!popBackStack()) currentScreen = "status"
                                        }
                                    )
                                } else if (currentScreen == "company_selection") {
                                    com.hwnix.cash.presentation.screens.CompanySelectionScreen(
                                        onNavigateNext = {
                                            statusViewModel.performFullSync(syncEngine)
                                            statusViewModel.checkWallets { hasWallets ->
                                                if (hasWallets) {
                                                    currentScreen = "status"
                                                } else {
                                                    navigateTo("onboarding_wizard")
                                                }
                                            }
                                        }
                                    )
                                } else if (currentScreen == "onboarding_wizard") {
                                    val factoryOnboarding = remember { com.hwnix.cash.core.di.ViewModelFactory() }
                                    val onboardingViewModel: com.hwnix.cash.presentation.onboarding.OnboardingViewModel = ViewModelProvider(this@MainActivity, factoryOnboarding)[com.hwnix.cash.presentation.onboarding.OnboardingViewModel::class.java]
                                    com.hwnix.cash.presentation.onboarding.OnboardingWizardScreen(
                                        viewModel = onboardingViewModel,
                                        onSuccess = { screenStack.clear(); currentScreen = "onboarding_success" },
                                        onBackToMain = { if (!popBackStack()) currentScreen = "status" }
                                    )
                                } else if (currentScreen == "onboarding_success") {
                                    com.hwnix.cash.presentation.onboarding.SuccessScreen(
                                        onFinish = { screenStack.clear(); currentScreen = "status" }
                                    )
                                } else {
                                    StatusScreen(
                                        state = statusState,
                                        onRefresh = { statusViewModel.refreshAll(currentVersionCode) },
                                        onSyncNowClick = { statusViewModel.performFullSync(syncEngine) },
                                        onSimSetupClick = { statusViewModel.openSimSetupDialog() },
                                        onBatteryOptimizeClick = {
                                            statusViewModel.disableBatteryOptimization()
                                        },
                                        onAddWalletClick = { navigateTo("onboarding_wizard") }
                                    )
                                }
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "القائمة",
                                        tint = androidx.compose.ui.graphics.Color(0xFF0D47A1),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        // Dialog إعداد أرقام الـ SIM
                        if (statusState.showSimDialog) {
                            SimSetupDialog(
                                sims = statusState.detectedSims,
                                phoneInputs = statusState.simPhoneInputs,
                                carrierInputs = statusState.simCarrierInputs,
                                gatewayName = statusState.gatewayName,
                                isSaving = statusState.isSavingSims,
                                saveResult = statusState.simSaveResult,
                                isFirstSetup = statusState.isFirstSetup,
                                onGatewayNameChange = { statusViewModel.onGatewayNameChange(it) },
                                onPhoneChange = { slotIndex, value ->
                                    statusViewModel.onSimPhoneChange(slotIndex, value)
                                },
                                onCarrierChange = { slotIndex, value ->
                                    statusViewModel.onSimCarrierChange(slotIndex, value)
                                },
                                onSave = { statusViewModel.saveSimSetup() },
                                onDismiss = { statusViewModel.dismissSimDialog() }
                            )
                        }

                        // Dialog تحديث التطبيق
                        if (statusState.showUpdateDialog) {
                            AlertDialog(
                                onDismissRequest = { statusViewModel.dismissUpdateDialog() },
                                title = { Text("تحديث جديد متاح") },
                                text = {
                                    Column {
                                        Text("يتوفر إصدار جديد من التطبيق (${statusState.updateVersionName}). هل تريد تنزيله وتثبيته الآن؟")
                                        if (statusState.isDownloadingUpdate) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            LinearProgressIndicator(
                                                progress = statusState.updateDownloadProgress,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "${(statusState.updateDownloadProgress * 100).toInt()}%",
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        enabled = !statusState.isDownloadingUpdate,
                                        onClick = { statusViewModel.downloadAndInstallUpdate() }
                                    ) { Text("تنزيل وتثبيت") }
                                },
                                dismissButton = {
                                    if (!statusState.isDownloadingUpdate) {
                                        TextButton(onClick = { statusViewModel.dismissUpdateDialog() }) {
                                            Text("إلغاء")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
