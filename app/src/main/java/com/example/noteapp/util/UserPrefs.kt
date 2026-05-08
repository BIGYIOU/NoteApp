package com.example.noteapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object UserPrefs {
    private const val PREFS = "user_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_AVATAR = "avatar_path"

    fun getName(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "BIGYIOU") ?: "BIGYIOU"
    }

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()
    }

    fun getAvatarPath(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, "") ?: ""
    }

    fun setAvatarPath(context: Context, path: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_AVATAR, path).apply()
    }

    fun saveAvatarFile(context: Context, sourceUri: Uri): String? {
        val dir = File(context.filesDir, "avatar")
        dir.mkdirs()
        val file = File(dir, "avatar.jpg")
        return try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return null

            // Crop to square center
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)

            // Resize to 256x256
            val resized = Bitmap.createScaledBitmap(cropped, 256, 256, true)
            if (cropped != resized && cropped != bitmap) cropped.recycle()
            if (bitmap != resized) bitmap.recycle()

            FileOutputStream(file).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            resized.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
