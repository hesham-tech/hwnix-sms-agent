package com.hwnix.smsagent.data.remote

import android.content.Context
import com.hwnix.smsagent.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var apiService: ApiService? = null

    fun getService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            val sessionManager = SessionManager(context)
            
            // تسجيل تفاصيل الطلبات (Logging)
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // عميل OkHttpClient لإدخال ترويسات التوكن تلقائياً
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    
                    // إرفاق التوكن إن وجد
                    sessionManager.getAuthToken()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    requestBuilder.addHeader("Accept", "application/json")
                    
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .build()

            // جلب الـ Base URL من الإعدادات الافتراضية
            val baseUrl = sessionManager.getBaseUrl()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ApiService::class.java)
            apiService = service
            service
        }
    }

    /**
     * إعادة تعيين العميل (عند تعديل الـ API Base URL).
     */
    fun resetClient() {
        synchronized(this) {
            apiService = null
        }
    }
}
