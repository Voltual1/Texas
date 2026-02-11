package me.voltual.pyrolysis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import me.voltual.pyrolysis.ROW_ICON
import me.voltual.pyrolysis.ROW_LABEL
import me.voltual.pyrolysis.ROW_NAME
import me.voltual.pyrolysis.ROW_REPOSITORY_ID
import me.voltual.pyrolysis.TABLE_REPOCATEGORY
import me.voltual.pyrolysis.TABLE_REPOCATEGORY_TEMP

@Entity(
    tableName = TABLE_REPOCATEGORY,
    primaryKeys = [ROW_REPOSITORY_ID, ROW_NAME],
    indices = [
        Index(value = [ROW_REPOSITORY_ID, ROW_NAME], unique = true),
        Index(value = [ROW_NAME, ROW_LABEL]),
        Index(value = [ROW_NAME, ROW_LABEL, ROW_ICON]),
        Index(value = [ROW_REPOSITORY_ID]),
        Index(value = [ROW_NAME]),
    ]
)
open class RepoCategory(
    val repositoryId: Long = 0,
    val name: String = "", // map key in index-v2
    val label: String = "", // name in index-v2
    val icon: String = "",
)

@Entity(tableName = TABLE_REPOCATEGORY_TEMP)
class RepoCategoryTemp(repositoryId: Long, name: String, label: String, icon: String) :
    RepoCategory(repositoryId, name, label, icon)

data class CategoryDetails(
    val name: String,
    val label: String,
    val icon: String = "",
)