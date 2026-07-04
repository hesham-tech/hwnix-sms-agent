package com.hwnix.smsagent

import android.app.Application
import android.util.Log

class SmsAgentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        com.hwnix.smsagent.core.di.ServiceLocator.initialize(this)
        Log.i("SmsAgentApp", "HWNix SMS Gateway Agent Application Initialized.")
    }

}
