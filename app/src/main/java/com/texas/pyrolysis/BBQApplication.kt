package com.texas.pyrolysis

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.texas.pyrolysis.data.db.AppDatabase
import com.texas.pyrolysis.ui.theme.ThemeColorStore
import com.texas.pyrolysis.ui.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.*
import java.lang.reflect.Field
import java.util.zip.ZipFile

@KoinApplication
class BBQApplication : Application() {

    // 数据库单例
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 执行签名“自杀”逻辑 (建议最早执行)
        runSignatureKiller()

        // 2. 原有逻辑：初始化 AuthManager
        AuthManager.initialize(this)

        // 3. 原有逻辑：初始化数据库
        database = AppDatabase.getDatabase(this)

        // 4. 原有逻辑：异步迁移
        CoroutineScope(Dispatchers.IO).launch {
            AuthManager.migrateFromSharedPreferences(applicationContext)
        }

        // 5. 原有逻辑：主题管理
        ThemeManager.initialize(this)
        ThemeManager.customColorSet = ThemeColorStore.loadColors(this)

        // 6. 原有逻辑：Koin
        startKoin {
            androidContext(this@BBQApplication)
            modules(appModule)
        }
    }

    private fun runSignatureKiller() {
        // 你的目标包名
        val packageName = "com.texas.pyrolysis" 
        
        // 你提供的原始签名 Base64
        val signatureData = """
            MIICoDCCAYgCAQEwDQYJKoZIhvcNAQELBQAwFTETMBEGA1UEAwwKUGlhbm9FdGhhbjAgFw0yNTEy
            MTkxMjIyMjZaGA8yMDc1MTIwNzEyMjIyNlowFTETMBEGA1UEAwwKUGlhbm9FdGhhbjCCASIwDQYJ
            KoZIhvcNAQEBBQADggEPADCCAQoCggEBAJwxIzwc+iWckmq6V8xcvxrjDsnAGTHhmyfMSbyeZZGi
            tVLTlBWIIcayJaCI1w4SKhq0uAtbRsCSQR1BPZCUDdhhx60yxPIibm239AmUhCivY+0bAomareS1
            eWdT7idmJKt3KGl/Le+XN6v14ZdOpq+1j/+gO5YHRYpx6n9fSQSn/NKkO0xFuEhw6YLJmnm2RViJ
            MN1vpldrlaXkCRDFgvYJ4vecqi8opy6V5ZZ0Tl0Nswx8wkymAmhJOLgc5IWPgnlz6FtrPWKPM5WQ
            Gc2UKUiNGfTbiDJV/bjPs72jAXZVlFD2eX/aHHaDoWQRrwPrRV6CGeZDxBhjLka1PKm6vgMCAwEA
            ATANBgkqhkiG9w0BAQsFAAOCAQEASN8KBgM3w+eRMjggFpsKvCZLBzncmKNwVlxIyKhcuR9BTlCJ
            +ceFhiaL3oMHUKCeY/F6ZpOOOJniUIqieWx4JV6phbGNaBcGxvujrCqrY4D5Rz66Msm/BRfF0nZH
            AAeGEpDUFUZxNxmIWDOsbD6PGOUCeykqUIpJppqRoSHFr59uhLlGPFfz2H8MUUMIPO8CxYtF0jZK
            RaXmSZ4zVnk0VhquQctxrkU1Dw4OyCReqodymDfmAFl5JoxMhrwsB88XW0HsKsNmu9XQnEZPDVMs
            jG14HfWcOgHu9Vgd0Xv+0L/fLFX21IrioyRHoEWoQ53Pb+cJl10YgtG5dc+pXdJL4g==
        """.trimIndent().replace("\n", "") // 去除换行符确保 Base64 解码正常

        try {
            killPM(packageName, signatureData)
            killOpen(packageName)
        } catch (e: Exception) {
            // 静默处理或记录日志
        }
    }

    private fun killPM(packageName: String, signatureData: String) {
        val fakeSignature = Signature(Base64.decode(signatureData, Base64.DEFAULT))
        val originalCreator = PackageInfo.CREATOR
        
        val creator = object : Parcelable.Creator<PackageInfo> {
            override fun createFromParcel(source: Parcel): PackageInfo {
                val packageInfo = originalCreator.createFromParcel(source)
                if (packageInfo.packageName == packageName) {
                    packageInfo.signatures?.takeIf { it.isNotEmpty() }?.let {
                        it[0] = fakeSignature
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() }?.let {
                            it[0] = fakeSignature
                        }
                    }
                }
                return packageInfo
            }

            override fun newArray(size: Int): Array<PackageInfo?> = originalCreator.newArray(size)
        }

        try {
            findField(PackageInfo::class.java, "CREATOR").set(null, creator)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("Landroid/os/Parcel;", "Landroid/content/pm", "Landroid/app")
            }

            // 清理缓存
            try {
                val cache = findField(PackageManager::class.java, "sPackageInfoCache").get(null)
                cache?.javaClass?.getMethod("clear")?.invoke(cache)
            } catch (ignored: Throwable) {}

            // 清理 Parcel 内部 Creator 缓存
            listOf("mCreators", "sPairedCreators").forEach { fieldName ->
                try {
                    val m = findField(Parcel::class.java, fieldName).get(null) as? MutableMap<*, *>
                    m?.clear()
                } catch (ignored: Throwable) {}
            }
        } catch (e: Exception) {
            // Log.e("Killer", "killPM failed", e)
        }
    }

    private fun killOpen(packageName: String) {
        try {
            System.loadLibrary("SignatureKiller")
        } catch (e: Throwable) {
            return
        }

        val apkPath = getApkPath(packageName) ?: return
        val apkFile = File(apkPath)
        val repFile = File(getDataFile(packageName), "origin.apk")

        try {
            ZipFile(apkFile).use { zipFile ->
                val entryName = "assets/SignatureKiller/origin.apk"
                val entry = zipFile.getEntry(entryName) ?: return
                
                if (!repFile.exists() || repFile.length() != entry.size) {
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(repFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            hookApkPath(apkFile.absolutePath, repFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SdCardPath")
    private fun getDataFile(packageName: String): File {
        val username = Environment.getExternalStorageDirectory().name
        if (username.matches(Regex("\\d+"))) {
            val file = File("/data/user/$username/$packageName")
            if (file.canWrite()) return file
        }
        return File("/data/data/$packageName")
    }

    private fun getApkPath(packageName: String): String? {
        return try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val arr = line!!.split(Regex("\\s+"))
                    val path = arr.last()
                    if (isApkPath(packageName, path)) return path
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isApkPath(packageName: String, path: String): Boolean {
        if (!path.startsWith("/") || !path.endsWith(".apk")) return false
        val splitStr = path.substring(1).split("/")
        val size = splitStr.size
        // 简化的路径匹配逻辑，对应原 Java 逻辑
        return when {
            (size == 4 || size == 5) && splitStr[0] == "data" && splitStr[1] == "app" -> splitStr[size - 2].startsWith(packageName)
            size == 3 && splitStr[0] == "data" && splitStr[1] == "app" -> splitStr[2].startsWith(packageName)
            else -> false
        }
    }

    private external fun hookApkPath(apkPath: String, repPath: String)

    companion object {
        lateinit var instance: BBQApplication
            private set

        private fun findField(clazz: Class<*>, fieldName: String): Field {
            var currentClass: Class<*>? = clazz
            while (currentClass != null && currentClass != Any::class.java) {
                try {
                    val field = currentClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    return field
                } catch (e: NoSuchFieldException) {
                    currentClass = currentClass.superclass
                }
            }
            throw NoSuchFieldException(fieldName)
        }
    }
}