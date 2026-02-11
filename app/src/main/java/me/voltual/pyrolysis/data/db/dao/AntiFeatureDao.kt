package me.voltual.pyrolysis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import me.voltual.pyrolysis.ROW_DESCRIPTION
import me.voltual.pyrolysis.ROW_ICON
import me.voltual.pyrolysis.ROW_LABEL
import me.voltual.pyrolysis.ROW_NAME
import me.voltual.pyrolysis.ROW_REPOSITORY_ID
import me.voltual.pyrolysis.TABLE_ANTIFEATURE
import me.voltual.pyrolysis.TABLE_ANTIFEATURE_TEMP
import me.voltual.pyrolysis.data.db.entity.AntiFeature
import me.voltual.pyrolysis.data.db.entity.AntiFeatureDetails
import me.voltual.pyrolysis.data.db.entity.AntiFeatureTemp
import kotlinx.coroutines.flow.Flow

@Dao
interface AntiFeatureDao : BaseDao<AntiFeature> {
    @Transaction
    @Query(
        """SELECT $ROW_NAME, $ROW_LABEL, $ROW_DESCRIPTION, $ROW_ICON
        FROM $TABLE_ANTIFEATURE
        GROUP BY $ROW_NAME HAVING 1
        ORDER BY $ROW_REPOSITORY_ID ASC"""
    )
    fun getAllAntiFeatureDetails(): List<AntiFeatureDetails>

    @Transaction
    @Query(
        """SELECT $ROW_NAME, $ROW_LABEL, $ROW_DESCRIPTION, $ROW_ICON
        FROM $TABLE_ANTIFEATURE
        GROUP BY $ROW_NAME HAVING 1
        ORDER BY $ROW_REPOSITORY_ID ASC"""
    )
    fun getAllAntiFeatureDetailsFlow(): Flow<List<AntiFeatureDetails>>

    @Query("DELETE FROM $TABLE_ANTIFEATURE WHERE $ROW_REPOSITORY_ID = :id")
    suspend fun deleteByRepoId(id: Long): Int

    @Query("DELETE FROM $TABLE_ANTIFEATURE")
    suspend fun emptyTable()
}

@Dao
interface AntiFeatureTempDao : BaseDao<AntiFeatureTemp> {
    @Query("SELECT * FROM $TABLE_ANTIFEATURE_TEMP")
    fun getAll(): Array<AntiFeatureTemp>

    @Query("DELETE FROM $TABLE_ANTIFEATURE_TEMP")
    suspend fun emptyTable()
}