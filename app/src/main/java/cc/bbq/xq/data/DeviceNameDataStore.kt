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

// 定义设备配置模型
@Serializable
data class DeviceConfig(
    val brand: String = "Generic",
    val model: String = "Android Device",
    val product: String = "",
    val device: String = ""
)

private val Context.deviceNameDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_info")

@Single
class DeviceNameDataStore(context: Context) {
    private val DEVICE_CONFIG_KEY = stringPreferencesKey("device_config_json")
    private val dataStore = context.deviceNameDataStore
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    // 提供 Flow 供 UI 订阅
    val deviceConfigFlow: Flow<DeviceConfig> = dataStore.data
        .map { preferences ->
            val jsonStr = preferences[DEVICE_CONFIG_KEY]
            if (jsonStr != null) {
                try {
                    json.decodeFromString<DeviceConfig>(jsonStr)
                } catch (e: Exception) {
                    DeviceConfig()
                }
            } else {
                // 默认值，或者迁移旧的单字符串数据逻辑（如果需要）
                DeviceConfig()
            }
        }

    suspend fun saveDeviceConfig(config: DeviceConfig) {
        dataStore.edit { preferences ->
            preferences[DEVICE_CONFIG_KEY] = json.encodeToString(config)
        }
    }

    /**
     * 从外部（如 Guise 模板）导入配置
     */
    suspend fun importConfigFromJson(configJson: String): Boolean {
        return try {
            val config = json.decodeFromString<DeviceConfig>(configJson)
            saveDeviceConfig(config)
            true
        } catch (e: Exception) {
            false
        }
    }
}