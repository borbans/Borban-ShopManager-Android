package de.borban.shopmanager.data

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class Repository(private val context:Context) {
    private val store=ShopStore(context)
    fun shops()=store.list()
    suspend fun pair(url:String,code:String,deviceName:String):Result<ShopConnection> = runCatching {
        val r=ApiFactory.pairing(url).pair(PairRequest(code,deviceName))
        val p=r.data ?: error(r.error ?: "Kopplung fehlgeschlagen")
        val s=ShopConnection(p.shop.id,p.shop.name,p.shop.url,p.device.deviceId,p.device.token,p.shop.currency)
        store.save(s); s
    }
    fun remove(id:String)=store.remove(id)
    suspend fun dashboards():Map<ShopConnection,Dashboard> = coroutineScope {
        shops().map { s -> async { runCatching { ApiFactory.forShop(s).dashboard().data }.getOrNull()?.let { s to it } } }.awaitAll().filterNotNull().toMap()
    }
    suspend fun orders(shop:ShopConnection, search:String="") = ApiFactory.forShop(shop).orders(search=search).data.orEmpty()
    suspend fun order(shop:ShopConnection,id:String) = ApiFactory.forShop(shop).order(id).data
    suspend fun transition(shop:ShopConnection,id:String,group:String,action:String) = ApiFactory.forShop(shop).transition(id,TransitionRequest(group,action)).ok
    suspend fun registerPushToken(token:String) = coroutineScope { shops().map { s -> async { runCatching { ApiFactory.forShop(s).pushToken(PushTokenRequest(token)) } } }.awaitAll() }
}
