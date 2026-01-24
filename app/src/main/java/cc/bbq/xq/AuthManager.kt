//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import androidx.datastore.dataStore
import cc.bbq.xq.data.proto.UserCredentials
import cc.bbq.xq.data.proto.UserCredentialsSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

// 定义扩展属性，将 Serializer 与 Tink 绑定
private val Context.credentialsStore: DataStore<UserCredentials> by dataStore(
    fileName = "user_credentials_v2.pb", // 建议换个文件名以区分旧的 EncryptedFile
    serializer = UserCredentialsSerializer(AuthManager.getAead())
)

object AuthManager {
    private lateinit var aead: Aead
    private const val KEYSET_NAME = "master_keyset"
    private const val PREF_FILE_NAME = "tink_auth_prefs"
    private const val MASTER_KEY_URI = "android-keystore://auth_master_key"

    fun getAead(): Aead = aead

    // --- 1. 初始化 (必须在 Application 调用) ---
fun initialize(context: Context) {
        AeadConfig.register()
        
        // Android 下管理 Keyset 的标准做法
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        //  按照官方示例，传入配置和 class 类型
        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    // --- 2. 保存逻辑 (保持原方法签名) ---
    suspend fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        context.credentialsStore.updateData { current ->
            current.toBuilder()
                .setUsername(username)
                .setPassword(password)
                .setToken(token)
                .setUserId(userId)
                .setDeviceId(current.deviceId.ifEmpty { generateDeviceId() })
                .build()
        }
    }

    suspend fun saveSineMarketToken(context: Context, token: String) {
        context.credentialsStore.updateData { it.toBuilder().setSineMarketToken(token).build() }
    }

    suspend fun saveSineOpenMarketToken(context: Context, token: String) {
        context.credentialsStore.updateData { it.toBuilder().setSineOpenMarketToken(token).build() }
    }

    // 新增：保存灵应用商店Token
    suspend fun saveLingMarketToken(context: Context, token: String) {
        context.credentialsStore.updateData { it.toBuilder().setLingMarketToken(token).build() }
    }

    // --- 3. 获取逻辑 (Flow 保持一致) ---
    fun getCredentials(context: Context): Flow<UserCredentials?> = context.credentialsStore.data

    fun getSineMarketToken(context: Context): Flow<String> = 
        getCredentials(context).map { it?.sineMarketToken ?: "" }

    fun getSineOpenMarketToken(context: Context): Flow<String> = 
        getCredentials(context).map { it?.sineOpenMarketToken ?: "" }

    // 新增：获取灵应用商店Token
    fun getLingMarketToken(context: Context): Flow<String> = 
        getCredentials(context).map { it?.lingMarketToken ?: "" }

    fun getUserId(context: Context): Flow<Long> = 
        getCredentials(context).map { it?.userId ?: -1L }

    fun getDeviceId(context: Context): Flow<String> = 
        getCredentials(context).map { it?.deviceId ?: generateDeviceId() }

    // --- 4. 其他操作 ---
    suspend fun clearCredentials(context: Context) {
        context.credentialsStore.updateData { UserCredentials.getDefaultInstance() }
    }

    private fun generateDeviceId(): String = (1..15).map { (0..9).random() }.joinToString("")

    // --- 迁移 SharedPreferences 到加密存储 ---
    suspend fun migrateFromSharedPreferences(context: Context) {
        val sharedPrefs = context.getSharedPreferences("bbq_auth", Context.MODE_PRIVATE)

        // 检查是否存在旧数据
        val encodedUser = sharedPrefs.getString("username", null)
        val encodedPass = sharedPrefs.getString("password", null)
        val token = sharedPrefs.getString("usertoken", null)
        val userId = sharedPrefs.getLong("userid", -1)
        
        if (encodedUser != null && encodedPass != null && token != null && userId != -1L) {
            val username = String(Base64.decode(encodedUser, Base64.DEFAULT))
            val password = String(Base64.decode(encodedPass, Base64.DEFAULT))
            val deviceId = sharedPrefs.getString("device_id", null) ?: generateDeviceId()

            // 写入新的 DataStore
            context.credentialsStore.updateData { current ->
                current.toBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setToken(token)
                    .setUserId(userId)
                    .setDeviceId(deviceId)
                    .build()
            }

            // 清除旧的 SharedPreferences
            sharedPrefs.edit().clear().apply()
            
            // --- 关键修正：清理旧的 androidx.security 遗留物理文件 ---
            // 注意：这里需要 import java.io.File
            try {
                val oldEncryptedFile = File(context.filesDir, "user_credentials_encrypted.pb")
                if (oldEncryptedFile.exists()) {
                    oldEncryptedFile.delete()
                }
                
                // 同时清理可能存在的旧 DataStore 备份（如果之前有用过 DataStore Preferences）
                val oldDataStoreFile = File(context.filesDir, "datastore/auth_preferences.pb")
                if (oldDataStoreFile.exists()) {
                    oldDataStoreFile.delete()
                }
            } catch (e: Exception) {
                // 仅记录日志，不影响主流程
                e.printStackTrace()
            }
        }
    }
}