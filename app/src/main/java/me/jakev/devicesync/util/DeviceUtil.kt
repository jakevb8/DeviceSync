package me.jakev.devicesync.util

import android.provider.Settings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Returns a stable device identifier (Android ID). */
    fun getDeviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    /** Returns a human-readable device name. */
    fun getDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model
        else "$manufacturer $model"
    }
}
