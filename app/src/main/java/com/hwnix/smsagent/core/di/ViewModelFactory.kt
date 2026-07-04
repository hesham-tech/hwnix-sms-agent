package com.hwnix.smsagent.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hwnix.smsagent.presentation.auth.login.LoginViewModel
import com.hwnix.smsagent.presentation.auth.register.RegisterViewModel
import com.hwnix.smsagent.presentation.status.StatusViewModel

// كلاس مصنع لتوفير الاعتمادات وحقنها في الـ ViewModels يدوياً
class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    sessionManager = ServiceLocator.sessionManager,
                    loginUseCase = ServiceLocator.loginUseCase
                ) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(
                    sessionManager = ServiceLocator.sessionManager,
                    registerUseCase = ServiceLocator.registerUseCase
                ) as T
            }
            modelClass.isAssignableFrom(StatusViewModel::class.java) -> {
                StatusViewModel(
                    sessionManager = ServiceLocator.sessionManager,
                    deviceRepository = ServiceLocator.deviceRepository,
                    checkUpdateUseCase = ServiceLocator.checkUpdateUseCase,
                    syncSimLinesUseCase = ServiceLocator.syncSimLinesUseCase,
                    updateManager = ServiceLocator.updateManager,
                    batteryManager = ServiceLocator.batteryManager,
                    simManager = ServiceLocator.simManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
