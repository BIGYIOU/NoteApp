package com.example.noteapp.util

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.noteapp.data.db.AppDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtils {

    suspend fun export(context: Context): File? {
        return try {
            // Clean orphan images before export
            val db = AppDatabase.getInstance(context)
            val allContents = db.noteDao().getAllContents()
            ImageUtils.cleanOrphanImages(context, allContents)

            val dbDir = context.getDatabasePath("noteapp.db").parentFile ?: return null
            val dbFile = File(dbDir, "noteapp.db")
            val dbShm = File(dbDir, "noteapp.db-shm")
            val dbWal = File(dbDir, "noteapp.db-wal")
            val imagesDir = File(context.filesDir, "images")

            // Close DB before copying (best effort — will work with WAL)
            val zipFile = File(context.cacheDir, "noteapp_backup_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                fun addFile(file: File, entryName: String) {
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                addFile(dbFile, "noteapp.db")
                addFile(dbShm, "noteapp.db-shm")
                addFile(dbWal, "noteapp.db-wal")
                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { img ->
                        addFile(img, "images/${img.name}")
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            Logger.e("Backup", "导出失败", e)
            null
        }
    }

    fun import(context: Context, uri: Uri): Boolean {
        return try {
            // First, copy the current DB as a safety backup
            val dbPath = context.getDatabasePath("noteapp.db").absolutePath
            val safetyBackup = File(context.filesDir, "pre_import_backup.db")
            try { File(dbPath).copyTo(safetyBackup, overwrite = true) } catch (_: Exception) {}

            val dbDir = context.getDatabasePath("noteapp.db").parentFile ?: return false
            val imagesDir = File(context.filesDir, "images")
            imagesDir.mkdirs()

            // Close DB connections
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = if (entry.name.startsWith("images/")) {
                            File(imagesDir, entry.name.removePrefix("images/"))
                        } else {
                            File(dbDir, entry.name)
                        }
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Logger.i("Backup", "导入成功，请重启应用")
            true
        } catch (e: Exception) {
            Logger.e("Backup", "导入失败", e)
            false
        }
    }
}
