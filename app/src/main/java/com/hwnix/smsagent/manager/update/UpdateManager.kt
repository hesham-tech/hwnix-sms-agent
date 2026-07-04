package com.hwnix.smsagent.manager.update

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
                if (file.isFile && file.name.startsWith("sms-agent-v") && file.name.endsWith(".apk")) {
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
                val downloadsDir = File(baseDir, "HWNixSMSAgent")
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
            val apkFile = File(downloadsDir, "sms-agent-v$versionName.apk")
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

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // منح صلاحية القراءة صراحة لجميع الحزم المتنصتة على هذا الحدث (لحل مشكلة فشل التثبيت أول مرة على أندرويد 9 وما دونه)
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
        } catch (e: Exception) {
            Log.e(TAG, "Install APK failed: ${e.message}", e)
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
            return Pair(bestApk, bestVersionName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan local updates: ${e.message}")
            return Pair(null, "")
        }
    }
}

