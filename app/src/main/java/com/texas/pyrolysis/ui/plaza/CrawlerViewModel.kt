package com.texas.pyrolysis.ui.plaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.texas.pyrolysis.data.repository.WysAppCrawlerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val errorCount: Int = 0,
    val lastLog: String = "就绪",
    val exportedFile: File? = null,
    val errorMessage: String? = null
)

class CrawlerViewModel(
    private val crawlerRepository: WysAppCrawlerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrawlerUiState())
    val uiState = _uiState.asStateFlow()

    private var crawlJob: Job? = null

    /**
     * 第一步：探测最大 ID
     */
    fun detectMaxId() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingMaxId = true, lastLog = "正在通过二分法探测最大可用 ID...") }
            try {
                val maxId = crawlerRepository.findMaxAppId()
                _uiState.update { 
                    it.copy(
                        isDetectingMaxId = false, 
                        endId = maxId, 
                        lastLog = "探测完成，当前商店最大 ID 约为: $maxId"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDetectingMaxId = false, errorMessage = "探测失败: ${e.message}") }
            }
        }
    }

    /**
     * 设置起始 ID
     */
    fun updateStartId(id: Int) {
        _uiState.update { it.copy(startId = id) }
    }

    /**
     * 设置结束 ID
     */
    fun updateEndId(id: Int) {
        _uiState.update { it.copy(endId = id) }
    }

    /**
     * 第二步：开始批量爬取
     */
    fun startCrawling() {
        if (_uiState.value.isCrawling) return

        crawlJob = viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isCrawling = true, 
                    successCount = 0, 
                    errorCount = 0,
                    currentProgress = 0,
                    errorMessage = null,
                    lastLog = "爬虫启动..."
                ) 
            }

            try {
                crawlerRepository.startCrawling(
                    startId = _uiState.value.startId,
                    endId = _uiState.value.endId,
                    concurrency = 3 // 默认 3 并发
                ) { current, total ->
                    // 进度回调
                    _uiState.update { 
                        it.copy(
                            currentProgress = current,
                            totalToCrawl = total,
                            successCount = current, // 这里简单处理，实际可根据 repo 返回细化
                            lastLog = "正在爬取进度: $current / $total"
                        )
                    }
                }
                _uiState.update { it.copy(isCrawling = false, lastLog = "爬取任务全部完成！") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCrawling = false, errorMessage = "爬取中断: ${e.message}") }
            }
        }
    }

    /**
     * 停止爬取
     */
    fun stopCrawling() {
        crawlJob?.cancel()
        _uiState.update { it.copy(isCrawling = false, lastLog = "用户手动停止了任务") }
    }

    /**
     * 第三步：导出 JSON
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, lastLog = "正在导出数据库...") }
            crawlerRepository.exportToJson()
                .onSuccess { file ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = file, lastLog = "导出成功: ${file.absolutePath}") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExporting = false, errorMessage = "导出失败: ${e.message}") }
                }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}