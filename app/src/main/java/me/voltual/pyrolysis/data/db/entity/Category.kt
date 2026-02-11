package me.voltual.pyrolysis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import me.voltual.pyrolysis.ROW_NAME
import me.voltual.pyrolysis.ROW_PACKAGE_NAME
import me.voltual.pyrolysis.ROW_REPOSITORY_ID
import me.voltual.pyrolysis.TABLE_CATEGORY
import me.voltual.pyrolysis.TABLE_CATEGORY_TEMP

@Entity(
    tableName = TABLE_CATEGORY,
    primaryKeys = [ROW_REPOSITORY_ID, ROW_PACKAGE_NAME, ROW_NAME],
    indices = [
        Index(value = [ROW_REPOSITORY_ID, ROW_PACKAGE_NAME, ROW_NAME], unique = true),
        Index(value = [ROW_PACKAGE_NAME, ROW_NAME]),
        Index(value = [ROW_NAME]),
        Index(value = [ROW_REPOSITORY_ID]),
    ]
)
open class Category(
    val repositoryId: Long = 0,
    val packageName: String = "",
    val name: String = "",
)

@Entity(tableName = TABLE_CATEGORY_TEMP)
class CategoryTemp(repositoryId: Long, packageName: String, name: String) :
    Category(repositoryId, packageName, name)