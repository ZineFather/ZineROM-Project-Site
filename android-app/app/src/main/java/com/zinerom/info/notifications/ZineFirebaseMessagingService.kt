package com.zinerom.info.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject

class ZineFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val preferences = getSharedPreferences("zinerom_notifications", MODE_PRIVATE)
        if (!preferences.getBoolean("enabled", true)) {
            Log.i(TAG, "Notification skipped because notifications are disabled in app settings")
            return
        }

        val title = message.data["title"]
            ?: message.notification?.title
            ?: "ZineROM"
        val body = message.data["message"]
            ?: message.notification?.body
            ?: ""

        try {
            saveToHistory(preferences, message, title, body)
            showNotification(message, title, body)
            Log.i(TAG, "FCM message displayed. messageId=${message.messageId ?: "unknown"}")
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process incoming FCM message", error)
        }
    }

    private fun saveToHistory(
        preferences: android.content.SharedPreferences,
        message: RemoteMessage,
        title: String,
        body: String
    ) {
        val history = try {
            JSONArray(preferences.getString("history", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        val item = JSONObject()
            .put("title", title)
            .put("message", body)
            .put("type", message.data["type"] ?: "CUSTOM")
            .put("created_at", System.currentTimeMillis())
            .put("target_version", message.data["target_version"] ?: "")
            .put("target_device", message.data["target_device"] ?: "")
            .put("button_text", message.data["button_text"] ?: "")
            .put("button_url", message.data["button_url"] ?: "")

        val updated = JSONArray().put(item)
        for (index in 0 until minOf(history.length(), 49)) {
            updated.put(history.getJSONObject(index))
        }
        preferences.edit().putString("history", updated.toString()).apply()
    }

    private fun showNotification(message: RemoteMessage, title: String, body: String) {
        val channelId = "zinerom_updates"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "ZineROM Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val destination = message.data["button_url"]
            ?.takeIf { it.startsWith("https://") }
            ?: "https://zinefather.github.io/ZineROM-Project-Site/"

        val openWebsite = Intent(Intent.ACTION_VIEW, Uri.parse(destination))
        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            openWebsite,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM registration token refreshed")
    }

    companion object {
        private const val TAG = "ZineFCM"
    }
}
