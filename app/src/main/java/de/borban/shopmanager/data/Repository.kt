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
    fun remove(connectionKey:String)=store.remove(connectionKey)
    suspend fun dashboards():Map<ShopConnection,Dashboard> = coroutineScope {
        shops().map { s -> async { runCatching { ApiFactory.forShop(s).dashboard().data }.getOrNull()?.let { s to it } } }.awaitAll().filterNotNull().toMap()
    }
    suspend fun statistics(shop:ShopConnection, range:String) = ApiFactory.forShop(shop).statistics(range).data
    suspend fun statisticsAll(range:String): StatisticsRange? = coroutineScope {
        val all = shops().map { s -> async { runCatching { ApiFactory.forShop(s).statistics(range).data }.getOrNull() } }.awaitAll().filterNotNull()
        aggregateStatistics(range, all)
    }
    suspend fun orders(shop:ShopConnection, search:String="") = ApiFactory.forShop(shop).orders(search=search).data.orEmpty()
    suspend fun order(shop:ShopConnection,id:String) = ApiFactory.forShop(shop).order(id).data
    suspend fun transition(shop:ShopConnection,id:String,group:String,action:String) = ApiFactory.forShop(shop).transition(id,TransitionRequest(group,action)).ok
    suspend fun registerPushToken(token:String) = coroutineScope { shops().map { s -> async { runCatching { ApiFactory.forShop(s).pushToken(PushTokenRequest(token)) } } }.awaitAll() }
    suspend fun setPushToken(shop:ShopConnection, token:String) = runCatching { ApiFactory.forShop(shop).pushToken(PushTokenRequest(token)) }
    suspend fun dropshipping(shop:ShopConnection): DropshippingState = ApiFactory.forShop(shop).dropshipping().data ?: DropshippingState()
    suspend fun markTransferred(shop:ShopConnection, orderIds:List<String>): Int = ApiFactory.forShop(shop).markTransferred(MarkTransferredRequest(orderIds)).data?.get("updated") ?: 0
    fun shopByDeviceId(deviceId:String):ShopConnection? = shops().firstOrNull { it.deviceId == deviceId }
    fun shopByConnectionKey(connectionKey:String):ShopConnection? = shops().firstOrNull { it.connectionKey() == connectionKey }

    private fun aggregateStatistics(range:String, entries:List<StatisticsRange>): StatisticsRange? {
        if (entries.isEmpty()) return null
        val bucketMap = linkedMapOf<String, StatBucket>()
        entries.first().buckets.forEach { bucketMap[it.key] = it.copy(revenue = 0.0, orders = 0) }
        entries.forEach { stat ->
            stat.buckets.forEach { bucket ->
                val current = bucketMap[bucket.key] ?: StatBucket(bucket.key, bucket.label)
                bucketMap[bucket.key] = current.copy(
                    label = bucket.label,
                    revenue = current.revenue + bucket.revenue,
                    orders = current.orders + bucket.orders,
                )
            }
        }
        val summary = StatSummary(
            orders = entries.sumOf { it.summary.orders },
            revenue = entries.sumOf { it.summary.revenue },
            averageOrderValue = 0.0,
            openOrders = entries.sumOf { it.summary.openOrders },
        )
        val previous = StatSummary(
            orders = entries.sumOf { it.previous.orders },
            revenue = entries.sumOf { it.previous.revenue },
            averageOrderValue = 0.0,
            openOrders = 0,
        )
        val finalSummary = summary.copy(averageOrderValue = if (summary.orders > 0) summary.revenue / summary.orders else 0.0)
        val finalPrevious = previous.copy(averageOrderValue = if (previous.orders > 0) previous.revenue / previous.orders else 0.0)
        return StatisticsRange(
            range = range,
            label = entries.first().label,
            timezone = entries.first().timezone,
            generatedAt = entries.first().generatedAt,
            summary = finalSummary,
            previous = finalPrevious,
            comparison = StatComparison(
                revenuePercent = percentChange(finalSummary.revenue, finalPrevious.revenue),
                ordersPercent = percentChange(finalSummary.orders.toDouble(), finalPrevious.orders.toDouble()),
                averageOrderValuePercent = percentChange(finalSummary.averageOrderValue, finalPrevious.averageOrderValue),
            ),
            buckets = bucketMap.values.toList(),
            previousBuckets = aggregatePreviousBuckets(entries),
        )
    }

    private fun aggregatePreviousBuckets(entries:List<StatisticsRange>): List<StatBucket> {
        val size = entries.maxOfOrNull { it.previousBuckets.orEmpty().size } ?: 0
        if (size == 0) return emptyList()
        return (0 until size).map { index ->
            val samples = entries.mapNotNull { it.previousBuckets.orEmpty().getOrNull(index) }
            val first = samples.firstOrNull() ?: StatBucket(index.toString(), "")
            StatBucket(
                key = first.key,
                label = first.label,
                revenue = samples.sumOf { it.revenue },
                orders = samples.sumOf { it.orders },
            )
        }
    }

    private fun percentChange(current:Double, previous:Double):Double? {
        if (previous == 0.0) return if (current == 0.0) 0.0 else null
        return (((current - previous) / previous) * 1000.0).toInt() / 10.0
    }
}
