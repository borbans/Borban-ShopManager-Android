package de.borban.shopmanager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DropshippingLocalStateTest {
    @Test
    fun reconcileKeepsOnlyCurrentProcessingOrdersAndLeavesNewOrdersPending() {
        val initial = reconcileLocalTransfers(
            processingOrderIds = listOf("a", "b", "c"),
            locallyTransferredOrderIds = setOf("b", "obsolete"),
        )

        assertEquals(listOf("a", "c"), initial.pendingOrderIds)
        assertEquals(setOf("b"), initial.transferredOrderIds)

        val afterPortal = reconcileLocalTransfers(
            processingOrderIds = listOf("a", "b", "c", "new"),
            locallyTransferredOrderIds = initial.transferredOrderIds + initial.pendingOrderIds,
        )

        assertEquals(listOf("new"), afterPortal.pendingOrderIds)
        assertEquals(setOf("a", "b", "c"), afterPortal.transferredOrderIds)
    }

    @Test
    fun reconcileRemovesOrdersNoLongerInProcessing() {
        val state = reconcileLocalTransfers(
            processingOrderIds = listOf("still-processing"),
            locallyTransferredOrderIds = setOf("finished", "still-processing"),
        )

        assertEquals(0, state.pending)
        assertEquals(1, state.transferred)
        assertEquals(setOf("still-processing"), state.transferredOrderIds)
    }
}
