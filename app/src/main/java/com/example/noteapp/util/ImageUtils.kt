package com.example.noteapp.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageUtils {

    private const val IMAGE_DIR = "images"

    fun getImageDir(context: Context): File =
        File(context.filesDir, IMAGE_DIR).also { it.mkdirs() }

    fun getAudioDir(context: Context): File =
        File(context.filesDir, "audio").also { it.mkdirs() }

    fun copyUriToImages(context: Context, uri: Uri, extension: String): File? {
        val file = File(getImageDir(context), "${UUID.randomUUID()}.$extension")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun markdownImageTag(file: File): String =
        "![image](${file.absolutePath})"

    fun deleteReferencedImages(content: String) {
        val regex = Regex("""!\[[^\]]*\]\(([^)]+)\)""")
        regex.findAll(content).forEach { match ->
            val path = match.groupValues[1]
            try {
                File(path).delete()
            } catch (_: Exception) { }
        }
    }

    fun cleanOrphanImages(context: Context, allContents: List<String>) {
        val referencedPaths = mutableSetOf<String>()
        val regex = Regex("""!\[[^\]]*\]\(([^)]+)\)""")
        allContents.forEach { content ->
            regex.findAll(content).forEach { match ->
                referencedPaths.add(match.groupValues[1])
            }
        }
        val imageDir = getImageDir(context)
        imageDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in referencedPaths) {
                file.delete()
            }
        }
    }
}
