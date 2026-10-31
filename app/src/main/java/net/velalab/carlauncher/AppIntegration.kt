package net.velalab.carlauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService

class VelaNotificationListener : NotificationListenerService()

data class LaunchableApp(val packageName: String, val label: String)

fun installedLaunchers(context: Context): List<LaunchableApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(query, 0).map {
        LaunchableApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString())
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

fun velaRadioApp(apps: List<LaunchableApp>): LaunchableApp? = apps.firstOrNull {
    it.label.replace(" ", "").contains("velaredio", true) || it.label.replace(" ", "").contains("velaradio", true)
}

fun launchPackage(context: Context, packageName: String): Boolean = try {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
} catch (_: Exception) { false }

fun mediaController(context: Context, packageName: String?): MediaController? {
    if (packageName == null) return null
    return try {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, VelaNotificationListener::class.java)
        manager.getActiveSessions(listener).firstOrNull { it.packageName == packageName }
    } catch (_: SecurityException) { null }
}

fun mediaCanControl(controller: MediaController?): Boolean = controller?.playbackState?.let {
    val actions = it.actions
    (actions and (android.media.session.PlaybackState.ACTION_PLAY or android.media.session.PlaybackState.ACTION_PAUSE)) != 0L
} == true
