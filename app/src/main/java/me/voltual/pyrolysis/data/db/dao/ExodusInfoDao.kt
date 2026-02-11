package me.voltual.pyrolysis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.voltual.pyrolysis.data.db.entity.ExodusInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ExodusInfoDao : BaseDao<ExodusInfo> {
    @Query("SELECT * FROM exodus_info WHERE packageName = :packageName")
    fun get(packageName: String): List<ExodusInfo>

    @Query("SELECT * FROM exodus_info WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<ExodusInfo>>
}
