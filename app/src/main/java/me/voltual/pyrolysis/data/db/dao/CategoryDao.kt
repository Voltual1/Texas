package me.voltual.pyrolysis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.voltual.pyrolysis.ROW_ENABLED
import me.voltual.pyrolysis.ROW_ID
import me.voltual.pyrolysis.ROW_NAME
import me.voltual.pyrolysis.ROW_REPOSITORY_ID
import me.voltual.pyrolysis.TABLE_CATEGORY
import me.voltual.pyrolysis.TABLE_CATEGORY_TEMP
import me.voltual.pyrolysis.TABLE_REPOSITORY
import me.voltual.pyrolysis.data.db.entity.Category
import me.voltual.pyrolysis.data.db.entity.CategoryTemp
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : BaseDao<Category> {
    @Query(
        """SELECT DISTINCT $TABLE_CATEGORY.$ROW_NAME
        FROM $TABLE_CATEGORY
        JOIN $TABLE_REPOSITORY
        ON $TABLE_CATEGORY.$ROW_REPOSITORY_ID = $TABLE_REPOSITORY.$ROW_ID
        WHERE $TABLE_REPOSITORY.$ROW_ENABLED != 0"""
    )
    fun getAllNames(): List<String>

    @Query(
        """SELECT DISTINCT $TABLE_CATEGORY.$ROW_NAME
        FROM $TABLE_CATEGORY
        JOIN $TABLE_REPOSITORY
        ON $TABLE_CATEGORY.$ROW_REPOSITORY_ID = $TABLE_REPOSITORY.$ROW_ID
        WHERE $TABLE_REPOSITORY.$ROW_ENABLED != 0"""
    )
    fun getAllNamesFlow(): Flow<List<String>>

    @Query("DELETE FROM $TABLE_CATEGORY WHERE $ROW_REPOSITORY_ID = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM $TABLE_CATEGORY")
    suspend fun emptyTable()
}

@Dao
interface CategoryTempDao : BaseDao<CategoryTemp> {
    @Query("SELECT * FROM $TABLE_CATEGORY_TEMP")
    fun getAll(): Array<CategoryTemp>

    @Query("DELETE FROM $TABLE_CATEGORY_TEMP")
    suspend fun emptyTable()
}