package de.borban.shopmanager.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.borban.shopmanager.MainActivity
import de.borban.shopmanager.data.Repository
import de.borban.shopmanager.data.ShopConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class PushTarget(val deviceId: String, val orderId: String)

object PushNavigation {
    var target by mutableStateOf<PushTarget?>(null)
        private set

    fun openFromIntent(intent: Intent?) {
        val deviceId = intent?.getStringExtra("bsm_device_id").orEmpty()
        val orderId = intent?.getStringExtra("bsm_order_id").orEmpty()
        if (deviceId.isNotBlank() && orderId.isNotBlank()) target = PushTarget(deviceId, orderId)
    }

    fun consume() { target = null }
}

object PushCoordinator {
    private const val PREFS = "borban_push_preferences"

    fun channelId(shop: ShopConnection): String = channelId(shop.deviceId)
    fun channelId(deviceId: String): String = "orders_${deviceId.lowercase()}"

    fun ensureChannels(context: Context, shops: List<ShopConnection>) {
        shops.forEach { ensureShopChannel(context, it) }
    }

    fun ensureShopChannel(context: Context, shop: ShopConnection) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId(shop), "${shop.name} · Bestellungen", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Neue Bestellungen aus ${shop.name}"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun openChannelSettings(context: Context, shop: ShopConnection) {
        ensureShopChannel(context, shop)
        context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId(shop))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }


    fun sendTestNotification(context: Context, shop: ShopConnection) {
        ensureShopChannel(context, shop)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, shop.deviceId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            NotificationCompat.Builder(context, channelId(shop))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("${shop.name} · Testbenachrichtigung")
                .setContentText("So klingt eine neue Bestellung aus diesem Shop.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setGroup("orders_${shop.deviceId}")
                .build(),
        )
    }

    fun isPushEnabled(context: Context, shop: ShopConnection): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled_${shop.deviceId}", true)

    fun setPushEnabled(context: Context, shop: ShopConnection, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled_${shop.deviceId}", enabled).apply()
        ensureShopChannel(context, shop)
        if (!enabled) {
            CoroutineScope(Dispatchers.IO).launch { Repository(context).setPushToken(shop, "") }
            return
        }
        registerCurrentTokenForShop(context, shop)
    }

    fun registerCurrentToken(context: Context) {
        ensureChannels(context, Repository(context).shops())
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> registerToken(context, token) }
    }

    fun registerCurrentTokenForShop(context: Context, shop: ShopConnection) {
        if (!isPushEnabled(context, shop) || FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch { Repository(context).setPushToken(shop, token) }
        }
    }

    fun registerToken(context: Context, token: String) {
        val repo = Repository(context)
        CoroutineScope(Dispatchers.IO).launch {
            repo.shops().forEach { shop ->
                if (isPushEnabled(context, shop)) repo.setPushToken(shop, token)
            }
        }
    }

    fun onShopPaired(context: Context, shop: ShopConnection) {
        ensureShopChannel(context, shop)
        registerCurrentTokenForShop(context, shop)
    }

    fun notificationBody(data: Map<String, String>): String {
        val amount = data["amount"]?.toDoubleOrNull()
        val currency = data["currency"].orEmpty().ifBlank { "EUR" }
        val positions = data["positions"]?.toIntOrNull() ?: 0
        val amountText = if (amount != null) runCatching {
            NumberFormat.getCurrencyInstance(Locale.GERMANY).apply { this.currency = Currency.getInstance(currency) }.format(amount)
        }.getOrDefault(String.format(Locale.GERMANY, "%.2f %s", amount, currency)) else "Neue Bestellung"
        return if (positions > 0) "$amountText · $positions ${if (positions == 1) "Position" else "Positionen"}" else amountText
    }
}

class BorbanMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { PushCoordinator.registerToken(applicationContext, token) }

    override fun onMessageReceived(message: RemoteMessage) {
        val deviceId = message.data["deviceId"].orEmpty()
        val orderId = message.data["orderId"].orEmpty()
        val shop = Repository(applicationContext).shopByDeviceId(deviceId) ?: return
        if (!PushCoordinator.isPushEnabled(applicationContext, shop)) return

        PushCoordinator.ensureShopChannel(applicationContext, shop)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("bsm_device_id", deviceId)
            putExtra("bsm_order_id", orderId)
        }
        val requestCode = (deviceId + ":" + orderId).hashCode()
        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = getSystemService(NotificationManager::class.java)
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(
            notificationId,
            NotificationCompat.Builder(this, PushCoordinator.channelId(shop))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("${shop.name} · Neue Bestellung")
                .setContentText(PushCoordinator.notificationBody(message.data))
                .setStyle(NotificationCompat.BigTextStyle().bigText(PushCoordinator.notificationBody(message.data)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setGroup("orders_${shop.deviceId}")
                .setNumber(1)
                .build(),
        )
    }
}
