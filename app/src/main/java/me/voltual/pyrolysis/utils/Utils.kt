package me.voltual.pyrolysis.utils

import android.Manifest
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.pm.Signature
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import me.voltual.pyrolysis.utils.extension.text.hex
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {    

    fun calculateSHA256(signature: Signature): String {
        return MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .hex()
    }
       
    fun calculateSHA256(hexadecString: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(
                hexadecString
                    .chunked(2)
                    .mapNotNull { byteStr ->
                        try {
                            byteStr.toInt(16).toByte()
                        } catch (_: NumberFormatException) {
                            null
                        }
                    }
                    .toByteArray()
            ).hex()
    }

    
}
fun Context.getLocaleDateString(time: Long): String {
    val date = Date(time)
    val format = if (DateUtils.isToday(date.time)) DateUtils.FORMAT_SHOW_TIME else
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
    return DateUtils.formatDateTime(this, date.time, format)
}