package com.example.noteapp.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private var logFile: File? = null
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs")
        dir.mkdirs()
        logFile = File(dir, "noteapp.log")
    }

    @Synchronized
    fun i(tag: String, msg: String) = log("INFO", tag, msg)

    @Synchronized
    fun w(tag: String, msg: String) = log("WARN", tag, msg)

    @Synchronized
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        val sw = StringWriter()
        if (throwable != null) {
            throwable.printStackTrace(PrintWriter(sw))
            log("ERROR", tag, "$msg\n$sw")
        } else {
            log("ERROR", tag, msg)
        }
    }

    private fun log(level: String, tag: String, msg: String) {
        val time = fmt.format(Date())
        val line = "$time [$level/$tag] $msg\n"
        try {
            logFile?.appendText(line)
        } catch (_: Exception) { }
    }

    fun readAll(): String {
        return try {
            logFile?.readText() ?: "日志为空"
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }

    fun clear() {
        try {
            logFile?.writeText("")
        } catch (_: Exception) { }
    }
}
