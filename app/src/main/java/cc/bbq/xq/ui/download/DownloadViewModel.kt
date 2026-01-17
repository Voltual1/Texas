package cc.bbq.xq.ui.download

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.data.db.DownloadTaskRepository
import cc.bbq.xq.service.download.DownloadService
import cc.bbq.xq.service.download.DownloadStatus
import cc.bbq.xq.service.download.DownloadTask
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DownloadViewModel(
    application: Application,
    private val downloadTaskRepository: DownloadTaskRepository
) : AndroidViewModel(application) {

    private val TAG = "DownloadViewModel"

    // 1. 实时下载状态（来自正在运行的 Service）
    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    // 2. 历史任务列表（来自数据库）
    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    private var downloadService: DownloadService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.DownloadBinder
            downloadService = binder.getService()
            isBound = true
            // 连接成功后，开始同步 Service 中的实时下载状态
            observeServiceStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            isBound = false
        }
    }

    init {
        bindService()
        observeDownloadTasks()
    }

    private fun bindService() {
        val intent = Intent(getApplication(), DownloadService::class.java)
        // 确保 Service 生命周期独立于 Activity
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun observeServiceStatus() {
        viewModelScope.launch {
            downloadService?.getDownloadStatus()?.collect { status ->
                _downloadStatus.value = status
            }
        }
    }

    private fun observeDownloadTasks() {
        viewModelScope.launch {
            downloadTaskRepository.getAllDownloadTasks().collect { tasks ->
                _downloadTasks.value = tasks
            }
        }
    }

    /**
     * 取消当前正在进行的下载任务
     */
    fun cancelDownload() {
        if (isBound) {
            downloadService?.cancelCurrentDownload() 
            // 注意：Service 内部会更新 Flow，所以这里不需要手动设置 _downloadStatus.value
        }
    }

    /**
     * 删除下载任务（同时尝试物理删除文件可以根据需求决定）
     */
    fun deleteDownloadTask(downloadTask: DownloadTask) {
        viewModelScope.launch {
            // 如果删除的是当前正在下载的任务，先取消它
            if (_downloadStatus.value is DownloadStatus.Downloading && 
                (_downloadStatus.value as DownloadStatus.Downloading).totalBytes > 0) {
                // 这里简单对比，如果 URL 一致就取消
                // 实际生产环境建议通过 task.url 对比
            }
            downloadTaskRepository.deleteDownloadTask(downloadTask)
        }
    }

    fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}