package me.voltual.pyrolysis.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.voltual.pyrolysis.ROW_ALLOW_UNSTABLE
import me.voltual.pyrolysis.ROW_FAVORITE
import me.voltual.pyrolysis.ROW_IGNORED_VERSION
import me.voltual.pyrolysis.ROW_IGNORE_UPDATES
import me.voltual.pyrolysis.ROW_IGNORE_VULNS
import me.voltual.pyrolysis.ROW_PACKAGE_NAME
import me.voltual.pyrolysis.TABLE_EXTRAS
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(
    tableName = TABLE_EXTRAS,
    indices = [
        Index(value = [ROW_PACKAGE_NAME], unique = true),
        Index(value = [ROW_FAVORITE]),
        Index(value = [ROW_IGNORE_VULNS]),
        Index(value = [ROW_IGNORE_UPDATES]),
        Index(value = [ROW_IGNORED_VERSION]),
        Index(value = [ROW_ALLOW_UNSTABLE]),
    ]
)
@Serializable
data class Extras(
    @PrimaryKey
    val packageName: String = "",
    val favorite: Boolean = false,
    val ignoreUpdates: Boolean = false,
    val ignoredVersion: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val ignoreVulns: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val allowUnstable: Boolean = false,
) {
    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Extras>(json)
    }
}