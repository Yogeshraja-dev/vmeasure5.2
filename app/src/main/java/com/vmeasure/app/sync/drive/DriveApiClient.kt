package com.vmeasure.app.sync.drive

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class DriveApiClient(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    companion object {
        private const val FILES = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
    }

    @Serializable data class FileListResponse(val files: List<DriveFile> = emptyList())
    @Serializable data class DriveFile(val id: String, val name: String? = null, val createdTime: String? = null)

    suspend fun findOrCreateFolder(accessToken: String, folderName: String, parentId: String? = null): String {
        val nameEsc = folderName.replace("'", "\\'")
        val baseQ = "mimeType='application/vnd.google-apps.folder' and name='$nameEsc' and trashed=false"
        val q = if (parentId == null) baseQ else "$baseQ and '$parentId' in parents"
        val url = "$FILES?q=${enc(q)}&fields=files(id,name,createdTime)&pageSize=10"
        val res = get(accessToken, url)
        val existing = json.decodeFromString(FileListResponse.serializer(), res).files.firstOrNull()
        if (existing != null) return existing.id

        val parentsJson = if (parentId != null) ""","parents":["$parentId"]""" else ""
        val body = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder"$parentsJson}"""
        val created = postJson(accessToken, "$FILES?fields=id", body)
        return json.decodeFromString(DriveFile.serializer(), created).id
    }

    suspend fun listSubfoldersLatestFirst(accessToken: String, parentFolderId: String): List<DriveFile> {
        val q = "'$parentFolderId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val url = "$FILES?q=${enc(q)}&orderBy=createdTime desc&fields=files(id,name,createdTime)&pageSize=50"
        val res = get(accessToken, url)
        return json.decodeFromString(FileListResponse.serializer(), res).files
    }

    suspend fun listFilesLatestFirst(accessToken: String, folderId: String): List<DriveFile> {
        val q = "'$folderId' in parents and trashed=false"
        val url = "$FILES?q=${enc(q)}&orderBy=createdTime desc&fields=files(id,name,createdTime)&pageSize=50"
        val res = get(accessToken, url)
        return json.decodeFromString(FileListResponse.serializer(), res).files
    }

    suspend fun uploadJson(accessToken: String, folderId: String, fileName: String, bytes: ByteArray): String {
        val boundary = "b_${UUID.randomUUID()}"
        val metadata = """{"name":"$fileName","parents":["$folderId"],"mimeType":"application/json"}"""

        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\n".toByteArray())
            write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toByteArray())
            write("\r\n".toByteArray())

            write("--$boundary\r\n".toByteArray())
            write("Content-Type: application/json\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n".toByteArray())
            write("--$boundary--\r\n".toByteArray())
        }.toByteArray()

        val conn = (URL("$UPLOAD?uploadType=multipart&fields=id,createdTime").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            doOutput = true
        }
        conn.outputStream.use { it.write(body) }
        val res = read(conn)
        return json.decodeFromString(DriveFile.serializer(), res).id
    }

    suspend fun downloadBytes(accessToken: String, fileId: String): ByteArray {
        val conn = (URL("$FILES/$fileId?alt=media").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        if (code !in 200..299) throw RuntimeException("Download failed: HTTP $code ${conn.errorStream?.bufferedReader()?.readText()}")
        return conn.inputStream.use { it.readBytes() }
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    private fun get(token: String, url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        return read(conn)
    }

    private fun postJson(token: String, url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        return read(conn)
    }

    private fun read(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw RuntimeException("Drive API error: HTTP $code $text")
        return text
    }
}
