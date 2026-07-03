package com.hwnix.smsagent.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.local.SyncEngine
import com.hwnix.smsagent.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ======================================================
// بيانات تمثيل شريحة SIM واحدة
// ======================================================
data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: String,
    val carrier: String,
    val phoneNumber: String, // قد يكون فارغاً إذا لم يستطع النظام قراءته
    val mcc: String,
    val mnc: String,
)

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var syncEngine: SyncEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)
        syncEngine = SyncEngine(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithDrawer(
                        sessionManager = sessionManager,
                        syncEngine = syncEngine,
                        context = this
                    )
                }
            }
        }
    }
}

// ======================================================
// التطبيق مع القائمة الجانبية
// ======================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithDrawer(
    sessionManager: SessionManager,
    syncEngine: SyncEngine,
    context: android.content.Context
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // معلومات الإصدار الحالي
    val currentVersionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "غير معروف" }
    }
    val currentVersionCode = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
        } catch (e: Exception) { 0 }
    }

    // حالة فحص التحديث
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDownloadUrl by remember { mutableStateOf("") }
    var updateVersionName by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    var localUpdateApk by remember { mutableStateOf<java.io.File?>(null) }
    var localUpdateVersionName by remember { mutableStateOf("") }

    fun scanLocalUpdates() {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val files = downloadsDir.listFiles() ?: return
            var highestVersionCode = currentVersionCode
            var bestApk: java.io.File? = null
            var bestVersionName = ""
            
            for (file in files) {
                if (file.isFile && file.name.startsWith("sms-agent-v") && file.name.endsWith(".apk")) {
                    val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                    if (info != null) {
                        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            info.longVersionCode.toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            info.versionCode
                        }
                        if (apkVersionCode > highestVersionCode) {
                            highestVersionCode = apkVersionCode
                            bestApk = file
                            bestVersionName = info.versionName ?: ""
                        }
                    }
                }
            }
            localUpdateApk = bestApk
            localUpdateVersionName = bestVersionName
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to scan local updates: ${e.message}")
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            scanLocalUpdates()
        }
    }

    LaunchedEffect(Unit) {
        scanLocalUpdates()
    }

    // دالة فحص التحديث يدوياً
    fun checkForUpdate() {
        isCheckingUpdate = true
        updateStatusMessage = null
        coroutineScope.launch {
            try {
                val updateInfo = withContext(Dispatchers.IO) {
                    val apiService = ApiClient.getService(context)
                    val response = apiService.checkAppUpdate()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.get("status")?.asBoolean == true) body.getAsJsonObject("data") else null
                    } else null
                }
                if (updateInfo != null) {
                    val serverVersionCode = updateInfo.get("version_code").asInt
                    val serverVersionNameStr = updateInfo.get("version_name").asString
                    val downloadUrl = updateInfo.get("download_url").asString
                    if (serverVersionCode > currentVersionCode) {
                        updateDownloadUrl = downloadUrl
                        updateVersionName = serverVersionNameStr
                        showUpdateDialog = true
                    } else {
                        updateStatusMessage = "✅ التطبيق محدّث. لا يوجد إصدار جديد."
                    }
                } else {
                    updateStatusMessage = "⚠️ تعذّر الاتصال بالسيرفر."
                }
            } catch (e: Exception) {
                updateStatusMessage = "❌ خطأ: ${e.message}"
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.78f)) {
                // رأس القائمة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            "HWNix SMS Agent",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "بوابة الرسائل النصية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // قسم حول التطبيق
                NavigationDrawerItem(
                    label = { Text("حول التطبيق", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    selected = false,
                    onClick = {}
                )

                // بطاقة معلومات الإصدار
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الإصدار الحالي: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(currentVersionName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("كود الإصدار: ", style = MaterialTheme.typography.bodySmall)
                            Text("$currentVersionCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (localUpdateApk != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إصدار متوفر للتثبيت: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(localUpdateVersionName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // زر التحقق من التحديثات أو زر التثبيت المحلي
                if (localUpdateApk != null) {
                    Button(
                        onClick = { installApk(context, localUpdateApk!!) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("تثبيت التحديث (v$localUpdateVersionName)")
                    }
                } else {
                    Button(
                        onClick = { checkForUpdate() },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الفحص...")
                        } else {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("التحقق من التحديثات")
                        }
                    }
                }

                // رسالة حالة التحديث
                if (updateStatusMessage != null) {
                    Text(
                        text = updateStatusMessage!!,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()
                Text(
                    "HWNix © 2025",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        // زر فتح القائمة الجانبية (Hamburger)
        Box(modifier = Modifier.fillMaxSize()) {
            GatewayScreen(
                sessionManager = sessionManager,
                syncEngine = syncEngine,
                context = context
            )
            IconButton(
                onClick = { coroutineScope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "القائمة", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Dialog التحديث (من الـ Drawer)
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) showUpdateDialog = false },
            title = { Text("تحديث جديد متاح") },
            text = {
                Column {
                    Text("يتوفر إصدار جديد ($updateVersionName). هل تريد تنزيله وتثبيته الآن؟")
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(downloadProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDownloading,
                    onClick = {
                        coroutineScope.launch {
                            isDownloading = true
                            val success = downloadAndInstallApk(context, updateDownloadUrl, updateVersionName) { progress ->
                                downloadProgress = progress
                            }
                            isDownloading = false
                            if (success) {
                                showUpdateDialog = false
                                scanLocalUpdates() // فحص جديد لتحديث الزر فوراً
                            }
                        }
                    }
                ) { Text("تنزيل وتثبيت") }
            },
            dismissButton = {
                if (!isDownloading) TextButton(onClick = { showUpdateDialog = false }) { Text("لاحقاً") }
            }
        )
    }
}

// ======================================================
// قراءة الـ SIM Cards من الهاتف
// ======================================================
@SuppressLint("MissingPermission")
fun readSimCards(context: Context): List<SimInfo> {
    val result = mutableListOf<SimInfo>()
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return emptyList()
            val activeInfoList = subManager.activeSubscriptionInfoList ?: return emptyList()
            for (info in activeInfoList) {
                val mccVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.mccString ?: ""
                } else {
                    @Suppress("DEPRECATION")
                    info.mcc?.toString() ?: ""
                }
                val mncVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.mncString ?: ""
                } else {
                    @Suppress("DEPRECATION")
                    info.mnc?.toString() ?: ""
                }
                result.add(
                    SimInfo(
                        slotIndex = info.simSlotIndex,
                        subscriptionId = info.subscriptionId.toString(),
                        carrier = if (info.carrierName?.toString().isNullOrBlank()) "Unknown" else info.carrierName.toString(),
                        phoneNumber = info.number ?: "",
                        mcc = mccVal,
                        mnc = mncVal,
                    )
                )
            }
        } else {
            // Fallback للأجهزة القديمة
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val number = tm?.line1Number ?: ""
            val carrierName = tm?.networkOperatorName
            val carrier = if (carrierName.isNullOrBlank()) "Unknown" else carrierName
            result.add(
                SimInfo(
                    slotIndex = 0,
                    subscriptionId = "-1",
                    carrier = carrier,
                    phoneNumber = number,
                    mcc = "",
                    mnc = "",
                )
            )
        }
        result
    } catch (e: Throwable) {
        Log.e("SimReader", "Failed to read SIM info: ${e.message}")
        emptyList()
    }
}

// ======================================================
// الشاشة الرئيسية
// ======================================================
@Composable
fun GatewayScreen(
    sessionManager: SessionManager,
    syncEngine: SyncEngine,
    context: android.content.Context
) {
    val coroutineScope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(sessionManager.getAuthToken() != null) }

    // طلب الصلاحيات
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // حقول المصادقة
    var serverUrl by remember {
        mutableStateOf(
            sessionManager.getBaseUrl().let { url ->
                if (url.contains("10.0.2.2") || url.isEmpty() || url.contains("localhost")) {
                    "https://api-teste.hwnix.com"
                } else {
                    var clean = url.trim()
                    if (clean.endsWith("/")) clean = clean.substring(0, clean.length - 1)
                    if (clean.endsWith("/api")) clean = clean.substring(0, clean.length - 4)
                    clean.trim()
                }
            }
        )
    }
    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // حالة تحديث التطبيق
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDownloadUrl by remember { mutableStateOf("") }
    var updateVersionName by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // حالة SIM setup
    var showSimDialog by remember { mutableStateOf(false) }
    var detectedSims by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
    // مدخلات المستخدم لأرقام الخطوط — Map من slotIndex إلى رقم الهاتف
    var simPhoneInputs by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var simCarrierInputs by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var simSaving by remember { mutableStateOf(false) }
    var simSaveResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
        kotlinx.coroutines.delay(1200)
        isStarting = false

        // فحص تحديث التطبيق
        try {
            val updateInfo = withContext(Dispatchers.IO) {
                val apiService = ApiClient.getService(context)
                val response = apiService.checkAppUpdate()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("status")?.asBoolean == true) body.getAsJsonObject("data") else null
                } else null
            }
            if (updateInfo != null) {
                val serverVersionCode = updateInfo.get("version_code").asInt
                val serverVersionName = updateInfo.get("version_name").asString
                val downloadUrl = updateInfo.get("download_url").asString
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
                if (serverVersionCode > currentVersionCode) {
                    updateDownloadUrl = downloadUrl
                    updateVersionName = serverVersionName
                    showUpdateDialog = true
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Check update failed: ${e.message}")
        }
    }

    val deviceUuid = remember { sessionManager.getDeviceUuid() }
    var deviceId by remember { mutableStateOf(sessionManager.getDeviceId()) }
    var configVersion by remember { mutableStateOf(sessionManager.getConfigVersion()) }
    var connectionStatus by remember { mutableStateOf("Offline") }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            connectionStatus = "Online"
            configVersion = sessionManager.getConfigVersion()
            deviceId = sessionManager.getDeviceId()
        } else {
            connectionStatus = "Offline"
        }
    }

    // Splash
    if (isStarting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "HWNix SMS Gateway",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("جاري تهيئة النظام...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HWNix SMS Gateway Agent",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!isLoggedIn) {
            // ========== شاشة تسجيل الدخول ==========
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("رابط السيرفر (Server API URL)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = loginInput,
                onValueChange = { loginInput = it },
                label = { Text("البريد الإلكتروني أو الهاتف") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("كلمة المرور") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    isLoading = true
                    val cleanedUrl = serverUrl.trim().removeSuffix("/")
                    sessionManager.saveBaseUrl(cleanedUrl)
                    ApiClient.resetClient()
                    coroutineScope.launch {
                        val errorMsg = authenticateAgent(
                            context = context,
                            sessionManager = sessionManager,
                            syncEngine = syncEngine,
                            login = loginInput.trim(),
                            password = passwordInput.trim()
                        )
                        isLoading = false
                        if (errorMsg == null) {
                            isLoggedIn = true
                        } else {
                            errorMessage = errorMsg
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("ربط وتسجيل الدخول")
                }
            }
        } else {
            // ========== شاشة الحالة ==========
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("حالة الاتصال: $connectionStatus", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("معرف الجهاز (ID): $deviceId")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("الـ UUID: $deviceUuid")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("إصدار الإعدادات: $configVersion")
                }
            }

            // زر المزامنة الفورية
            Button(
                onClick = {
                    coroutineScope.launch {
                        connectionStatus = "جاري المزامنة..."
                        val syncSuccess = withContext(Dispatchers.IO) {
                            try { syncEngine.performFullSync(); true } catch (e: Exception) { false }
                        }
                        connectionStatus = if (syncSuccess) "Online" else "Error"
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("مزامنة فورية الآن")
            }

            // ========== زر إعداد الخطوط ==========
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        // قراءة الشرائح من الهاتف فوراً
                        val sims = readSimCards(context)
                        detectedSims = sims
                        
                        // جلب البيانات المحفوظة سابقاً من السيرفر
                        val savedLines = withContext(Dispatchers.IO) {
                            try { syncEngine.getSavedSimLines() } catch(e: Exception) { emptyMap() }
                        }
                        
                        // دمج البيانات: نعطي الأولوية لبيانات السيرفر وإلا نقرأ من الخط
                        simPhoneInputs = sims.associate { sim ->
                            val savedPhone = savedLines[sim.slotIndex]?.second
                            sim.slotIndex to (if (!savedPhone.isNullOrBlank()) savedPhone else sim.phoneNumber)
                        }
                        simCarrierInputs = sims.associate { sim ->
                            val savedCarrier = savedLines[sim.slotIndex]?.first
                            val carrierVal = if (!savedCarrier.isNullOrBlank()) savedCarrier else sim.carrier
                            sim.slotIndex to (if (carrierVal == "Unknown") "" else carrierVal)
                        }
                        
                        simSaveResult = null
                        showSimDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.SimCard, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("إعداد أرقام الخطوط (SIM)")
            }

            OutlinedButton(
                onClick = {
                    sessionManager.clearSession()
                    isLoggedIn = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل الخروج وإلغاء الربط")
            }
        }

        // ========== Dialog تحديث التطبيق ==========
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDownloading) showUpdateDialog = false },
                title = { Text("تحديث جديد متاح") },
                text = {
                    Column {
                        Text("يتوفر إصدار جديد من التطبيق ($updateVersionName). هل تريد تنزيله وتثبيته الآن؟")
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("جاري التنزيل: ${(downloadProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isDownloading,
                        onClick = {
                            coroutineScope.launch {
                                isDownloading = true
                                val success = downloadAndInstallApk(context, updateDownloadUrl, updateVersionName) { progress ->
                                    downloadProgress = progress
                                }
                                isDownloading = false
                                if (success) showUpdateDialog = false
                            }
                        }
                    ) { Text("تنزيل وتثبيت") }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { showUpdateDialog = false }) { Text("إلغاء") }
                    }
                }
            )
        }

        // ========== Dialog إعداد أرقام الـ SIM ==========
        if (showSimDialog) {
            SimSetupDialog(
                sims = detectedSims,
                phoneInputs = simPhoneInputs,
                carrierInputs = simCarrierInputs,
                isSaving = simSaving,
                saveResult = simSaveResult,
                onPhoneChange = { slotIndex, value ->
                    simPhoneInputs = simPhoneInputs.toMutableMap().also { it[slotIndex] = value }
                },
                onCarrierChange = { slotIndex, value ->
                    simCarrierInputs = simCarrierInputs.toMutableMap().also { it[slotIndex] = value }
                },
                onSave = {
                    coroutineScope.launch {
                        simSaving = true
                        simSaveResult = null
                        val lines = detectedSims.map { sim ->
                            mapOf(
                                "slot_index" to sim.slotIndex.toString(),
                                "subscription_id" to sim.subscriptionId,
                                "carrier" to (simCarrierInputs[sim.slotIndex]?.ifBlank { "Unknown" } ?: "Unknown"),
                                "phone_number" to (simPhoneInputs[sim.slotIndex] ?: ""),
                                "mcc" to sim.mcc,
                                "mnc" to sim.mnc,
                            )
                        }
                        val error = withContext(Dispatchers.IO) {
                            syncEngine.syncSimLines(lines)
                        }
                        simSaving = false
                        simSaveResult = if (error == null) "✅ تم حفظ أرقام الخطوط بنجاح!" else "❌ $error"
                    }
                },
                onDismiss = { showSimDialog = false }
            )
        }
    }
}

// ======================================================
// Dialog إعداد أرقام الخطوط
// ======================================================
@Composable
fun SimSetupDialog(
    sims: List<SimInfo>,
    phoneInputs: Map<Int, String>,
    carrierInputs: Map<Int, String>,
    isSaving: Boolean,
    saveResult: String?,
    onPhoneChange: (Int, String) -> Unit,
    onCarrierChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("إعداد أرقام الخطوط") },
        text = {
            Column {
                if (sims.isEmpty()) {
                    Text("⚠️ لم يتم اكتشاف أي شريحة نشطة في الجهاز.")
                } else {
                    Text(
                        "تم اكتشاف ${sims.size} خط. أدخل البيانات لكل خط:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sims.forEach { sim ->
                        val currentCarrier = carrierInputs[sim.slotIndex] ?: ""
                        val currentPhone = phoneInputs[sim.slotIndex] ?: ""
                        
                        OutlinedTextField(
                            value = currentCarrier,
                            onValueChange = { onCarrierChange(sim.slotIndex, it) },
                            label = { Text("اسم مشغل الشبكة (SIM ${sim.slotIndex + 1}) *") },
                            placeholder = { Text("مثال: Vodafone أو Orange") },
                            singleLine = true,
                            isError = currentCarrier.isBlank(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = currentPhone,
                            onValueChange = { onPhoneChange(sim.slotIndex, it) },
                            label = { Text("رقم الهاتف (SIM ${sim.slotIndex + 1})") },
                            placeholder = { Text("مثال: 01XXXXXXXXX") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }
                }

                if (saveResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        saveResult,
                        color = if (saveResult.startsWith("✅")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                if (isSaving) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            val isAllCarriersValid = sims.all { !carrierInputs[it.slotIndex].isNullOrBlank() }
            Button(
                onClick = onSave,
                enabled = !isSaving && sims.isNotEmpty() && isAllCarriersValid
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("إغلاق")
            }
        }
    )
}

// ======================================================
// دوال المساعدة
// ======================================================
private suspend fun authenticateAgent(
    context: android.content.Context,
    sessionManager: SessionManager,
    syncEngine: SyncEngine,
    login: String,
    password: String
): String? = withContext(Dispatchers.IO) {
    try {
        val payload = JsonObject().apply {
            addProperty("login", login)
            addProperty("password", password)
            addProperty("device_uuid", sessionManager.getDeviceUuid())
            addProperty("device_name", android.os.Build.MODEL)
            addProperty("brand", android.os.Build.BRAND)
            addProperty("model", android.os.Build.MODEL)
            addProperty("android_version", android.os.Build.VERSION.RELEASE)
            addProperty("app_version", "1.0.11")
        }

        val apiService = ApiClient.getService(context)
        val response = apiService.login(payload)

        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            if (responseBody.get("status")?.asBoolean == true) {
                val data = responseBody.getAsJsonObject("data")
                val token = data.get("token").asString
                sessionManager.saveAuthToken(token)
                ApiClient.resetClient()

                val freshApiService = ApiClient.getService(context)
                val regPayload = JsonObject().apply {
                    addProperty("android_id", sessionManager.getDeviceUuid())
                    addProperty("uuid", sessionManager.getDeviceUuid())
                    addProperty("device_name", android.os.Build.MODEL)
                    addProperty("brand", android.os.Build.BRAND)
                    addProperty("model", android.os.Build.MODEL)
                    addProperty("android_version", android.os.Build.VERSION.RELEASE)
                    addProperty("app_version", "1.0.11")
                }

                val regResponse = freshApiService.registerDevice(
                    idempotencyKey = java.util.UUID.randomUUID().toString(),
                    body = regPayload
                )

                if (regResponse.isSuccessful && regResponse.body() != null) {
                    val regBody = regResponse.body()!!
                    if (regBody.get("status")?.asBoolean == true) {
                        val regData = regBody.getAsJsonObject("data")
                        val deviceId = regData.get("device_id").asLong
                        sessionManager.saveDeviceId(deviceId)
                    }
                }

                syncEngine.performFullSync()
                return@withContext null
            }
        }

        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrEmpty()) {
            try {
                val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                val msg = json.get("message")?.asString
                if (!msg.isNullOrEmpty()) return@withContext msg
            } catch (ex: Exception) {}
        }
        return@withContext "بيانات الاعتماد غير صحيحة. يرجى التحقق من البريد وكلمة المرور."
    } catch (e: java.net.ConnectException) {
        return@withContext "فشل الاتصال بالسيرفر. يرجى التأكد من صحة الرابط."
    } catch (e: java.net.UnknownHostException) {
        return@withContext "فشل الاتصال بالإنترنت أو رابط السيرفر غير صحيح."
    } catch (e: Exception) {
        Log.e("MainActivity", "Authentication failed: ${e.message}")
        return@withContext "حدث خطأ أثناء الاتصال: ${e.localizedMessage ?: e.message}"
    }
}

private fun saveApkToPublicDownloads(context: Context, sourceFile: java.io.File, versionName: String) {
    try {
        val fileName = "sms-agent-v$versionName.apk"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/HWNixSMSAgent")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } else {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadsDir = java.io.File(baseDir, "HWNixSMSAgent")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = java.io.File(downloadsDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
        }
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to save to public downloads: ${e.message}")
    }
}

private suspend fun downloadAndInstallApk(
    context: android.content.Context,
    downloadUrl: String,
    versionName: String,
    onProgress: (Float) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = java.net.URL(downloadUrl)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connect()
        if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) return@withContext false
        val fileLength = connection.contentLength
        val input = connection.inputStream
        
        // حفظ الملف في المجلد الخاص بالتنزيل للتطبيق
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = java.io.File(downloadsDir, "sms-agent-v$versionName.apk")
        if (apkFile.exists()) apkFile.delete()
        
        val output = java.io.FileOutputStream(apkFile)
        val data = ByteArray(65536) // 64KB buffer
        var total = 0L
        var count: Int
        var lastProgressUpdate = 0f
        while (input.read(data).also { count = it } != -1) {
            total += count
            if (fileLength > 0) {
                val currentProgress = total.toFloat() / fileLength.toFloat()
                if (currentProgress - lastProgressUpdate >= 0.01f || currentProgress >= 1f) {
                    onProgress(currentProgress)
                    lastProgressUpdate = currentProgress
                }
            }
            output.write(data, 0, count)
        }
        output.flush(); output.close(); input.close()
        
        // حفظ نسخة في مجلد التنزيلات العام للهاتف
        saveApkToPublicDownloads(context, apkFile, versionName)
        
        withContext(Dispatchers.Main) { installApk(context, apkFile) }
        return@withContext true
    } catch (e: Exception) {
        Log.e("MainActivity", "Download APK failed: ${e.message}")
    }
    return@withContext false
}

private fun installApk(context: android.content.Context, apkFile: java.io.File) {
    try {
        // فحص وتوجيه المستخدم لتفعيل صلاحية تثبيت التطبيقات غير المعروفة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "يرجى تفعيل صلاحية تثبيت التطبيقات غير المعروفة للتطبيق والمحاولة مجدداً.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("MainActivity", "Install APK failed: ${e.message}")
    }
}
