//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.service.download

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载状态密封接口，用于 UI 响应式更新
 */
sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object Pending : DownloadStatus
    
    data class Downloading(
        val progress: Float, 
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: String
    ) : DownloadStatus

    // 建议保留 Paused，即使当前版本暂未实现手动暂停，未来扩展很方便
    data class Paused(
        val downloadedBytes: Long, 
        val totalBytes: Long
    ) : DownloadStatus

    // 确保这里的 File 导入正确 (java.io.File)
    data class Success(val file: java.io.File) : DownloadStatus

    data class Error(
        val message: String, 
        val throwable: Throwable? = null
    ) : DownloadStatus
}

/**
 * 下载配置
 */
data class DownloadConfig(
    val url: String,
    val savePath: String,
    val fileName: String,
    val threadCount: Int = 3 // 默认3线程并发
)

/**
 * 内部使用的分块信息
 */
internal data class Chunk(
    val id: Int,
    val start: Long,
    val end: Long,
    var current: Long
) {
    val size: Long get() = end - start + 1
    val isComplete: Boolean get() = current > end
}

/**
 * 下载任务实体类，用于 Room 数据库持久化存储
 */
@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey val url: String, // 使用 URL 作为主键，保证唯一性
    val fileName: String,
    val savePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // 使用字符串存储下载状态，方便转换
    val progress: Float,
    val speed: String? = null,
    val errorMessage: String? = null
)