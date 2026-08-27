package de.borban.shopmanager

data class ShopSortItem(
    val id: String,
    val name: String,
    val orders: Int,
    val revenue: Double,
    val open: Int,
)

fun sortShopItems(items: List<ShopSortItem>, mode: String): List<ShopSortItem> = when (mode) {
    "revenue" -> items.sortedWith(compareByDescending<ShopSortItem> { it.revenue }.thenByDescending { it.orders }.thenBy { it.name.lowercase() })
    "open" -> items.sortedWith(compareByDescending<ShopSortItem> { it.open }.thenByDescending { it.orders }.thenBy { it.name.lowercase() })
    "az" -> items.sortedBy { it.name.lowercase() }
    else -> items.sortedWith(compareByDescending<ShopSortItem> { it.orders }.thenByDescending { it.revenue }.thenBy { it.name.lowercase() })
}

fun operationalStatus(orderStateTechnical: String, deliveryStateTechnical: String): String {
    val order = orderStateTechnical.lowercase()
    val delivery = deliveryStateTechnical.lowercase()
    return when {
        delivery == "shipped" -> "Versendet"
        order == "completed" -> "Erledigt"
        order == "cancelled" -> "Storniert"
        order == "in_progress" -> "In Bearbeitung"
        else -> "Offen"
    }
}
