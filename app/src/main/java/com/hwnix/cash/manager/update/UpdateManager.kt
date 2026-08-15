package com.hwnix.cash.manager.update

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// مسؤولية هذا الكلاس: إدارة تنزيل وحفظ وتثبيت تحديثات التطبيق (APK)
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
    }

    /**
     * التحقق مما إذا كان التطبيق يمتلك إذن تثبيت الحزم من مصادر غير معروفة.
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * تنظيف ملفات الـ APK القديمة من المجلد الخاص بالتطبيق لتوفير المساحة.
     */
    fun cleanOldApkFiles() {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val files = downloadsDir.listFiles() ?: return
            for (file in files) {
                if (file.isFile && (file.name.startsWith("hwnix-cash-v") || file.name.startsWith("sms-agent-v")) && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old APK files: ${e.message}")
        }
    }

    /**
     * حفظ نسخة من ملف الـ APK في مجلد التنزيلات العام للهاتف ليكون متاحاً للمستخدم.
     */
    fun saveApkToPublicDownloads(sourceFile: File, versionName: String) {
        try {
            val fileName = "hwnix-cash-v$versionName.apk"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/HWNixCash")
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
                val downloadsDir = File(baseDir, "HWNixCash")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
            }
            Log.i(TAG, "Successfully copied APK to public downloads.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to public downloads: ${e.message}")
        }
    }

    /**
     * تنزيل ملف الـ APK من الرابط وتمرير نسبة التقدم للواجهة.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        versionName: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            cleanOldApkFiles()

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val fileLength = connection.contentLength
            val input = connection.inputStream

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadsDir, "hwnix-cash-v$versionName.apk")
            if (apkFile.exists()) apkFile.delete()

            val output = FileOutputStream(apkFile)
            val data = ByteArray(65536) // 64KB buffer
            var total = 0L
            var count: Int
            var lastProgressUpdate = 0f

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val currentProgress = total.toFloat() / fileLength.toFloat()
                    if (currentProgress - lastProgressUpdate >= 0.01f || currentProgress >= 1f) {
                        withContext(Dispatchers.Main) {
                            onProgress(currentProgress)
                        }
                        lastProgressUpdate = currentProgress
                    }
                }
                output.write(data, 0, count)
            }
            output.flush()
            try {
                output.fd.sync() // إجبار النظام على كتابة الملف بالكامل للقرص لتفادي القراءة الناقصة أثناء التثبيت
            } catch (e: Exception) {
                Log.e(TAG, "File sync failed: ${e.message}")
            }
            output.close()
            input.close()

            saveApkToPublicDownloads(apkFile, versionName)
            return@withContext apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Download APK failed: ${e.message}")
            return@withContext null
        }
    }

    /**
     * توجيه النظام لتثبيت ملف الـ APK.
     */
    fun installApk(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "يرجى تفعيل صلاحية تثبيت التطبيقات غير المعروفة للتطبيق والمحاولة مجدداً.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                installApkWithPackageInstaller(apkFile)
            } else {
                installApkLegacy(apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Install APK failed: ${e.message}", e)
        }
    }

    private fun installApkLegacy(apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val resInfoList = context.packageManager.queryIntentActivities(
                intent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            Log.e(TAG, "Legacy install failed: ${ex.message}")
        }
    }

    private fun installApkWithPackageInstaller(apkFile: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val sizeBytes = apkFile.length()
            val inStream = java.io.FileInputStream(apkFile)
            val outStream = session.openWrite("package", 0, sizeBytes)
            inStream.copyTo(outStream)
            session.fsync(outStream)
            outStream.close()
            inStream.close()
            
            val intent = Intent("com.hwnix.cash.ACTION_INSTALL_COMPLETE")
            intent.setPackage(context.packageName)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, sessionId, intent, flags
            )
            
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context, receivedIntent: Intent) {
                    val status = receivedIntent.getIntExtra(android.content.pm.PackageInstaller.EXTRA_STATUS, android.content.pm.PackageInstaller.STATUS_FAILURE)
                    val message = receivedIntent.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No message"
                    
                    if (status == android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION) {
                        val confirmationIntent = receivedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        if (confirmationIntent != null) {
                            confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(confirmationIntent)
                        }
                    } else if (status != android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                        val errorMsg = "خطأ في التثبيت!\nالرمز: $status\nالرسالة: $message"
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                        }
                        try {
                            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            java.io.File(downloads, "hwnix_install_error.txt").writeText(errorMsg)
                        } catch (e: Exception) {}
                        Log.e(TAG, "Install failed: $status - $message")
                        ctx.applicationContext.unregisterReceiver(this)
                    } else {
                        ctx.applicationContext.unregisterReceiver(this)
                    }
                }
            }
            val intentFilter = android.content.IntentFilter("com.hwnix.cash.ACTION_INSTALL_COMPLETE")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.applicationContext.registerReceiver(receiver, intentFilter)
            }
            
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller failed: ${e.message}", e)
            installApkLegacy(apkFile)
        }
    }

    /**
     * فحص وجود تحديث محمل محلياً بالفعل وإرجاع الملف واسم الإصدار إذا كان أعلى من الإصدار الحالي.
     */
    fun scanLocalUpdates(currentVersionCode: Int): Pair<File?, String> {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return Pair(null, "")
            val files = downloadsDir.listFiles() ?: return Pair(null, "")
            var highestVersionCode = currentVersionCode
            var bestApk: File? = null
            var bestVersionName = ""
            
            for (file in files) {
                if (file.isFile && (file.name.startsWith("hwnix-cash-v") || file.name.startsWith("sms-agent-v")) && file.name.endsWith(".apk")) {
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
            return Pair(bestApk, bestVersionName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan local updates: ${e.message}")
            return Pair(null, "")
        }
    }
}

