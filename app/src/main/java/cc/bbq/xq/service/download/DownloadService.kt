package cc.bbq.xq.service.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import cc.bbq.xq.data.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DownloadService : Service() {
    companion object {
    private const val TAG = "DownloadService"
    const val ACTION_START_DOWNLOAD = "cc.bbq.xq.action.START_DOWNLOAD"
    const val ACTION_CANCEL_DOWNLOAD = "cc.bbq.xq.action.CANCEL_DOWNLOAD"
    
    // 补充缺失的 Key 常量
    const val EXTRA_URL = "extra_url"
    const val EXTRA_FILE_NAME = "extra_file_name"
    const val EXTRA_SAVE_PATH = "extra_save_path"
}

    private val binder = DownloadBinder()
    private lateinit var downloader: KtorDownloader
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    
    // 专门管理下载任务的 Job
    private var downloadTaskJob: Job? = null
    
    private lateinit var appDatabase: AppDatabase
    private val downloadTaskDao by lazy { appDatabase.downloadTaskDao() }
    private var currentDownloadConfig: DownloadConfig? = null

    // 在 DownloadService.kt 中修改 DownloadBinder
inner class DownloadBinder : Binder() {
    fun getService(): DownloadService = this@DownloadService
    
    // 暴露状态流
    fun getDownloadStatus(): StateFlow<DownloadStatus> = downloader.status
    
    // 暴露取消方法
    fun cancelDownload() {
        this@DownloadService.cancelCurrentDownload()
    }
}

    override fun onCreate() {
        super.onCreate()
        downloader = KtorDownloader()
        appDatabase = AppDatabase.getDatabase(this)
        setupStatusObserver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
        ACTION_START_DOWNLOAD -> {
            val url = intent.getStringExtra(EXTRA_URL)
            val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
            val savePath = intent.getStringExtra(EXTRA_SAVE_PATH)
            
            if (url != null && fileName != null) {
                // 调用你已经写好的 startDownload 方法
                startDownload(url, fileName, savePath)
            }
        }
        ACTION_CANCEL_DOWNLOAD -> {
            cancelCurrentDownload()
        }
    }
    // START_NOT_STICKY 表示如果系统内存不足杀掉了 Service，不会自动尝试重新创建（适合下载任务）
    return START_NOT_STICKY
}

    /**
     * 核心取消逻辑：既取消协程，又重置 Downloader 状态
     */
    private fun cancelCurrentDownload() {
        Log.d(TAG, "Cancelling current download task")
        downloadTaskJob?.cancel() // 修复点 2：物理切断协程
        downloader.cancel()       // 重置状态
    }

    fun startDownload(url: String, fileName: String, customSavePath: String? = null) {
    if (downloader.status.value is DownloadStatus.Downloading) return

    val savePath = customSavePath ?: getDefaultDownloadPath()
    val config = DownloadConfig(url, savePath, fileName)
    currentDownloadConfig = config

    // 关键：合并到一个 launch 块中，确保顺序执行
    serviceScope.launch {
        try {
            // 1. 先检查并确保数据库有这条记录
            val existing = downloadTaskDao.getDownloadTask(url).firstOrNull()
            if (existing == null) {
                val newTask = DownloadTask(
                    url = url,
                    fileName = fileName,
                    savePath = savePath,
                    status = DownloadStatus.Pending::class.java.simpleName,
                    progress = 0f
                )
                downloadTaskDao.insert(newTask)
                Log.d(TAG, "Task inserted into DB")
            }

            // 2. 只有数据库准备好了，才开始下载
            // 注意：这里不再开启新的 launch，直接在当前协程执行或调用挂起函数
            downloader.startDownload(config)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize download task", e)
        }
    }
}

    private fun setupStatusObserver() {
        downloader.status
            .onEach { status ->
                // 更新数据库逻辑 (保持你原有的不变)
                updateDatabaseStatus(status)
                
                if (status is DownloadStatus.Success || status is DownloadStatus.Error) {
                    stopForegroundWithCompat()
                }
            }
            .launchIn(serviceScope)
    }

    private suspend fun updateDatabaseStatus(status: DownloadStatus) {
        val config = currentDownloadConfig ?: return
        val task = downloadTaskDao.getDownloadTask(config.url).firstOrNull() ?: return
        
        val updated = when (status) {
            is DownloadStatus.Downloading -> task.copy(
                status = "Downloading",
                downloadedBytes = status.downloadedBytes,
                totalBytes = status.totalBytes,
                progress = status.progress
            )
            is DownloadStatus.Success -> task.copy(status = "Success")
            is DownloadStatus.Error -> task.copy(status = "Error")
            else -> task
        }
        downloadTaskDao.update(updated)
    }

    private fun stopForegroundWithCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        downloader.close()
        serviceJob.cancel() // 修复点 4：销毁 Service 时取消所有 Job
        super.onDestroy()
    }

    private fun getDefaultPath(): String = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
}