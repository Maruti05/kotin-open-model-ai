package com.vedica.labs.ind.app.chat.openmodels

import android.app.Application
import android.os.Environment
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class OpenModelsApp : Application() {

    companion object {
        private const val TAG = "OpenModels"
        private var crashLogFile: File? = null
    }

    override fun onCreate() {
        super.onCreate()

        setupLogging()
        setupCrashHandler()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val logFile = crashLogFile ?: run {
                    val dir = File(filesDir, "logs")
                    dir.mkdirs()
                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                    File(dir, "session_$date.log").also { crashLogFile = it }
                }
                try {
                    val level = when (priority) {
                        Log.VERBOSE -> "V"
                        Log.DEBUG -> "D"
                        Log.INFO -> "I"
                        Log.WARN -> "W"
                        Log.ERROR -> "E"
                        Log.ASSERT -> "A"
                        else -> "?"
                    }
                    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
                    val tTag = tag ?: "OpenModels"
                    FileWriter(logFile, true).use { writer ->
                        writer.append("$time $level/$tTag: $message\n")
                        if (t != null) {
                            val sw = java.io.StringWriter()
                            t.printStackTrace(PrintWriter(sw))
                            writer.append(sw.toString())
                            writer.append("\n")
                        }
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Install a crash handler that saves full stack traces to filesDir/crashes/
        // before forwarding to the default handler. This is our safety net for diagnosing
        // crashes that happen in production (especially native SIGSEGV from llama.cpp)
        // which the Java/Kotlin layer can't fully recover from — but we at least capture
        // any Java-side context leading up to it and write it to persistent storage.
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.tag("CRASH").e(throwable, "Uncaught exception on thread: %s", thread.name)

                val crashDir = File(filesDir, "crashes")
                crashDir.mkdirs()
                val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val crashFile = File(crashDir, "crash_$date.txt")

                FileWriter(crashFile).use { writer ->
                    writer.append("=== CRASH REPORT ===\n")
                    writer.append("Time: $date\n")
                    writer.append("Thread: ${thread.name}\n")
                    writer.append("Model: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n\n")
                    writer.append("Stack Trace:\n")
                    throwable.printStackTrace(PrintWriter(writer))
                }
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Native crash signal handler (best-effort - caught via logcat)
        Timber.tag("CRASH").w("Crash handler installed. Native crashes will appear in logcat as signal logs.")
    }
}
