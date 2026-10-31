package net.velalab.carlauncher

import org.junit.Assert.*
import org.junit.Test

class GpsPolicyTest {
    @Test fun convertsRealGpsSpeed() { assertEquals(72f, GpsPolicy.speedKmh(20f, 5f, 1000, 1000)!!, .001f) }
    @Test fun clearsStaleAndInaccurateSpeed() {
        assertNull(GpsPolicy.speedKmh(20f, 5f, 1000, 7001))
        assertNull(GpsPolicy.speedKmh(20f, 90f, 1000, 1000))
        assertNull(GpsPolicy.speedKmh(null, 5f, 1000, 1000))
    }
    @Test fun convertsUnitsOnlyForDisplay() { assertEquals(62, GpsPolicy.display(100f, true)); assertEquals(100, GpsPolicy.display(100f, false)) }
}
