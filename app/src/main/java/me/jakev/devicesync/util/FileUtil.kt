package me.jakev.devicesync.util

import java.io.File
import java.security.MessageDigest

object FileUtil {

    /** Compute MD5 checksum of a file for change detection. */
    fun md5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Build the Firebase Storage path for a file. */
    fun buildRemotePath(uid: String, pairId: String, relativePath: String): String =
        "syncs/$uid/$pairId/$relativePath"

    /** Returns the relative path of a file from its base folder. */
    fun relativePath(file: File, baseFolder: File): String =
        baseFolder.toURI().relativize(file.toURI()).path

    /** Walk a folder recursively and return all files (not directories). */
    fun walkFolder(folder: File): List<File> {
        if (!folder.exists() || !folder.isDirectory) return emptyList()
        return folder.walkTopDown().filter { it.isFile }.toList()
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
