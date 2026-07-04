package com.hwnix.smsagent.core.di

import android.content.Context
import com.hwnix.smsagent.data.local.AppDatabase
import com.hwnix.smsagent.data.local.SessionManager
import com.hwnix.smsagent.data.remote.ApiClient
import com.hwnix.smsagent.data.remote.ApiService
import com.hwnix.smsagent.data.repository.AuthRepositoryImpl
import com.hwnix.smsagent.data.repository.DeviceRepositoryImpl
import com.hwnix.smsagent.data.repository.SmsRepositoryImpl
import com.hwnix.smsagent.domain.repository.AuthRepository
import com.hwnix.smsagent.domain.repository.DeviceRepository
import com.hwnix.smsagent.domain.repository.SmsRepository
import com.hwnix.smsagent.domain.usecase.*



import com.hwnix.smsagent.manager.battery.BatteryManager
import com.hwnix.smsagent.manager.sim.SimManager
import com.hwnix.smsagent.manager.update.UpdateManager

// محدد الخدمات لتوفير وحقن الاعتمادات يدوياً بشكل آمن وتجنب تكرار التهيئة
object ServiceLocator {

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(appContext)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    val apiService: ApiService by lazy {
        ApiClient.getService(appContext)
    }

    val simManager: SimManager by lazy {
        SimManager(appContext)
    }

    val updateManager: UpdateManager by lazy {
        UpdateManager(appContext)
    }

    val batteryManager: BatteryManager by lazy {
        BatteryManager(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, sessionManager)
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepositoryImpl(apiService, sessionManager, database.smsDao())
    }

    val smsRepository: SmsRepository by lazy {
        SmsRepositoryImpl(apiService, sessionManager, database.smsDao())
    }

    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository, deviceRepository)
    }

    val registerUseCase: RegisterUseCase by lazy {
        RegisterUseCase(authRepository, deviceRepository)
    }

    val registerDeviceUseCase: RegisterDeviceUseCase by lazy {
        RegisterDeviceUseCase(deviceRepository)
    }

    val syncSimLinesUseCase: SyncSimLinesUseCase by lazy {
        SyncSimLinesUseCase(deviceRepository, simManager)
    }

    val checkUpdateUseCase: CheckUpdateUseCase by lazy {
        CheckUpdateUseCase(deviceRepository)
    }


}

