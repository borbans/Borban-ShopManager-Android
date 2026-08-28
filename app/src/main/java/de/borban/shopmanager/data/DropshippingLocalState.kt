package de.borban.shopmanager.data

data class LocalDropshippingState(
    val pending: Int,
    val transferred: Int,
    val pendingOrderIds: List<String>,
    val transferredOrderIds: Set<String>,
)

fun reconcileLocalTransfers(
    processingOrderIds: List<String>,
    locallyTransferredOrderIds: Set<String>,
): LocalDropshippingState {
    val processing = processingOrderIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val current = processing.toSet()
    val transferred = locallyTransferredOrderIds
        .map { it.trim() }
        .filter { it.isNotEmpty() && it in current }
        .toSet()
    val pending = processing.filterNot { it in transferred }
    return LocalDropshippingState(pending.size, transferred.size, pending, transferred)
}
