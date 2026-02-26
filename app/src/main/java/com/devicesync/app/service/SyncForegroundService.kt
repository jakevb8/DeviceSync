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
import com.google.firebase.auth.FirebaseAuth
import com.devicesync.app.R
import com.devicesync.app.data.model.DeviceRole
import com.devicesync.app.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service with two modes:
 *
 * CHILD mode: Starts the [FileSyncServer] (NanoHTTPD) so the parent device can
 *   pull files directly over LAN at any time. Keeps running in the background.
 *
 * PARENT mode: Enqueues the periodic [SyncWorker] which connects to the child's
 *   HTTP server and pulls new/changed files over WiFi.
 */
@AndroidEntryPoint
class SyncForegroundService : Service() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var auth: FirebaseAuth

    private var fileSyncServer: FileSyncServer? = null

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.devicesync.action.START_SYNC"
        const val ACTION_STOP  = "com.devicesync.action.STOP_SYNC"
        const val EXTRA_PAIR_ID     = "pair_id"
        const val EXTRA_FOLDER_PATH = "folder_path"
        const val EXTRA_ROLE        = "role"
        const val EXTRA_PORT        = "port"

        fun startIntent(context: Context, pairId: String, folderPath: String,
                        role: DeviceRole, port: Int = 8765) =
            Intent(context, SyncForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PAIR_ID, pairId)
                putExtra(EXTRA_FOLDER_PATH, folderPath)
                putExtra(EXTRA_ROLE, role.name)
                putExtra(EXTRA_PORT, port)
            }

        fun stopIntent(context: Context) =
            Intent(context, SyncForegroundService::class.java).apply { action = ACTION_STOP }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pairId     = intent.getStringExtra(EXTRA_PAIR_ID) ?: return START_NOT_STICKY
                val folderPath = intent.getStringExtra(EXTRA_FOLDER_PATH) ?: return START_NOT_STICKY
                val role       = DeviceRole.valueOf(intent.getStringExtra(EXTRA_ROLE) ?: "CHILD")
                val port       = intent.getIntExtra(EXTRA_PORT, 8765)

                when (role) {
                    DeviceRole.CHILD -> startChildServer(folderPath, port)
                    DeviceRole.PARENT -> startParentWorker(pairId, role)
                }
            }
            ACTION_STOP -> {
                stopChildServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    // ---- Child ----

    private fun startChildServer(folderPath: String, port: Int) {
        stopChildServer()
        fileSyncServer = FileSyncServer(port, folderPath).also { server ->
            server.start()
            Timber.d("SyncForegroundService: child HTTP server started on port $port")
        }
        startForeground(NOTIFICATION_ID, buildNotification("📤 Sharing folder on WiFi (port $port)"))
    }

    private fun stopChildServer() {
        fileSyncServer?.stop()
        fileSyncServer = null
    }

    // ---- Parent ----

    private fun startParentWorker(pairId: String, role: DeviceRole) {
        startForeground(NOTIFICATION_ID, buildNotification("🔄 Syncing from child device…"))
        val req = SyncWorker.buildPeriodicRequest(pairId, role)
        workManager.enqueueUniquePeriodicWork(
            "${SyncWorker.WORK_NAME_PREFIX}$pairId",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
        Timber.d("SyncForegroundService: enqueued parent pull worker for pair $pairId")
    }

    // ---- Helpers ----

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
        ).apply { description = "Background folder sync" }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopChildServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
