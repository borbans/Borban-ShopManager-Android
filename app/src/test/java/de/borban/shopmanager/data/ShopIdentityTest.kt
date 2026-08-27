package de.borban.shopmanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopIdentityTest {
    private fun shop(url: String, deviceId: String) = ShopConnection(
        shopId = "sw5-1",
        name = url,
        url = url,
        deviceId = deviceId,
        token = "token-$deviceId",
        currency = "EUR",
    )

    @Test
    fun sameShopIdOnDifferentDomainsRemainsStored() {
        val first = shop("https://master.example", "master-device")
        val satellite = shop("https://satellite.example", "satellite-device")

        val result = mergeShopConnection(mergeShopConnection(emptyList(), first), satellite)

        assertEquals(2, result.size)
        assertTrue(result.contains(first))
        assertTrue(result.contains(satellite))
    }

    @Test
    fun repairingSameDomainReplacesOnlyThatConnection() {
        val first = shop("HTTPS://SHOP.EXAMPLE/", "old-device")
        val other = shop("https://other.example", "other-device")
        val repaired = shop("https://shop.example", "new-device")

        val result = mergeShopConnection(listOf(first, other), repaired)

        assertEquals(2, result.size)
        assertTrue(result.contains(other))
        assertTrue(result.contains(repaired))
    }
}
