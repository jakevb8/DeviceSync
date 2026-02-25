package com.devicesync.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.devicesync.app.R
import com.devicesync.app.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SyncForegroundService : Service() {

    @Inject
    lateinit var workManager: WorkManager

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.devicesync.action.START_SYNC"
        const val ACTION_STOP = "com.devicesync.action.STOP_SYNC"
        const val EXTRA_PAIR_ID = "pair_id"
        const val EXTRA_FOLDER_PATH = "folder_path"

        fun startIntent(context: Context, pairId: String, folderPath: String) =
            Intent(context, SyncForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PAIR_ID, pairId)
                putExtra(EXTRA_FOLDER_PATH, folderPath)
            }

        fun stopIntent(context: Context) =
            Intent(context, SyncForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pairId = intent.getStringExtra(EXTRA_PAIR_ID) ?: return START_NOT_STICKY
                val folderPath = intent.getStringExtra(EXTRA_FOLDER_PATH) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, buildNotification("Sync active"))
                enqueueSyncWork(pairId, folderPath)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun enqueueSyncWork(pairId: String, folderPath: String) {
        val workRequest = SyncWorker.buildRequest(pairId, folderPath)
        workManager.enqueueUniquePeriodicWork(
            "${SyncWorker.WORK_NAME_PREFIX}$pairId",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Timber.d("SyncForegroundService: enqueued periodic sync for pair $pairId")
    }

    private fun buildNotification(status: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DeviceSync")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_sync)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Sync Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Background folder sync notifications" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
