package net.velalab.carlauncher

import android.content.Context

data class LauncherSettings(
    val lightTheme: Boolean = false,
    val miles: Boolean = false,
    val navigationPackage: String = "",
    val quickApps: List<String> = listOf("maps", "music", "velaredio", "youtube", "apps")
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("vela_car_settings", Context.MODE_PRIVATE)
    fun load(): LauncherSettings = LauncherSettings(
        lightTheme = prefs.getBoolean("light_theme", false),
        miles = prefs.getBoolean("miles", false),
        navigationPackage = prefs.getString("navigation_package", "") ?: "",
        quickApps = QuickAppOrder.parse(prefs.getString("quick_apps", null))
    )
    fun save(value: LauncherSettings) {
        prefs.edit().putBoolean("light_theme", value.lightTheme).putBoolean("miles", value.miles)
            .putString("navigation_package", value.navigationPackage)
            .putString("quick_apps", QuickAppOrder.serialize(value.quickApps)).apply()
    }
}

object QuickAppOrder {
    fun parse(raw: String?): List<String> = (raw ?: "maps,music,velaredio,youtube,apps")
        .split(',').filter { it.isNotBlank() }.distinct()
    fun serialize(apps: List<String>): String = apps.filter { it.isNotBlank() }.distinct().joinToString(",")
}
