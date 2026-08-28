package de.borban.shopmanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

private data class StoredTransferBatch(val connectionKey: String, val orderIds: List<String>, val startedAt: Long)
data class PendingTransferBatch(val connectionKey: String, val orderIds: List<String>, val startedAt: Long)

class ShopPreferences(context: Context) {
    private val gson = Gson()
    private val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "borban_shop_preferences",
        master,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun portalUrl(shop: ShopConnection): String = prefs.getString("portal_${shop.connectionKey()}", "").orEmpty()

    fun setPortalUrl(shop: ShopConnection, url: String) {
        prefs.edit().putString("portal_${shop.connectionKey()}", url.trim()).apply()
    }

    private fun transferredKey(shop: ShopConnection): String = "transferred_${shop.connectionKey()}"

    fun transferredOrderIds(shop: ShopConnection): Set<String> =
        prefs.getStringSet(transferredKey(shop), emptySet())?.toSet().orEmpty()

    fun reconcileTransfers(shop: ShopConnection, processingOrderIds: List<String>): LocalDropshippingState {
        val stored = transferredOrderIds(shop)
        val state = reconcileLocalTransfers(processingOrderIds, stored)
        if (state.transferredOrderIds != stored) {
            prefs.edit().putStringSet(transferredKey(shop), state.transferredOrderIds).apply()
        }
        return state
    }

    fun markTransferred(shop: ShopConnection, orderIds: List<String>) {
        val merged = transferredOrderIds(shop) + orderIds.map { it.trim() }.filter { it.isNotEmpty() }
        prefs.edit().putStringSet(transferredKey(shop), merged).apply()
    }

    fun beginTransfer(shop: ShopConnection, orderIds: List<String>) {
        val batch = StoredTransferBatch(shop.connectionKey(), orderIds.distinct(), System.currentTimeMillis())
        prefs.edit().putString("pending_transfer", gson.toJson(batch)).apply()
    }

    fun pendingTransfer(): PendingTransferBatch? {
        val raw = prefs.getString("pending_transfer", null) ?: return null
        return runCatching {
            gson.fromJson(raw, StoredTransferBatch::class.java)?.let {
                PendingTransferBatch(it.connectionKey, it.orderIds.distinct(), it.startedAt)
            }
        }.getOrNull()
    }

    fun clearPendingTransfer() {
        prefs.edit().remove("pending_transfer").apply()
    }
}
