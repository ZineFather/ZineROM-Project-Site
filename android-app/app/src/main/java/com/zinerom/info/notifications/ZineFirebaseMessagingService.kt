package com.zinerom.info.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zinerom.info.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class ZineFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val preferences = getSharedPreferences("zinerom_notifications", MODE_PRIVATE)
        if (!preferences.getBoolean("enabled", true)) return

        val title = message.data["title"] ?: message.notification?.title ?: "ZineROM"
        val body = message.data["message"] ?: message.notification?.body.orEmpty()

        try {
            saveToHistory(preferences, message, title, body)
            showHeadsUpNotification(message, title, body)
            Log.i(TAG, "FCM notification posted: ${message.messageId ?: "unknown"}")
        } catch (error: Exception) {
            Log.e(TAG, "Unable to post FCM notification", error)
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

    private fun showHeadsUpNotification(message: RemoteMessage, title: String, body: String) {
        val channelId = "zinerom_alerts_v2"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "ZineROM Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Important ZineROM announcements and update alerts"
                        enableVibration(true)
                        enableLights(true)
                        setShowBadge(true)
                    }
                )
            }
        }

        val destination = message.data["button_url"]
            ?.takeIf { it.startsWith("https://") }
            ?: "https://zinefather.github.io/ZineROM-Project-Site/"
        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            Intent(Intent.ACTION_VIEW, Uri.parse(destination)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Status-bar icons must be transparent monochrome vectors, never the full-colour launcher icon.
        val largeIconSizePx = (64f * resources.displayMetrics.density).roundToInt()
        val largeIcon: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.zine_app_icon)
            ?.let { source ->
                Bitmap.createScaledBitmap(source, largeIconSizePx, largeIconSizePx, true).also {
                    if (it !== source) source.recycle()
                }
            }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_zinerom)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
