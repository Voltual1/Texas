//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.


package com.texas.pyrolysis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntry)

    @Query("SELECT * FROM bot_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    // 新增：专门用于爬虫数据导出的同步查询
    @Query("SELECT * FROM bot_logs WHERE type = :type ORDER BY timestamp ASC")
    suspend fun getLogsByType(type: String): List<LogEntry>

    @Query("SELECT MAX(id) FROM bot_logs") // 这里的 ID 是自增的，探测逻辑需微调
    suspend fun getMaxLogId(): Int?

    @Query("DELETE FROM bot_logs WHERE id IN (:logIds)")
    suspend fun deleteLogsByIds(logIds: List<Int>)

    @Query("DELETE FROM bot_logs")
    suspend fun clearAll()

    @Query("SELECT * FROM bot_logs WHERE requestBody LIKE :query OR responseBody LIKE :query ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<LogEntry>>
}