package com.kwame.datadash

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global crash handler. There's no ADB/logcat access available while
 * building this app, so instead of the generic "App keeps stopping"
 * dialog, any uncaught exception is shown on-screen with its full
 * stack trace so it can be read directly (e.g. via screenshot).
 */
class DataDashApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))

                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_STACK_TRACE, sw.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // If even the crash screen fails, fall through silently.
            }

            Process.killProcess(Process.myPid())
            @Suppress("DEPRECATION")
            System.exit(10)
        }
    }
}
