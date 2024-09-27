package com.akdag.inseminationtrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "insemination_channel"
    private const val CHANNEL_NAME = "Kuruya Çıkma Bildirimi"
    private const val CHANNEL_DESCRIPTION = "İneklerin kuruya çıkma tarihi yaklaştığında gelen bildirimler"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, cowTag: String, daysUntilDryingOff: Long) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setContentTitle("Kuruya Çıkma Tarihi Yaklaşıyor!")
                .setContentText("Küpe Numarası: $cowTag, Kuruya çıkarma tarihi yaklaşıyor!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), notification)
            }
        }
    }
}