package com.example.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OsMedicineMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance(
                    applicationContext.getString(R.string.firestore_database_id)
                )
                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "fcmToken" to token,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Unable to persist refreshed FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new OS Medicine update."
        showLocalNotification(
            context = applicationContext,
            title = title,
            body = body,
            notificationId = (System.currentTimeMillis() % 100_000).toInt()
        )
    }

    companion object {
        private const val TAG = "OsMessagingService"
        const val CHANNEL_ID = "os_medicine_alerts"

        fun showLocalNotification(
            context: Context,
            title: String,
            body: String,
            notificationId: Int = 1001
        ) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "OS Medicine Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time prescription, pharmacy, and payment updates"
                }
                manager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Privacy: redact sensitive details on lock screen
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                manager.notify(notificationId, notification)
            } catch (se: SecurityException) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted", se)
            }
        }
    }
}
