package de.borban.shopmanager.data

fun canonicalShopUrl(url: String): String = url.trim().trimEnd('/').lowercase()

fun ShopConnection.connectionKey(): String = canonicalShopUrl(url) + "|" + shopId.lowercase()

fun mergeShopConnection(existing: List<ShopConnection>, shop: ShopConnection): List<ShopConnection> {
    val key = shop.connectionKey()
    return existing.filterNot { it.connectionKey() == key } + shop
}
