package de.borban.shopmanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DropshippingLogicTest {
    @Test
    fun transferCountsDeduplicateOrdersAndSeparatePendingFromTransferred() {
        val counts = transferCounts(
            processingIds = listOf("a", "a", "b", "c"),
            transferredIds = setOf("b", "outside"),
        )

        assertEquals(TransferCounts(pending = 2, transferred = 1), counts)
        assertEquals(listOf("a", "c"), snapshotPendingIds(listOf("a", "a", "b", "c"), setOf("b")))
    }

    @Test
    fun portalUrlAddsHttpsAndRejectsUnsupportedSchemes() {
        assertEquals("https://portal.example/path", normalizePortalUrl("portal.example/path"))
        assertEquals("https://secure.example", normalizePortalUrl("https://secure.example"))
        assertEquals("", normalizePortalUrl("javascript:alert(1)"))
    }
}
