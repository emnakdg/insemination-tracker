package com.akdag.inseminationtrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CowBirthReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val earTag = inputData.getString("earTag") ?: return Result.failure()

        // Bildirimi oluştur
        showNotification("Kuruya Çıkma Tarihi Yaklaşıyor", "Küpe Numarası: $earTag - Kuruya çıkarma tarihi yaklaşıyor!")
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notificationId = 1
            val channelId = "cow_birth_reminder"

            // Android 8.0 ve sonrası için bildirim kanalı oluştur
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Cow Birth Reminder"
                val descriptionText = "Kuruya çıkarma hatırlatıcı bildirimi"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            with(NotificationManagerCompat.from(applicationContext)) {
                notify(notificationId, builder.build())
            }
        }
    }
}
