package de.borban.shopmanager.data

data class TransferCounts(val pending: Int, val transferred: Int)

fun transferCounts(processingIds: List<String>, transferredIds: Set<String>): TransferCounts {
    val processing = processingIds.distinct()
    val transferred = processing.count { it in transferredIds }
    return TransferCounts(pending = processing.size - transferred, transferred = transferred)
}

fun snapshotPendingIds(processingIds: List<String>, transferredIds: Set<String>): List<String> =
    processingIds.distinct().filterNot { it in transferredIds }

fun normalizePortalUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val lowerRaw = trimmed.lowercase()
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed) && !lowerRaw.startsWith("https://") && !lowerRaw.startsWith("http://")) return ""
    val candidate = if ("://" in trimmed) trimmed else "https://$trimmed"
    val lower = candidate.lowercase()
    return if (lower.startsWith("https://") || lower.startsWith("http://")) candidate else ""
}
