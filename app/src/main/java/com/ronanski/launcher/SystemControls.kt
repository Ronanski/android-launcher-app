package com.ronanski.launcher

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToInt

/** Reads/changes system state for the Quick Settings flyout (only what Android allows apps to do). */
object SystemControls {
    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Actions.toast(context, "Can't open that setting on this device")
        }
    }

    fun openSettings(context: Context, action: String) = start(context, Intent(action))

    // ---- Wi-Fi / internet ----
    fun wifiOn(context: Context): Boolean = try {
        context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled == true
    } catch (e: Exception) {
        false
    }

    fun openWifi(context: Context) = openSettings(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS,
    )

    // ---- Bluetooth ----
    fun bluetoothOn(context: Context): Boolean = try {
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    } catch (e: Exception) {
        false
    }

    fun openBluetooth(context: Context) = openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)

    // ---- Airplane / location / battery saver ----
    fun airplaneOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    fun locationOn(context: Context): Boolean = try {
        val lm = context.getSystemService(LocationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) lm?.isLocationEnabled == true else true
    } catch (e: Exception) {
        false
    }

    fun batterySaverOn(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

    // ---- Do not disturb ----
    fun dndOn(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
    }

    fun hasDndAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun toggleDnd(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            Actions.toast(context, "Allow Do Not Disturb access for Pad Launcher")
            openSettings(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            return
        }
        nm.setInterruptionFilter(
            if (dndOn(context)) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
    }

    // ---- System settings writes (rotation, brightness) ----
    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    fun requestWrite(context: Context) {
        Actions.toast(context, "Allow \"Modify system settings\" for Pad Launcher")
        start(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    fun autoRotateOn(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    fun toggleAutoRotate(context: Context) {
        if (!canWrite(context)) return requestWrite(context)
        try {
            Settings.System.putInt(
                context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (autoRotateOn(context)) 0 else 1,
            )
        } catch (e: Exception) {
        }
    }

    /**
     * Since Android 11 the int SCREEN_BRIGHTNESS setting is 0..255 on most devices, but some ROMs
     * (including MIUI/HyperOS) store larger values. Pick the scale from the current value.
     */
    private fun maxBrightness(context: Context): Int {
        val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
        if (current <= 255) return 255
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android")
        val configured = if (id != 0) res.getInteger(id) else 255
        return listOf(configured, 2047, 4095, 8191, 16383).firstOrNull { it >= current } ?: current
    }

    fun brightness(context: Context): Float {
        // Prefer the float setting (0..1) when the ROM provides it.
        val f = try {
            Settings.System.getFloat(context.contentResolver, "screen_brightness_float")
        } catch (e: Exception) {
            -1f
        }
        if (f in 0f..1f && f > 0f) return f
        val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        return (value.toFloat() / maxBrightness(context)).coerceIn(0f, 1f)
    }

    fun setBrightness(context: Context, fraction: Float) {
        if (!canWrite(context)) return requestWrite(context)
        try {
            val cr = context.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            val max = maxBrightness(context)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (fraction * max).roundToInt().coerceIn(1, max))
        } catch (e: Exception) {
        }
    }

    // ---- Volume ----
    fun volume(context: Context): Float {
        val am = context.getSystemService(AudioManager::class.java) ?: return 0f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    fun setVolume(context: Context, fraction: Float) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).roundToInt().coerceIn(0, max), 0)
        } catch (e: Exception) {
        }
    }

    // ---- Flashlight ----
    val torchOn = mutableStateOf(false)
    private var torchId: String? = null
    private var torchRegistered = false

    fun initTorch(context: Context) {
        if (torchRegistered) return
        val cm = context.getSystemService(CameraManager::class.java) ?: return
        torchId = try {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
        try {
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchId) torchOn.value = enabled
                }
            }, Handler(Looper.getMainLooper()))
            torchRegistered = true
        } catch (e: Exception) {
        }
    }

    fun hasTorch(): Boolean = torchId != null

    fun toggleTorch(context: Context) {
        initTorch(context)
        val id = torchId ?: return Actions.toast(context, "No flashlight on this device")
        try {
            context.getSystemService(CameraManager::class.java)?.setTorchMode(id, !torchOn.value)
        } catch (e: Exception) {
            Actions.toast(context, "Flashlight is busy")
        }
    }
}
