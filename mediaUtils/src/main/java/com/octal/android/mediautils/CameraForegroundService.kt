package com.octal.android.mediautils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class CameraForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a persistent notification
        val notification = createNotification()
        startForeground(1, notification)

        // Your logic here (optional)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need binding here
    }

    private fun createNotification(): Notification {
        // Create a notification for the foreground service
        val channelId = "camera_service_channel"
        val channel = NotificationChannel(
            channelId, "Camera Service", NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Camera is Running")
            .setContentText("Taking a picture")
            .setSmallIcon(R.drawable.ic_camera_service)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup when service is stopped
    }

}