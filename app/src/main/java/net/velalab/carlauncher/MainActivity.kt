package net.velalab.carlauncher

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val bg = Color(0xFF07111F)
private val panel = Color(0xFF10253D)
private val blue = Color(0xFF199DFF)

class MainActivity : ComponentActivity() {
    private var gpsPermissionGranted by mutableStateOf(false)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        gpsPermissionGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gpsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        setContent { Dashboard(gpsPermissionGranted = gpsPermissionGranted, onRequestGps = { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) }
    }
}

@Composable
private fun Dashboard(gpsPermissionGranted: Boolean, onRequestGps: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vela_car", 0) }
    var driving by remember { mutableStateOf(prefs.getBoolean("driving", true)) }
    val locationManager = remember { context.getSystemService(LocationManager::class.java) }
    var gpsSpeed by remember { mutableStateOf<Float?>(null) }
    var lastGpsElapsed by remember { mutableLongStateOf(0L) }
    var gpsStatus by remember { mutableStateOf("Waiting for GPS") }
    val configuration = LocalConfiguration.current
    DisposableEffect(gpsPermissionGranted) {
        if (!gpsPermissionGranted) onDispose { }
        else {
            val listener = android.location.LocationListener { loc ->
                gpsSpeed = if (loc.hasSpeed() && loc.hasAccuracy() && loc.accuracy <= 80f) (loc.speed * 3.6f).coerceAtLeast(0f) else null
                lastGpsElapsed = SystemClock.elapsedRealtime()
                gpsStatus = if (gpsSpeed == null) "GPS speed unavailable" else "GPS SPEED"
            }
            try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener) } catch (_: Exception) { }
            onDispose { locationManager.removeUpdates(listener) }
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = blue, background = bg, surface = panel)) {
        Row(Modifier.fillMaxSize().background(bg)) {
            Column(Modifier.width(104.dp).fillMaxHeight().background(Color(0xFF0A1B2E)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("V", fontSize = 46.sp, fontWeight = FontWeight.Black, color = blue)
                listOf("Home" to Icons.Default.Home, "Navigation" to Icons.Default.Navigation, "Radio" to Icons.Default.Radio, "Apps" to Icons.Default.Apps, "Settings" to Icons.Default.Settings).forEach { (label, icon) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().background(if (label == "Home") panel else Color.Transparent, RoundedCornerShape(14.dp)).clickable {
                        if (label == "Navigation") { driving = false; prefs.edit().putBoolean("driving", false).apply() }
                        if (label == "Home") { driving = true; prefs.edit().putBoolean("driving", true).apply() }
                    }.padding(vertical = 10.dp)) {
                        Icon(icon, label, tint = if (label == "Home") blue else Color.White, modifier = Modifier.size(25.dp))
                        Text(label, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VELA CAR", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("LAUNCHER • V0.1", color = Color(0xFF90BDE1), fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { driving = true; prefs.edit().putBoolean("driving", true).apply() }, colors = ButtonDefaults.buttonColors(containerColor = if (driving) blue else panel)) { Text("Driving") }
                    Button(onClick = { driving = false; prefs.edit().putBoolean("driving", false).apply() }, colors = ButtonDefaults.buttonColors(containerColor = if (!driving) blue else panel)) { Text("Navigation") }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.55f).fillMaxHeight().background(panel, RoundedCornerShape(22.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (driving) {
                            GpsSpeedometer(gpsSpeed, if (!gpsPermissionGranted) "GPS permission required" else gpsStatus, Modifier.fillMaxWidth().weight(1f))
                            if (!gpsPermissionGranted) TextButton(onClick = onRequestGps) { Text("Allow GPS") }
                        } else {
                            Icon(Icons.Default.Navigation, null, tint = blue, modifier = Modifier.size(70.dp))
                            Text("Navigation Mode", color = Color.White, fontSize = 26.sp)
                            Text("Open Maps for turn-by-turn navigation", color = Color.LightGray, textAlign = TextAlign.Center)
                            Button(onClick = { try { val maps = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps"); if (maps != null) context.startActivity(maps) else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))) } catch (_: Exception) { } }) { Text("Open Maps") }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard("WEATHER", "Connect Vela Weather", Modifier.weight(1f))
                        InfoCard("VELAREDIO", "Media controls coming next", Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Maps", "Music", "VelaRedio", "YouTube", "Apps").forEach { title ->
                        Box(Modifier.weight(1f).height(86.dp).background(panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.background(panel, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text(title, color = blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(detail, color = Color.White, fontSize = 16.sp)
    }
}
