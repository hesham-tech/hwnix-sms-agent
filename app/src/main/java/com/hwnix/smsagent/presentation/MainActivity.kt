package com.hwnix.smsagent.presentation

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
import com.hwnix.smsagent.core.di.ViewModelFactory
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.local.SyncEngine
import com.hwnix.smsagent.data.service.AgentForegroundService
import com.hwnix.smsagent.presentation.components.AppDrawer
import com.hwnix.smsagent.presentation.components.SimSetupDialog
import com.hwnix.smsagent.presentation.screens.LoginScreen
import com.hwnix.smsagent.presentation.screens.RegisterScreen
import com.hwnix.smsagent.presentation.screens.StatusScreen
import com.hwnix.smsagent.presentation.auth.login.LoginViewModel
import com.hwnix.smsagent.presentation.auth.register.RegisterViewModel
import com.hwnix.smsagent.presentation.status.StatusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// الكلاس الأساسي للتطبيق - يحتوي فقط على تهيئة المكونات وتوجيه دورة حياة Compose
class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var syncEngine: SyncEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)
        syncEngine = SyncEngine(applicationContext)

        // تشغيل الخدمة الخلفية المستمرة لإرسال واستقبال الرسائل
        val serviceIntent = Intent(this, AgentForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start foreground service: ${e.message}", e)
        }


        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val factory = remember { ViewModelFactory() }
                    val loginViewModel: LoginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
                    val registerViewModel: RegisterViewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]
                    val statusViewModel: StatusViewModel = ViewModelProvider(this, factory)[StatusViewModel::class.java]

                    val loginState by loginViewModel.uiState.collectAsState()
                    val registerState by registerViewModel.uiState.collectAsState()
                    val statusState by statusViewModel.uiState.collectAsState()

                    var isLoggedIn by remember { mutableStateOf(sessionManager.getAuthToken() != null) }
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

                    // فحص الصلاحيات والتحقق من التحديثات عند البدء
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.RECEIVE_SMS,
                                android.Manifest.permission.SEND_SMS,
                                android.Manifest.permission.READ_SMS,
                                android.Manifest.permission.READ_PHONE_STATE,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                        delay(800)
                        isStarting = false
                        statusViewModel.checkBatteryOptimization()

                        // التحقق من وجود تحديث جديد عند فتح التطبيق
                        try {
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

                    // متابعة نجاح تسجيل الدخول / إنشاء الحساب
                    LaunchedEffect(loginState.isSuccess) {
                        if (loginState.isSuccess) {
                            isLoggedIn = true
                            statusViewModel.refreshDeviceInfo()
                        }
                    }

                    LaunchedEffect(registerState.isSuccess) {
                        if (registerState.isSuccess) {
                            isLoggedIn = true
                            statusViewModel.refreshDeviceInfo()
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
                                onServerUrlChange = { registerViewModel.onServerUrlChange(it) },
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
                                onServerUrlChange = { loginViewModel.onServerUrlChange(it) },
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

                        AppDrawer(
                            drawerState = drawerState,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            localUpdateVersionName = statusState.localUpdateVersionName,
                            localUpdateApk = statusState.localUpdateApk,
                            isCheckingUpdate = statusState.isCheckingUpdate,
                            updateStatusMessage = statusState.updateStatusMessage,
                            onInstallLocalApk = { statusViewModel.installLocalApk(it) },
                            onCheckForUpdate = { statusViewModel.checkForUpdate(currentVersionCode) }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                StatusScreen(
                                    state = statusState,
                                    onRefresh = { statusViewModel.refreshAll(currentVersionCode) },
                                    onSyncNowClick = { statusViewModel.performFullSync(syncEngine) },
                                    onSimSetupClick = { statusViewModel.openSimSetupDialog() },
                                    onLogoutClick = {
                                        statusViewModel.logout {
                                            isLoggedIn = false
                                        }
                                    },
                                    onBatteryOptimizeClick = {
                                        statusViewModel.disableBatteryOptimization()
                                    }
                                )
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "القائمة",
                                        tint = MaterialTheme.colorScheme.primary
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
