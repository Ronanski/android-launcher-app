package com.ronanski.launcher

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat

data class NotifItem(
    val key: String,
    val pkg: String,
    val title: String,
    val text: String,
    val time: Long,
    val intent: PendingIntent?,
    val clearable: Boolean,
    val autoCancel: Boolean,
)

/** Notifications shown in the Windows-style notification center. */
object NotificationStore {
    val items = mutableStateOf<List<NotifItem>>(emptyList())
    val connected = mutableStateOf(false)

    @Volatile
    var service: LauncherNotificationService? = null

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun openSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }

    fun dismiss(item: NotifItem) {
        try {
            service?.cancelNotification(item.key)
        } catch (e: Exception) {
        }
    }

    fun clearAll() {
        try {
            service?.cancelAllNotifications()
        } catch (e: Exception) {
        }
    }

    fun open(context: Context, item: NotifItem) {
        val pi = item.intent
        try {
            if (pi != null) {
                val options = if (Build.VERSION.SDK_INT >= 34) {
                    ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                } else {
                    null
                }
                pi.send(context, 0, null, null, null, null, options)
            } else {
                context.packageManager.getLaunchIntentForPackage(item.pkg)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } catch (e: Exception) {
            Actions.toast(context, "Can't open this notification")
        }
        if (item.autoCancel) dismiss(item)
    }
}

class LauncherNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationStore.service = this
        NotificationStore.connected.value = true
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (NotificationStore.service === this) NotificationStore.service = null
        NotificationStore.connected.value = false
        NotificationStore.items.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        NotificationStore.items.value = active.mapNotNull { sbn ->
            val n = sbn.notification ?: return@mapNotNull null
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return@mapNotNull null
            val extras = n.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) return@mapNotNull null
            NotifItem(
                key = sbn.key,
                pkg = sbn.packageName,
                title = title,
                text = text,
                time = sbn.postTime,
                intent = n.contentIntent,
                clearable = sbn.isClearable,
                autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
            )
        }.sortedByDescending { it.time }
    }
}
