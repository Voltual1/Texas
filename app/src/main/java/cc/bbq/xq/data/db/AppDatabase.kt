//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cc.bbq.xq.ui.community.BrowseHistory

@Database(
    entities = [LogEntry::class, BrowseHistory::class, NetworkCacheEntry::class, PostDraft::class],
    version = 7, // 升级到版本7，移除下载表
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun browseHistoryDao(): BrowseHistoryDao
    abstract fun networkCacheDao(): NetworkCacheDao
    abstract fun postDraftDao(): PostDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `browse_history` (`postId` INTEGER NOT NULL, `title` TEXT NOT NULL, `previewContent` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`postId`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `network_cache` (`requestKey` TEXT NOT NULL, `responseJson` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`requestKey`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `post_draft` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `imageUris` TEXT NOT NULL, `imageUrls` TEXT NOT NULL, `subsectionId` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        // 保留旧的迁移脚本，但在新版本中不包含下载表
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `download_tasks` (`url` TEXT NOT NULL, `fileName` TEXT NOT NULL, `savePath` TEXT NOT NULL, `totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `progress` REAL NOT NULL, PRIMARY KEY(`url`))"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加speed和errorMessage字段
                db.execSQL(
                    "ALTER TABLE `download_tasks` ADD COLUMN `speed` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `download_tasks` ADD COLUMN `errorMessage` TEXT"
                )
            }
        }

        // 新增迁移脚本：从版本6到版本7，删除下载表
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 删除下载表
                db.execSQL("DROP TABLE IF EXISTS `download_tasks`")
            }
        }

        // 从版本4直接到版本7的迁移（跳过下载表的创建）
        private val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 什么都不做，因为我们已经移除了下载表
                // 这个迁移适用于那些还在版本4的用户，他们直接升级到版本7
            }
        }

        // 从版本5直接到版本7的迁移
        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 删除下载表（如果存在）
                db.execSQL("DROP TABLE IF EXISTS `download_tasks`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qubot_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, 
                        MIGRATION_2_3, 
                        MIGRATION_3_4,
                        // 提供多条迁移路径
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_4_7,
                        MIGRATION_5_7
                    )
//                    .fallbackToDestructiveMigration() // 如果迁移失败，重建数据库（数据会丢失，但避免崩溃）
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}