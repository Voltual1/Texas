package me.voltual.pyrolysis.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import me.voltual.pyrolysis.ROW_DESCRIPTION
import me.voltual.pyrolysis.ROW_ICON
import me.voltual.pyrolysis.ROW_LABEL
import me.voltual.pyrolysis.ROW_NAME
import me.voltual.pyrolysis.ROW_REPOSITORY_ID
import me.voltual.pyrolysis.TABLE_ANTIFEATURE
import me.voltual.pyrolysis.TABLE_ANTIFEATURE_TEMP

@Entity(
    tableName = TABLE_ANTIFEATURE,
    primaryKeys = [ROW_REPOSITORY_ID, ROW_NAME],
    indices = [
        Index(value = [ROW_NAME, ROW_LABEL]),
        Index(value = [ROW_NAME, ROW_LABEL, ROW_DESCRIPTION, ROW_ICON]),
        Index(value = [ROW_NAME]),
        Index(value = [ROW_REPOSITORY_ID]),
    ]
)
open class AntiFeature(
    val repositoryId: Long = 0,
    val name: String = "", // map key in index-v2
    val label: String = "", // name in index-v2
    val description: String = "",
    val icon: String = "",
)

@Entity(tableName = TABLE_ANTIFEATURE_TEMP)
class AntiFeatureTemp(
    repositoryId: Long,
    name: String,
    label: String,
    description: String,
    icon: String
) : AntiFeature(repositoryId, name, label, description, icon)

data class AntiFeatureDetails(
    val name: String,
    val label: String,
    val description: String = "",
    val icon: String = "",
)