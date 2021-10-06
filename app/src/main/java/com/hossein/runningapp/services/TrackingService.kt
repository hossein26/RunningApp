package com.hossein.runningapp.services

import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.hossein.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import timber.log.Timber

class TrackingService: LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    Timber.d("start service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}