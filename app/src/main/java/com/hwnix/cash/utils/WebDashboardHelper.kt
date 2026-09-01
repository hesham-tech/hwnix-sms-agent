package com.hwnix.cash.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hwnix.cash.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WebDashboardHelper {
    suspend fun openMagicLink(context: Context, apiService: ApiService) {
        try {
            val response = apiService.generateMagicLink()
            if (response.isSuccessful) {
                val token = response.body()?.getAsJsonObject("data")?.get("token")?.asString
                if (token != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bill.hwnix.com/magic-login?token=$token"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "لم يتم العثور على التوكن في الاستجابة", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فشل إنشاء رابط الدخول الآمن", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "تعذر الاتصال بالسيرفر", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
