package me.voltual.pyrolysis.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import me.voltual.pyrolysis.data.db.entity.Tracker
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao : BaseDao<Tracker> {
    @Query("SELECT * FROM tracker")
    fun getAll(): List<Tracker>

    @Query("SELECT * FROM tracker")
    fun getAllFlow(): Flow<List<Tracker>>

    @Query("SELECT * FROM tracker WHERE key = :key")
    fun get(key: Int): Tracker?

    @Query("SELECT * FROM tracker WHERE key = :key")
    fun getFlow(key: Int): Flow<Tracker?>

    @Transaction
    suspend fun multipleUpserts(updates: Collection<Tracker>) {
        updates.forEach { metadata ->
            upsert(metadata)
        }
    }
}
