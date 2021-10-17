package com.hossein.runningapp.other

import android.graphics.Color

object Constants {

    const val RUNNING_DATABASE_NAME = "running_db"

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_SHOW_TRACKING_FRAGMENT = "ACTION_SHOW_TRACKING_FRAGMENT"

    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_INTERVAL = 2000L

    const val SHARED_PREFERENCE_NAME = "shared_preference_name"
    const val KEY_NAME = "KEY_NAME"
    const val KEY_WEIGHT = "KEY_WEIGHT"
    const val KEY_FIRST_TIME_TOGGLE = "KEY_FIRST_TIME_TOGGLE"

    const val POLYLINE_COLOR = Color.BLUE
    const val POLYLINE_WIDTH = 10f
    const val MAP_ZOOM = 18f
    const val TIMER_UPDATE_INTERVAL = 50L

    const val NOTIFICATION_ID = 1
    const val NOTIFICATION_CHANNEL_NAME = "Tracking"
    const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
}