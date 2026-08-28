package de.borban.shopmanager.data

data class ApiEnvelope<T>(val ok: Boolean, val data: T? = null, val error: String? = null)
data class DeviceKeys(val deviceId: String, val token: String)
data class PrivacyInfo(val pushContainsPersonalData: Boolean, val customerDetailsDirectOnly: Boolean)
data class ShopMeta(val id: String, val name: String, val url: String, val currency: String, val apiVersion: Int, val privacy: PrivacyInfo)
data class PairResponse(val device: DeviceKeys, val shop: ShopMeta)
data class PairRequest(val pairingCode: String, val deviceName: String, val appVersion: String = "0.2.4")
data class ShopConnection(val shopId: String, val name: String, val url: String, val deviceId: String, val token: String, val currency: String)
data class DayStats(val orders: Int = 0, val revenue: Double = 0.0)
data class Dashboard(
    val today: DayStats,
    val yesterday: DayStats,
    val openOrders: Int,
    val processingOrders: Int = 0,
    val paidToday: Int,
    val dropshipPending: Int = 0,
    val dropshipTransferred: Int = 0,
    val timezone: String,
    val generatedAt: String,
)
data class StateLabel(val technical: String = "", val label: String = "")
data class OrderSummary(val id:String,val number:String,val date:String,val amount:Double,val currency:String,val positions:Int,val customer:String,val orderState:StateLabel,val paymentState:StateLabel,val deliveryState:StateLabel,val paymentMethod:String,val shippingMethod:String)
data class Address(val company:String="",val firstName:String="",val lastName:String="",val street:String="",val zipcode:String="",val city:String="",val country:String="",val phone:String="")
data class CustomerDetail(val firstName:String="",val lastName:String="",val email:String="",val customerNumber:String="",val billing:Address?=null,val shipping:Address?=null)
data class OrderItem(val id:String,val label:String,val quantity:Int,val unitPrice:Double,val totalPrice:Double,val productNumber:String,val image:String?=null)
data class OrderDetail(val id:String,val number:String,val date:String,val amount:Double,val amountNet:Double,val shippingTotal:Double,val currency:String,val orderState:StateLabel,val paymentState:StateLabel,val deliveryState:StateLabel,val paymentMethod:String,val shippingMethod:String,val trackingCodes:List<String> = emptyList(),val customerComment:String="",val items:List<OrderItem> = emptyList(),val customer:CustomerDetail?=null)
data class TransitionRequest(val group:String,val action:String)
data class PushTokenRequest(val token:String)
data class DropshippingState(
    val processing:Int=0,
    val pending:Int=0,
    val transferred:Int=0,
    val pendingOrderIds:List<String> = emptyList(),
    val processingOrderIds:List<String> = emptyList(),
)

data class StatSummary(val orders:Int=0,val revenue:Double=0.0,val averageOrderValue:Double=0.0,val openOrders:Int=0)
data class StatComparison(val revenuePercent:Double?=null,val ordersPercent:Double?=null,val averageOrderValuePercent:Double?=null)
data class StatBucket(val key:String,val label:String,val revenue:Double=0.0,val orders:Int=0)
data class StatisticsRange(
    val range:String="week",
    val label:String="",
    val timezone:String="Europe/Berlin",
    val generatedAt:String="",
    val summary:StatSummary=StatSummary(),
    val previous:StatSummary=StatSummary(),
    val comparison:StatComparison=StatComparison(),
    val buckets:List<StatBucket> = emptyList(),
    val previousBuckets:List<StatBucket>? = null
)
