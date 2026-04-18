package com.texas.pyrolysis.ui.plaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.texas.pyrolysis.data.repository.WysAppCrawlerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CrawlerUiState(
    val isDetectingMaxId: Boolean = false,
    val isCrawling: Boolean = false,
    val isExporting: Boolean = false,
    val startId: Int = 1,
    val endId: Int = 0,
    val currentProgress: Int = 0,
    val totalToCrawl: Int = 0,
    val successCount: Int = 0,
    val lastLog: String = "就绪",
    val exportedFile: File? = null
)

class CrawlerViewModel(
    private val crawlerRepository: WysAppCrawlerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrawlerUiState())
    val uiState = _uiState.asStateFlow()

    // 使用 Channel 发送一次性 Snackbar 事件
    private val _snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarEvents = _snackbarChannel.receiveAsFlow()

    private var crawlJob: Job? = null

    fun detectMaxId() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingMaxId = true, lastLog = "正在探测...") }
            try {
                val maxId = crawlerRepository.findMaxAppId()
                _uiState.update { it.copy(isDetectingMaxId = false, endId = maxId, lastLog = "探测完成: $maxId") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDetectingMaxId = false) }
                _snackbarChannel.send("探测失败: ${e.message}")
            }
        }
    }

    fun updateStartId(id: Int) { _uiState.update { it.copy(startId = id) } }
    fun updateEndId(id: Int) { _uiState.update { it.copy(endId = id) } }

    fun startCrawling() {
        if (_uiState.value.isCrawling) return
        crawlJob = viewModelScope.launch {
            _uiState.update { it.copy(isCrawling = true, successCount = 0, currentProgress = 0, lastLog = "启动爬虫...") }
            try {
                crawlerRepository.startCrawling(
                    startId = _uiState.value.startId,
                    endId = _uiState.value.endId
                ) { current, total, isSuccess ->
                    _uiState.update { 
                        it.copy(
                            currentProgress = current,
                            totalToCrawl = total,
                            successCount = if (isSuccess) it.successCount + 1 else it.successCount,
                            lastLog = "进度: $current/$total | 成功: ${if (isSuccess) it.successCount + 1 else it.successCount}"
                        )
                    }
                }
                _uiState.update { it.copy(isCrawling = false, lastLog = "任务结束") }
                _snackbarChannel.send("抓取任务已完成，共保存 ${uiState.value.successCount} 条数据")
            } catch (e: Exception) {
                _uiState.update { it.copy(isCrawling = false) }
                _snackbarChannel.send("抓取中断: ${e.message}")
            }
        }
    }

    fun stopCrawling() {
        crawlJob?.cancel()
        _uiState.update { it.copy(isCrawling = false, lastLog = "已手动停止") }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            crawlerRepository.exportToJson()
                .onSuccess { file ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = file) }
                    _snackbarChannel.send("导出成功！")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExporting = false) }
                    _snackbarChannel.send("导出失败: ${e.message}")
                }
        }
    }

    fun clearExportedFile() { _uiState.update { it.copy(exportedFile = null) } }
}