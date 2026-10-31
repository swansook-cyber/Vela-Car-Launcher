package net.velalab.carlauncher

import android.location.Location
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.roundToInt

data class DriveTelemetry(
    val speedKmh: Float? = null,
    val accuracyMetres: Float? = null,
    val bearingDegrees: Float? = null,
    val distanceKm: Double = 0.0,
    val durationMillis: Long = 0,
    val gpsStatus: String = "Waiting for GPS"
)

/** Reject old fixes and old speed independently of Android's last-known-location cache. */
object GpsPolicy {
    const val STALE_MILLIS = 5_000L
    fun speedKmh(speedMps: Float?, accuracyMetres: Float?, fixElapsedMillis: Long, nowElapsedMillis: Long): Float? {
        if (speedMps == null || accuracyMetres == null || !speedMps.isFinite() ||
            speedMps < 0f || accuracyMetres > 80f || accuracyMetres < 0f ||
            nowElapsedMillis - fixElapsedMillis !in 0..STALE_MILLIS) return null
        return speedMps * 3.6f
    }
    fun display(speed: Float?, miles: Boolean): Int? = speed?.let { (if (miles) it * 0.621371f else it).roundToInt() }
}

class DriveTracker {
    private var previous: Location? = null
    private var distanceMetres = 0f
    private var movingMillis = 0L
    private var latest: Location? = null
    private var latestReceivedAt = 0L
    private var previousReceivedAt = 0L

    fun onLocation(location: Location): Boolean {
        if (!location.hasAccuracy() || location.accuracy > 80f) return false
        val now = SystemClock.elapsedRealtime()
        if (location.time <= 0 || abs(System.currentTimeMillis() - location.time) > 10_000L) return false
        previous?.let { old ->
            val delta = old.distanceTo(location)
            val elapsed = now - previousReceivedAt
            // Ignore GPS jumps and stationary jitter.
            if (delta in 3f..250f && elapsed in 1..10_000 && location.hasSpeed() && location.speed > 1f) {
                distanceMetres += delta
                movingMillis += elapsed
            }
        }
        previous = location
        previousReceivedAt = now
        latest = location
        latestReceivedAt = now
        return true
    }

    fun snapshot(now: Long = SystemClock.elapsedRealtime()): DriveTelemetry {
        val fix = latest
        val age = fix?.let { now - latestReceivedAt }
        val fresh = age != null && age in 0..GpsPolicy.STALE_MILLIS
        val speed = if (fresh && fix!!.hasSpeed()) GpsPolicy.speedKmh(fix.speed, fix.accuracy, latestReceivedAt, now) else null
        return DriveTelemetry(
            speedKmh = speed,
            accuracyMetres = if (fresh) fix!!.accuracy else null,
            bearingDegrees = if (fresh && fix!!.hasBearing()) fix.bearing else null,
            distanceKm = distanceMetres / 1000.0,
            durationMillis = movingMillis,
            gpsStatus = when { fix == null -> "Waiting for GPS"; !fresh -> "GPS signal lost"; speed == null -> "GPS speed unavailable"; else -> "GPS active" }
        )
    }
}
