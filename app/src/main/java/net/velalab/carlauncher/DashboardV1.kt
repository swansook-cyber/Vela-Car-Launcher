package net.velalab.carlauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val navy = Color(0xFF071422)
private val card = Color(0xDD0B2035)
private val electric = Color(0xFF10AEFF)
private val muted = Color(0xFFA9C5DD)

@Composable
fun DashboardV1(gpsPermissionGranted: Boolean, onRequestGps: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var page by remember { mutableStateOf("Home") }
    var driving by remember { mutableStateOf(true) }
    val apps = remember { installedLaunchers(context) }
    val radio = remember(apps) { velaRadioApp(apps) }
    val tracker = remember { DriveTracker() }
    var telemetry by remember { mutableStateOf(DriveTelemetry()) }
    var position by remember { mutableStateOf<Location?>(null) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val weatherRepo = remember { WeatherRepository(context) }
    var weather by remember { mutableStateOf(weatherRepo.cached()) }
    var weatherState by remember { mutableStateOf(if (weather == null) "Unavailable" else "Cached") }
    var controller by remember { mutableStateOf(mediaController(context, radio?.packageName)) }
    val locationManager = remember { context.getSystemService(LocationManager::class.java) }

    fun save(newValue: LauncherSettings) { settings = newValue; store.save(newValue) }
    fun openNavigation() {
        val chosen = settings.navigationPackage.takeIf { it.isNotBlank() }
        if (chosen != null && launchPackage(context, chosen)) return
        val maps = apps.firstOrNull { it.packageName == "com.google.android.apps.maps" }
        if (maps != null && launchPackage(context, maps.packageName)) return
        apps.firstOrNull { it.label.contains("map", true) || it.label.contains("nav", true) }?.let { launchPackage(context, it.packageName) }
    }
    fun openQuick(key: String) {
        when (key) {
            "apps" -> page = "Apps"
            "maps" -> openNavigation()
            "music" -> apps.firstOrNull { it.label.contains("music", true) }?.let { launchPackage(context, it.packageName) }
            "velaredio" -> radio?.let { launchPackage(context, it.packageName) }
            "youtube" -> apps.firstOrNull { it.packageName == "com.google.android.youtube" || it.label.equals("YouTube", true) }?.let { launchPackage(context, it.packageName) }
            else -> launchPackage(context, key)
        }
    }
    fun appFor(key: String): LaunchableApp? = when (key) {
        "maps" -> apps.firstOrNull { it.packageName == "com.google.android.apps.maps" } ?: apps.firstOrNull { it.label.contains("map", true) }
        "music" -> apps.firstOrNull { it.label.contains("music", true) }
        "velaredio" -> radio
        "youtube" -> apps.firstOrNull { it.packageName == "com.google.android.youtube" || it.label.equals("YouTube", true) }
        else -> apps.firstOrNull { it.packageName == key }
    }

    DisposableEffect(gpsPermissionGranted) {
        if (!gpsPermissionGranted) onDispose { }
        else {
            val listener = android.location.LocationListener { fix -> if (tracker.onLocation(fix)) { position = fix; telemetry = tracker.snapshot() } }
            try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener) } catch (_: SecurityException) { }
            onDispose { locationManager.removeUpdates(listener) }
        }
    }
    LaunchedEffect(gpsPermissionGranted) {
        while (true) {
            telemetry = if (gpsPermissionGranted) tracker.snapshot() else DriveTelemetry(gpsStatus = "GPS permission required")
            controller = mediaController(context, radio?.packageName)
            clock = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(position?.latitude?.times(100)?.toInt(), position?.longitude?.times(100)?.toInt()) {
        val fix = position ?: return@LaunchedEffect
        if (!gpsPermissionGranted || kotlin.math.abs(System.currentTimeMillis() - fix.time) > 30_000) return@LaunchedEffect
        while (true) {
            weatherState = "Updating"
            val result = withContext(Dispatchers.IO) { runCatching { weatherRepo.fetch(fix.latitude, fix.longitude) } }
            result.onSuccess { weather = it; weatherState = "Live" }.onFailure { weatherState = if (weather == null) "Offline / Unavailable" else "Offline • cached" }
            delay(30 * 60 * 1000L)
        }
    }

    val background = if (settings.lightTheme) Color(0xFFE5F2FB) else navy
    val foreground = if (settings.lightTheme) Color(0xFF061729) else Color.White
    MaterialTheme(colorScheme = if (settings.lightTheme) lightColorScheme(primary = Color(0xFF0077BC)) else darkColorScheme(primary = electric)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(background)) {
            if (!settings.lightTheme) {
                Image(painterResource(R.drawable.dashboard_road), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color(0x99020B18)))
            }
            val compact = maxWidth < 850.dp || maxHeight < 460.dp
            val sidebarWidth = if (compact) 74.dp else 106.dp
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(sidebarWidth).fillMaxHeight().background(Color(0xFF061321)).verticalScroll(rememberScrollState()).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("V", color = electric, fontSize = if (compact) 32.sp else 46.sp, fontWeight = FontWeight.Black)
                    listOf(Triple("Home", Icons.Default.Home, "Home"), Triple("Navigation", Icons.Default.Navigation, "Navigation"), Triple("Music", Icons.Default.MusicNote, "Music"), Triple("Radio", Icons.Default.Radio, "Radio"), Triple("Apps", Icons.Default.Apps, "Apps"), Triple("Settings", Icons.Default.Settings, "Settings")).forEach { (label, icon, target) ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp).background(if (page == target) Color(0xFF064780) else Color.Transparent, RoundedCornerShape(14.dp)).clickable {
                            page = target
                            if (target == "Music") openQuick("music")
                            if (target == "Radio") openQuick("velaredio")
                        }.heightIn(min = if (compact) 58.dp else 72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(icon, label, tint = if (page == target) electric else Color.White, modifier = Modifier.size(if (compact) 24.dp else 29.dp))
                            Text(label, color = Color.White, fontSize = if (compact) 9.sp else 11.sp, maxLines = 1)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 9.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.vela_brand), "Vela Car Launcher", Modifier.size(if (compact) 48.dp else 62.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(8.dp))
                        Column { Text("VELA CAR", color = foreground, fontWeight = FontWeight.Black, fontSize = if (compact) 20.sp else 28.sp); Text("LAUNCHER", color = electric, fontSize = 11.sp, letterSpacing = 2.sp) }
                        Spacer(Modifier.weight(1f))
                        Text("GPS: ${telemetry.gpsStatus}  •  ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(clock))}  ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(clock))}", color = foreground, fontSize = if (compact) 11.sp else 14.sp, maxLines = 1)
                    }
                    when (page) {
                        "Settings" -> SettingsPage(settings, apps, ::save, foreground)
                        "Apps" -> AppsPage(apps, ::openQuick)
                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { driving = true; page = "Home" }, colors = ButtonDefaults.buttonColors(containerColor = if (driving && page == "Home") electric else card, contentColor = Color.White), modifier = Modifier.heightIn(min = 48.dp)) { Text("Driving") }
                                Button(onClick = { driving = false; page = "Navigation" }, colors = ButtonDefaults.buttonColors(containerColor = if (!driving || page == "Navigation") electric else card, contentColor = Color.White), modifier = Modifier.heightIn(min = 48.dp)) { Text("Navigation") }
                                if (page == "Navigation") Button(onClick = ::openNavigation, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open navigation app") }
                            }
                            if (compact) {
                                DrivePanel(telemetry, settings.miles, gpsPermissionGranted, onRequestGps, foreground, Modifier.fillMaxWidth().height(280.dp))
                                WeatherCard(weather, weatherState, foreground, Modifier.fillMaxWidth().height(150.dp))
                                RadioCard(radio, controller, foreground, Modifier.fillMaxWidth().height(150.dp), onOpen = { openQuick("velaredio") })
                            } else {
                                Row(Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DrivePanel(telemetry, settings.miles, gpsPermissionGranted, onRequestGps, foreground, Modifier.weight(1.55f).fillMaxHeight())
                                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        WeatherCard(weather, weatherState, foreground, Modifier.weight(1f).fillMaxWidth())
                                        RadioCard(radio, controller, foreground, Modifier.weight(1f).fillMaxWidth(), onOpen = { openQuick("velaredio") })
                                    }
                                }
                            }
                            val visible = settings.quickApps.filter { it == "apps" || appFor(it) != null }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                visible.forEach { key ->
                                    QuickApp(key, appFor(key), onClick = { openQuick(key) }, Modifier.width(if (compact) 108.dp else 145.dp).height(if (compact) 83.dp else 110.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun DrivePanel(data: DriveTelemetry, miles: Boolean, permitted: Boolean, onRequest: () -> Unit, foreground: Color, modifier: Modifier) {
    Row(modifier.background(card, RoundedCornerShape(22.dp)).border(1.dp, electric.copy(alpha = .7f), RoundedCornerShape(22.dp)).padding(12.dp)) {
        Column(Modifier.weight(1.4f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            GpsSpeedometer(if (miles) data.speedKmh?.times(.621371f) else data.speedKmh, data.gpsStatus, Modifier.weight(1f).fillMaxWidth(), if (miles) "mph" else "km/h")
            if (!permitted) TextButton(onClick = onRequest) { Text("Allow GPS") }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
            Metric("GPS accuracy", data.accuracyMetres?.let { "${it.toInt()} m" } ?: "—", foreground)
            Metric("Direction", data.bearingDegrees?.let(::compass) ?: "—", foreground)
            Metric("Trip distance", if (data.durationMillis > 0) String.format(Locale.US, "%.1f km", data.distanceKm) else "—", foreground)
            Metric("Trip time", if (data.durationMillis > 0) "${data.durationMillis / 3_600_000}h ${(data.durationMillis / 60_000) % 60}m" else "—", foreground)
        }
    }
}

private fun compass(degrees: Float): String = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[(degrees / 45f + .5f).toInt().mod(8)]

@Composable private fun Metric(label: String, value: String, foreground: Color) { Column { Text(label, color = muted, fontSize = 12.sp); Text(value, color = foreground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) } }

@Composable private fun WeatherCard(data: WeatherData?, state: String, foreground: Color, modifier: Modifier) {
    Column(modifier.background(card, RoundedCornerShape(22.dp)).border(1.dp, electric.copy(alpha = .45f), RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("WEATHER", color = electric, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(if (data == null) state else "${data.temperature.toInt()}°C  ${weatherDescription(data.code)}", color = foreground, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(if (data == null) "Waiting for GPS and network" else "Tomorrow ${data.tomorrowHigh?.toInt() ?: "—"}° / ${data.tomorrowLow?.toInt() ?: "—"}°  •  $state  •  ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(data.updatedAt))}", color = muted, fontSize = 12.sp)
    }
}

@Composable private fun RadioCard(app: LaunchableApp?, controller: android.media.session.MediaController?, foreground: Color, modifier: Modifier, onOpen: () -> Unit) {
    Column(modifier.background(card, RoundedCornerShape(22.dp)).border(1.dp, Color(0xFFEC9C4E).copy(alpha = .7f), RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("VELAREDIO", color = electric, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(controller?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: if (app == null) "Not installed" else "Open VelaRedio", color = foreground, fontSize = 20.sp, maxLines = 1)
        if (mediaCanControl(controller)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val actions = controller?.playbackState?.actions ?: 0L
                if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) IconButton(onClick = { controller?.transportControls?.skipToPrevious() }) { Icon(Icons.Default.SkipPrevious, "Previous") }
                if (actions and PlaybackState.ACTION_PLAY != 0L) IconButton(onClick = { controller?.transportControls?.play() }) { Icon(Icons.Default.PlayArrow, "Play") }
                if (actions and PlaybackState.ACTION_PAUSE != 0L) IconButton(onClick = { controller?.transportControls?.pause() }) { Icon(Icons.Default.Pause, "Pause") }
                if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) IconButton(onClick = { controller?.transportControls?.skipToNext() }) { Icon(Icons.Default.SkipNext, "Next") }
            }
        } else if (app != null) Button(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open VelaRedio") }
        else Text("Install VelaRedio to connect", color = muted, fontSize = 12.sp)
    }
}

@Composable private fun QuickApp(key: String, app: LaunchableApp?, onClick: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val image = remember(app) { app?.let { runCatching { context.packageManager.getApplicationIcon(it.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull() } }
    Column(modifier.background(card, RoundedCornerShape(18.dp)).border(1.dp, electric.copy(alpha = .5f), RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (image != null) androidx.compose.foundation.Image(image, app?.label, Modifier.size(48.dp), contentScale = ContentScale.Fit)
        else Icon(Icons.Default.Apps, null, tint = electric, modifier = Modifier.size(43.dp))
        Text(app?.label ?: if (key == "apps") "Apps" else key, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable private fun AppsPage(apps: List<LaunchableApp>, onOpen: (String) -> Unit) {
    Text("Installed apps", color = electric, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    apps.forEach { app -> ListItem(headlineContent = { Text(app.label) }, supportingContent = { Text(app.packageName) }, modifier = Modifier.clickable { onOpen(app.packageName) }.heightIn(min = 56.dp)) }
}

@Composable private fun SettingsPage(value: LauncherSettings, apps: List<LaunchableApp>, save: (LauncherSettings) -> Unit, foreground: Color) {
    val context = LocalContext.current
    Text("Settings", color = electric, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically) { Text("Light theme", color = foreground); Switch(value.lightTheme, { save(value.copy(lightTheme = it)) }) }
    Row(verticalAlignment = Alignment.CenterVertically) { Text("Speed in mph", color = foreground); Switch(value.miles, { save(value.copy(miles = it)) }) }
    Text("Default navigation app", color = foreground)
    val navigation = apps.filter { it.label.contains("map", true) || it.label.contains("nav", true) }
    navigation.forEach { app -> TextButton(onClick = { save(value.copy(navigationPackage = app.packageName)) }, modifier = Modifier.heightIn(min = 48.dp)) { Text((if (value.navigationPackage == app.packageName) "✓ " else "") + app.label) } }
    Text("Quick Apps • tap to show/hide; arrows reorder", color = foreground)
    val candidates = listOf("maps", "music", "velaredio", "youtube", "apps") + apps.map { it.packageName }
    candidates.distinct().forEach { key ->
        val app = apps.firstOrNull { it.packageName == key }
        val label = app?.label ?: key.replaceFirstChar { it.uppercase() }
        val index = value.quickApps.indexOf(key)
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(index >= 0, onCheckedChange = { checked -> save(value.copy(quickApps = if (checked) value.quickApps + key else value.quickApps - key)) })
            Text(label, color = foreground, modifier = Modifier.weight(1f))
            if (index > 0) IconButton(onClick = { val list = value.quickApps.toMutableList(); java.util.Collections.swap(list, index, index - 1); save(value.copy(quickApps = list)) }) { Icon(Icons.Default.ArrowUpward, "Move up") }
            if (index >= 0 && index < value.quickApps.lastIndex) IconButton(onClick = { val list = value.quickApps.toMutableList(); java.util.Collections.swap(list, index, index + 1); save(value.copy(quickApps = list)) }) { Icon(Icons.Default.ArrowDownward, "Move down") }
        }
    }
    Button(onClick = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Choose default home app / restore previous launcher") }
    Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Allow media session access") }
}
