package me.jakev.devicesync.service

import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

/**
 * Lightweight HTTP server that runs on the child device and serves its sync folder.
 * The parent device connects to this server over LAN to download files.
 *
 * Endpoints:
 *   GET /manifest          → JSON list of all files with name, size, lastModified, checksum
 *   GET /file?path=<rel>   → raw file bytes for the given relative path
 */
class FileSyncServer(
    port: Int,
    private val folderPath: String
) : NanoHTTPD(port) {

    private val baseFolder = File(folderPath)

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.uri == "/manifest" -> serveManifest()
                session.uri == "/file" -> serveFile(session.parameters["path"]?.firstOrNull())
                session.uri == "/ping" -> newFixedLengthResponse("pong")
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "FileSyncServer error")
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message ?: "Error"
            )
        }
    }

    private fun serveManifest(): Response {
        val files = walkFolder(baseFolder)
        val json = buildString {
            append("[")
            files.forEachIndexed { i, file ->
                val rel = relativePath(file, baseFolder)
                val checksum = computeMd5(file)
                if (i > 0) append(",")
                append("""{"path":${jsonString(rel)},"size":${file.length()},"modified":${file.lastModified()},"checksum":${jsonString(checksum)}}""")
            }
            append("]")
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveFile(relativePath: String?): Response {
        if (relativePath.isNullOrBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing path")
        }
        val file = File(baseFolder, relativePath)
        if (!file.exists() || !file.canonicalPath.startsWith(baseFolder.canonicalPath)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
        return newChunkedResponse(
            Response.Status.OK,
            "application/octet-stream",
            FileInputStream(file)
        ).also { it.addHeader("X-File-Size", file.length().toString()) }
    }

    private fun walkFolder(folder: File): List<File> {
        if (!folder.exists() || !folder.isDirectory) return emptyList()
        return folder.walkTopDown().filter { it.isFile }.toList()
    }

    private fun relativePath(file: File, base: File): String =
        base.toURI().relativize(file.toURI()).path

    private fun computeMd5(file: File): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) md.update(buf, 0, read)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun jsonString(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
