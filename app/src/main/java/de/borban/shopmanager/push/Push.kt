package de.borban.shopmanager.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.borban.shopmanager.MainActivity
import de.borban.shopmanager.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushCoordinator {
    fun registerCurrentToken(context:Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> CoroutineScope(Dispatchers.IO).launch { Repository(context).registerPushToken(token) } }
    }
}

class BorbanMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) { CoroutineScope(Dispatchers.IO).launch { Repository(applicationContext).registerPushToken(token) } }
    override fun onMessageReceived(message:RemoteMessage) {
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("orders","Neue Bestellungen",NotificationManager.IMPORTANCE_HIGH))
        val intent=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title=message.notification?.title ?: "Neue Bestellung"
        val body=message.notification?.body ?: "Eine neue Bestellung ist eingegangen."
        nm.notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),NotificationCompat.Builder(this,"orders").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi).build())
    }
}
