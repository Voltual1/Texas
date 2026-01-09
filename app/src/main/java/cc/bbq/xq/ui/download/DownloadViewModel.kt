//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.download

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.data.db.AppDatabase
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
    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    // 添加一个 StateFlow 来存储从数据库获取的所有下载任务
    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    private var downloadService: DownloadService? = null
    private var isBound = false

    // 使用 DownloadTaskRepository


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.DownloadBinder
            downloadService = binder.getService()
            isBound = true
            // 连上服务后，立即开始观察状态
            observeServiceStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            isBound = false
        }
    }

    init {
        bindService()
        // 从数据库获取所有下载任务
        observeDownloadTasks()
    }

    private fun bindService() {
        val intent = Intent(getApplication(), DownloadService::class.java)
        // 启动服务以确保它在后台运行（如果尚未运行）
        getApplication<Application>().startService(intent)
        // 绑定服务以进行通信
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

    /**
     * 从数据库获取所有下载任务
     */
    private fun observeDownloadTasks() {
        viewModelScope.launch {
            downloadTaskRepository.getAllDownloadTasks().collect { tasks ->
                _downloadTasks.value = tasks
            }
        }
    }

    /**
     * 根据 URL 获取特定的下载任务
     */
    fun getDownloadTaskByUrl(url: String): DownloadTask? {
        return _downloadTasks.value.find { it.url == url }
    }

    /**
     * 取消当前下载
     */
    fun cancelDownload() {
        // 这里假设 Service 或 Downloader 有 cancel 方法
        // 目前 KtorDownloader.cancel() 只是重置状态，实际逻辑需完善 Job 管理
        // downloadService?.cancelCurrentTask()
        // 暂时模拟重置状态
        _downloadStatus.value = DownloadStatus.Idle
    }

    /**
     * 删除下载任务
     */
    fun deleteDownloadTask(downloadTask: DownloadTask) {
        viewModelScope.launch {
            downloadTaskRepository.deleteDownloadTask(downloadTask)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * 使用浏览器打开链接
     */
    fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // 确保在新的任务栈中打开
        try {
            ContextCompat.startActivity(getApplication(), intent, null)
        } catch (e: Exception) {
            // 处理无法打开链接的情况，例如没有浏览器应用
            // 可以显示一个 Toast 提示用户
        }
    }
}