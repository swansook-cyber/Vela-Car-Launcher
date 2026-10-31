package net.velalab.carlauncher

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {
    @Test fun orderAndVisibilityRoundTrip() {
        val selected = listOf("youtube", "maps", "apps")
        assertEquals(selected, QuickAppOrder.parse(QuickAppOrder.serialize(selected)))
        assertFalse(QuickAppOrder.parse(QuickAppOrder.serialize(selected)).contains("music"))
    }
    @Test fun duplicatesAreRemoved() { assertEquals(listOf("maps", "apps"), QuickAppOrder.parse("maps,maps,apps")) }
}
