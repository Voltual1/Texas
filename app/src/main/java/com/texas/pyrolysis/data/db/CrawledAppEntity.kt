package com.texas.pyrolysis.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "crawled_apps")
data class CrawledAppEntity(
    @PrimaryKey val appId: Int,
    val name: String,
    val packageName: String,
    val version: String,
    val downloadUrlsJson: String, // 存储序列化后的 List<UnifiedDownloadSource>
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.room.Dao
interface CrawledAppDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: CrawledAppEntity)

    @androidx.room.Query("SELECT * FROM crawled_apps")
    suspend fun getAllApps(): List<CrawledAppEntity>

    @androidx.room.Query("SELECT MAX(appId) FROM crawled_apps")
    suspend fun getMaxCrawledId(): Int?

    @androidx.room.Query("SELECT COUNT(*) FROM crawled_apps")
    suspend fun getCount(): Int
}