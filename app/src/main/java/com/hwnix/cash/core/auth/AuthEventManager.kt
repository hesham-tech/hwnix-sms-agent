package com.hwnix.cash.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * مدير أحداث الأمان والجلسات - يرسل إشعارات عند انتهاء صلاحية التوكن أو إلغاء الجلسة من السيرفر
 */
object AuthEventManager {

    private val _sessionExpiredEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<String> = _sessionExpiredEvents.asSharedFlow()

    fun emitSessionExpired(reason: String = "انتهت صلاحية جلسة العمل أو تم إلغاؤها من جهاز آخر.") {
        _sessionExpiredEvents.tryEmit(reason)
    }
}
