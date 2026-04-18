package com.texas.pyrolysis.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "crawler_data") // 换一个全新的表名，彻底避开之前的 Migration 问题
data class CrawlerDataEntry(
    @PrimaryKey val appId: Int,
    val name: String,
    val packageName: String,
    val version: String,
    val downloadUrlsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.room.Dao
interface CrawlerDataDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CrawlerDataEntry)

    @androidx.room.Query("SELECT * FROM crawler_data ORDER BY appId ASC")
    suspend fun getAll(): List<CrawlerDataEntry>

    @androidx.room.Query("SELECT MAX(appId) FROM crawler_data")
    suspend fun getMaxId(): Int?

    @androidx.room.Query("DELETE FROM crawler_data")
    suspend fun clearAll()
}