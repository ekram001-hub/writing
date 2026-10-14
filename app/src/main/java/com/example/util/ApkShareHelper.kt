package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkShareHelper {
    suspend fun shareAppApk(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val appInfo = context.applicationInfo
                val sourceApk = File(appInfo.sourceDir)
                if (!sourceApk.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Cannot locate installed APK file.", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                // Copy APK to cache directory with clean name
                val shareDir = File(context.cacheDir, "shared_apk")
                if (!shareDir.exists()) {
                    shareDir.mkdirs()
                }
                val destFile = File(shareDir, "MicroWrite.apk")

                FileInputStream(sourceApk).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val authority = "${context.packageName}.fileprovider"
                val apkUri = FileProvider.getUriForFile(context, authority, destFile)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, "MicroWrite App APK")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Here is MicroWrite - an offline writing practice app for competitive examinations in Bangladesh. Install this APK to practice completely offline!"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    val chooser = Intent.createChooser(shareIntent, "Share MicroWrite APK with Friends")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error sharing APK: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
