package me.voltual.pyrolysis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import me.voltual.pyrolysis.data.db.dao.*
import me.voltual.pyrolysis.data.db.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get

@Database(
    entities = [
        Repository::class,
        Product::class,
        Release::class,
        Category::class,
        RepoCategory::class,
        Installed::class,
        ExodusInfo::class,
        AntiFeature::class,
        Tracker::class,
        // 核心同步依赖的临时表（Temp 表）
        ProductTemp::class,
        ReleaseTemp::class,
        CategoryTemp::class,
        RepoCategoryTemp::class,
        AntiFeatureTemp::class
    ],
    version = 1, // 既然是新移植，直接从 1 开始
    exportSchema = false // 内部使用可关闭，减少 schema 文件维护
)
@TypeConverters(Converters::class)
abstract class DatabaseX : RoomDatabase() {

    // --- 正式表 DAO ---
    abstract fun getRepositoryDao(): RepositoryDao
    abstract fun getProductDao(): ProductDao
    abstract fun getReleaseDao(): ReleaseDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun getRepoCategoryDao(): RepoCategoryDao
    abstract fun getAntiFeatureDao(): AntiFeatureDao
    abstract fun getInstalledDao(): InstalledDao
    abstract fun getExodusInfoDao(): ExodusInfoDao
    abstract fun getTrackerDao(): TrackerDao

    // --- 临时表 DAO (用于 Index 数据解析时的原子更新) ---
    abstract fun getProductTempDao(): ProductTempDao
    abstract fun getReleaseTempDao(): ReleaseTempDao
    abstract fun getCategoryTempDao(): CategoryTempDao
    abstract fun getRepoCategoryTempDao(): RepoCategoryTempDao
    abstract fun getAntiFeatureTempDao(): AntiFeatureTempDao

    companion object {
        const val TAG = "DatabaseX"

        // 数据库创建时的初始化回调
        val dbCreateCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = get<RepositoryDao>(RepositoryDao::class.java)
                    if (dao.getCount() == 0) {
                        // 初始时注入 F-Droid 官方源或你自定义的默认源
                        dao.put(*Repository.defaultRepositories.toTypedArray())
                    }
                }
            }
        }
    }

    /**
     * 同步成功后，将临时表数据覆盖到正式表
     * 这是移植 Neo/F-Droid 逻辑中最关键的函数
     */
    suspend fun finishTemporary(repository: Repository, success: Boolean) {
        withTransaction {
            val repoId = repository.id
            if (success) {
                // 1. 清理该仓库下的旧数据
                getProductDao().deleteById(repoId)
                getCategoryDao().deleteById(repoId)
                getRepoCategoryDao().deleteByRepoId(repoId)
                getAntiFeatureDao().deleteByRepoId(repoId)
                getReleaseDao().deleteById(repoId)

                // 2. 将临时表里的新索引数据搬运过来
                getProductDao().insert(*(getProductTempDao().getAll()))
                getCategoryDao().insert(*(getCategoryTempDao().getAll()))
                getRepoCategoryDao().insert(*(getRepoCategoryTempDao().getAll()))
                getAntiFeatureDao().insert(*(getAntiFeatureTempDao().getAll()))
                getReleaseDao().insert(*(getReleaseTempDao().getAll()))

                // 3. 更新仓库的 LastModified 等状态
                getRepositoryDao().put(repository)
            }
            
            // 无论成功失败，最后都要清空临时表，释放内存和空间
            getProductTempDao().emptyTable()
            getCategoryTempDao().emptyTable()
            getRepoCategoryTempDao().emptyTable()
            getAntiFeatureTempDao().emptyTable()
            getReleaseTempDao().emptyTable()
        }
    }
}