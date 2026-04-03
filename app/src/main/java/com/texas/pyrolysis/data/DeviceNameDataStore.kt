package com.texas.pyrolysis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first // 必须导入此项以支持 .first()
import kotlinx.coroutines.flow.take  // 建议导入
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
data class DeviceConfig(
    val alias: String = "默认机型",
    val brand: String = "Generic",
    val model: String = "Android Device",
    val product: String = "",
    val device: String = "",
    val isSelected: Boolean = false
)

@Serializable
private data class GuiseTemplate(
    val name: String = "未命名机型",
    val configuration: String
)

private val Context.deviceNameDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_info")

@Single
class DeviceNameDataStore(context: Context) {
    private val DEVICE_LIST_KEY = stringPreferencesKey("device_config_list_json")
    private val dataStore = context.deviceNameDataStore

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private fun generateRandomModel() = "${(1000..9999).random()}-${('A'..'Z').random()}"

    // --- 修复部分 ---
    suspend fun applyEmergencyRandomModel() {
        // 1. 确保获取的是 List<DeviceConfig>
        val currentList: List<DeviceConfig> = deviceListFlow.first()
        val randomModel = generateRandomModel()

        val emergencyConfig = DeviceConfig(
            alias = randomModel,
            model = randomModel,
            brand = "Generic",
            isSelected = true
        )

        // 2. 这里的 map 是 List 的扩展函数，不是 Flow 的
        val updatedList = currentList.map { it.copy(isSelected = false) } + emergencyConfig
        updateDeviceList(updatedList)
    }

    val deviceListFlow: Flow<List<DeviceConfig>> = dataStore.data
        .map { preferences ->
            val jsonStr = preferences[DEVICE_LIST_KEY]
            if (!jsonStr.isNullOrEmpty()) {
                try {
                    json.decodeFromString<List<DeviceConfig>>(jsonStr)
                } catch (e: Exception) {
                    listOf(DeviceConfig(isSelected = true))
                }
            } else {
                listOf(DeviceConfig(isSelected = true))
            }
        }

    val currentConfigFlow: Flow<DeviceConfig> = deviceListFlow.map { list ->
        list.find { it.isSelected } ?: list.firstOrNull() ?: DeviceConfig()
    }

    suspend fun updateDeviceList(newList: List<DeviceConfig>) {
        dataStore.edit { preferences ->
            preferences[DEVICE_LIST_KEY] = json.encodeToString(newList)
        }
    }

    suspend fun selectDevice(alias: String) {
        // 使用 first() 获取当前状态并修改，避免 collect 可能导致的无限循环或挂起
        val currentList = deviceListFlow.first()
        val updatedList = currentList.map { it.copy(isSelected = it.alias == alias) }
        updateDeviceList(updatedList)
    }

    suspend fun importConfigsFromJson(configJson: String): Int {
        return try {
            val input = configJson.trim()
            val importedConfigs = mutableListOf<DeviceConfig>()

            when {
                input.startsWith("[") -> {
                    val list = json.decodeFromString<List<GuiseTemplate>>(input)
                    list.forEach { template ->
                        val inner = json.decodeFromString<DeviceConfig>(template.configuration)
                        importedConfigs.add(inner.copy(alias = template.name))
                    }
                }
                input.contains("\"configuration\"") -> {
                    val template = json.decodeFromString<GuiseTemplate>(input)
                    val inner = json.decodeFromString<DeviceConfig>(template.configuration)
                    importedConfigs.add(inner.copy(alias = template.name))
                }
                else -> {
                    val single = json.decodeFromString<DeviceConfig>(input)
                    importedConfigs.add(single)
                }
            }

            if (importedConfigs.isNotEmpty()) {
                val currentList = deviceListFlow.first()
                val combinedList = (currentList + importedConfigs).distinctBy { it.alias + it.model }
                updateDeviceList(combinedList)
                importedConfigs.size
            } else 0
        } catch (e: Exception) {
            0
        }
    }
}