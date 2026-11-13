package com.zinerom.info.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class ZineFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: "ZineROM"
        val body = message.data["message"] ?: message.notification?.body.orEmpty()
        val prefs = getSharedPreferences("zinerom_notifications", MODE_PRIVATE)
        val history = try { org.json.JSONArray(prefs.getString("history", "[]")) } catch (_: Exception) { org.json.JSONArray() }
        val item = JSONObject().put("title", title).put("message", body)
            .put("type", message.data["type"] ?: "CUSTOM")
            .put("created_at", System.currentTimeMillis())
            .put("target_version", message.data["target_version"] ?: "")
            .put("button_text", message.data["button_text"] ?: "")
            .put("button_url", message.data["button_url"] ?: "")
        val updated = org.json.JSONArray().put(item)
        for (i in 0 until minOf(history.length(), 49)) updated.put(history.getJSONObject(i))
        prefs.edit().putString("history", updated.toString()).apply()

        val channelId = "zinerom_updates"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "ZineROM updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = android.app.Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
