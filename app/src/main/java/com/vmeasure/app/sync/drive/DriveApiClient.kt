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
        private const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
    }

    @Serializable
    data class FileListResponse(val files: List<DriveFile> = emptyList())

    @Serializable
    data class DriveFile(
        val id: String,
        val name: String? = null,
        val mimeType: String? = null,
        val createdTime: String? = null
    )

    suspend fun findOrCreateFolder(accessToken: String, folderName: String): String {
        val q = "mimeType='application/vnd.google-apps.folder' and name='${folderName.replace("'", "\\'")}' and trashed=false"
        val url = "$DRIVE_FILES?q=${encode(q)}&fields=files(id,name,mimeType)"
        val res = get(accessToken, url)
        val parsed = json.decodeFromString(FileListResponse.serializer(), res)
        val existing = parsed.files.firstOrNull()
        if (existing != null) return existing.id

        // create folder
        val createBody = """{"name":"$folderName","mimeType":"application/vnd.google-apps.folder"}"""
        val createRes = postJson(accessToken, "$DRIVE_FILES?fields=id", createBody)
        val created = json.decodeFromString(DriveFile.serializer(), createRes)
        return created.id
    }

    suspend fun uploadJsonFile(
        accessToken: String,
        folderId: String,
        fileName: String,
        jsonBytes: ByteArray
    ): String {
        val boundary = "boundary_${UUID.randomUUID()}"
        val metadata = """{"name":"$fileName","parents":["$folderId"],"mimeType":"application/json"}"""

        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\n".toByteArray())
            write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toByteArray())
            write("\r\n".toByteArray())

            write("--$boundary\r\n".toByteArray())
            write("Content-Type: application/json\r\n\r\n".toByteArray())
            write(jsonBytes)
            write("\r\n".toByteArray())

            write("--$boundary--\r\n".toByteArray())
        }.toByteArray()

        val url = "$DRIVE_UPLOAD?uploadType=multipart&fields=id,createdTime"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            doOutput = true
        }
        conn.outputStream.use { it.write(body) }
        val response = readResponse(conn)
        val created = json.decodeFromString(DriveFile.serializer(), response)
        return created.id
    }

    suspend fun listLatestBackupFileId(accessToken: String, folderId: String): String? {
        val q = "'$folderId' in parents and trashed=false"
        // createdTime only, newest first
        val url = "$DRIVE_FILES?q=${encode(q)}&orderBy=createdTime desc&fields=files(id,name,createdTime)&pageSize=10"
        val res = get(accessToken, url)
        val parsed = json.decodeFromString(FileListResponse.serializer(), res)
        return parsed.files.firstOrNull()?.id
    }

    suspend fun downloadFileBytes(accessToken: String, fileId: String): ByteArray {
        val url = "$DRIVE_FILES/$fileId?alt=media"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        if (code !in 200..299) throw RuntimeException("Drive download failed: HTTP $code ${conn.errorStream?.bufferedReader()?.readText()}")
        return conn.inputStream.use { it.readBytes() }
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    private fun get(accessToken: String, url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return readResponse(conn)
    }

    private fun postJson(accessToken: String, url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw RuntimeException("Drive API failed: HTTP $code $text")
        return text
    }
}
