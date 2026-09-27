package com.deepak.automotive.core.car

import android.content.Context
import android.content.pm.PackageManager

/**
 * The Car API (android.car.*) only exists on Android Automotive OS devices.
 * Touching those classes on a phone throws NoClassDefFoundError, so we always check first.
 */
object CarAvailability {
    fun isAutomotive(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
}
