package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    val isSelected: Boolean = false // 标记当前正在使用的配置
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

    // 获取所有机型列表
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

    // 获取当前选中的机型
    val currentConfigFlow: Flow<DeviceConfig> = deviceListFlow.map { list ->
        list.find { it.isSelected } ?: list.firstOrNull() ?: DeviceConfig()
    }

    suspend fun updateDeviceList(newList: List<DeviceConfig>) {
        dataStore.edit { preferences ->
            preferences[DEVICE_LIST_KEY] = json.encodeToString(newList)
        }
    }

    suspend fun selectDevice(alias: String) {
        deviceListFlow.map { list ->
            list.map { it.copy(isSelected = it.alias == alias) }
        }.collect { updateDeviceList(it) }
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
                deviceListFlow.map { currentList ->
                    // 合并列表，去重（以 alias 为准），并保持导入的第一个为选中状态（可选）
                    (currentList + importedConfigs).distinctBy { it.alias + it.model }
                }.collect { updateDeviceList(it) }
                importedConfigs.size
            } else 0
        } catch (e: Exception) {
            0
        }
    }
}