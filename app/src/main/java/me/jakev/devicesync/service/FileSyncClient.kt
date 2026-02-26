package me.jakev.devicesync.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteFileEntry(
    val path: String,
    val size: Long,
    val modified: Long,
    val checksum: String
)

/**
 * Runs on the PARENT device. Connects to the child's [FileSyncServer] over LAN,
 * fetches the file manifest, and downloads any new or changed files into the
 * parent's destination folder.
 *
 * No cloud involved — pure local WiFi transfer.
 */
@Singleton
class FileSyncClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /**
     * Returns true if the child server is reachable.
     */
    fun ping(childIp: String, port: Int): Boolean {
        return try {
            val req = Request.Builder().url("http://$childIp:$port/ping").build()
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch the list of files available on the child device.
     */
    fun fetchManifest(childIp: String, port: Int): List<RemoteFileEntry> {
        val req = Request.Builder().url("http://$childIp:$port/manifest").build()
        val body = okHttpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Manifest fetch failed: ${response.code}")
            response.body?.string() ?: throw Exception("Empty manifest response")
        }
        val arr = JSONArray(body)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RemoteFileEntry(
                path = obj.getString("path"),
                size = obj.getLong("size"),
                modified = obj.getLong("modified"),
                checksum = obj.getString("checksum")
            )
        }
    }

    /**
     * Download a single file from the child device into the parent's destination folder.
     * Skips the download if the local file already exists with a matching checksum.
     *
     * @param childIp      LAN IP of the child device
     * @param port         Port of the child's FileSyncServer
     * @param entry        The remote file entry from the manifest
     * @param destFolder   Parent device's destination folder
     * @param localChecksum Checksum of the existing local file, or null if it doesn't exist
     * @return true if a download was performed, false if skipped
     */
    fun downloadFile(
        childIp: String,
        port: Int,
        entry: RemoteFileEntry,
        destFolder: File,
        localChecksum: String?
    ): Boolean {
        // Skip if unchanged
        if (localChecksum != null && localChecksum == entry.checksum) {
            Timber.v("FileSyncClient: skipping unchanged ${entry.path}")
            return false
        }

        val destFile = File(destFolder, entry.path)
        destFile.parentFile?.mkdirs()

        val encodedPath = java.net.URLEncoder.encode(entry.path, "UTF-8")
        val req = Request.Builder()
            .url("http://$childIp:$port/file?path=$encodedPath")
            .build()

        okHttpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download failed for ${entry.path}: ${response.code}")
            response.body?.byteStream()?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Empty body for ${entry.path}")
        }

        Timber.d("FileSyncClient: downloaded ${entry.path} (${entry.size} bytes)")
        return true
    }
}
