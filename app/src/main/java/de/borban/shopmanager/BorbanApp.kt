package de.borban.shopmanager

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import de.borban.shopmanager.push.PushCoordinator

class BorbanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val firebaseAppId = getString(R.string.firebase_app_id)
        val firebaseApiKey = getString(R.string.firebase_api_key)
        val firebaseProjectId = getString(R.string.firebase_project_id)
        val firebaseSenderId = getString(R.string.firebase_sender_id)
        if (firebaseAppId.isNotBlank() && firebaseApiKey.isNotBlank() && firebaseProjectId.isNotBlank() && firebaseSenderId.isNotBlank()) {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                    .setApplicationId(firebaseAppId)
                    .setApiKey(firebaseApiKey)
                    .setProjectId(firebaseProjectId)
                    .setGcmSenderId(firebaseSenderId)
                    .build())
            }
            PushCoordinator.registerCurrentToken(this)
        }
    }
}
