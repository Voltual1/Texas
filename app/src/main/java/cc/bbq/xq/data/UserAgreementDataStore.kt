package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

// 定义当前的协议版本号（当协议修订时，只需增加这里的数字）
object AgreementVersions {
    const val USER_AGREEMENT = 1
    const val XIAOQU_AGREEMENT = 1 
    const val SINE_PRIVACY = 1
    const val SINE_AGREEMENT = 1
    const val WYSMARKET_PRIVACY = 1
}

private val Context.userAgreementDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_agreement_prefs")

@Single
class UserAgreementDataStore(context: Context) {

    private val dataStore = context.userAgreementDataStore

    private object Keys {
        val USER_AGREEMENT_VER = intPreferencesKey("user_agreement_ver")
        val XIAOQU_AGREEMENT_VER = intPreferencesKey("xiaoqu_user_agreement_ver")
        val SINE_AGREEMENT_VER = intPreferencesKey("sine_user_agreement_ver")
        val SINE_PRIVACY_VER = intPreferencesKey("sine_privacy_policy_ver")
        val WYSMARKET_AGREEMENT_VER = intPreferencesKey("wysappmarket_user_agreement_ver")
        val WYSMARKET_PRIVACY_VER = intPreferencesKey("wysappmarket_privacy_policy_ver")
    }

    // --- 通用判断逻辑 ---
    private fun isAccepted(key: Preferences.Key<Int>, currentVersion: Int): Flow<Boolean> =
        dataStore.data.map { prefs ->
            (prefs[key] ?: 0) >= currentVersion
        }

    // --- 对外暴露的 Flow (对应 MainActivity 监听) ---
    val isUserAgreementAccepted = isAccepted(Keys.USER_AGREEMENT_VER, AgreementVersions.USER_AGREEMENT)
    val isXiaoquAccepted = isAccepted(Keys.XIAOQU_AGREEMENT_VER, AgreementVersions.XIAOQU_AGREEMENT)
    val isSineAgreementAccepted = isAccepted(Keys.SINE_AGREEMENT_VER, AgreementVersions.SINE_AGREEMENT)
    val isSinePrivacyAccepted = isAccepted(Keys.SINE_PRIVACY_VER, AgreementVersions.SINE_PRIVACY)
    val isWysMarketAgreementAccepted = isAccepted(Keys.WYSMARKET_AGREEMENT_VER, AgreementVersions.WYSMARKET_PRIVACY)
    val isWysMarketPrivacyAccepted = isAccepted(Keys.WYSMARKET_PRIVACY_VER, AgreementVersions.WYSMARKET_PRIVACY)

    // --- 写入方法 (对应 Dialog 调用) ---
    suspend fun acceptUserAgreement() = saveVersion(Keys.USER_AGREEMENT_VER, AgreementVersions.USER_AGREEMENT)
    suspend fun acceptXiaoquAgreement() = saveVersion(Keys.XIAOQU_AGREEMENT_VER, AgreementVersions.XIAOQU_AGREEMENT)
    suspend fun acceptSineAgreement() = saveVersion(Keys.SINE_AGREEMENT_VER, AgreementVersions.SINE_PRIVACY)
    suspend fun acceptSinePrivacy() = saveVersion(Keys.SINE_PRIVACY_VER, AgreementVersions.SINE_PRIVACY)
    suspend fun acceptWysMarketAgreement() = saveVersion(Keys.WYSMARKET_AGREEMENT_VER, AgreementVersions.WYSMARKET_PRIVACY)
    suspend fun acceptWysMarketPrivacy() = saveVersion(Keys.WYSMARKET_PRIVACY_VER, AgreementVersions.WYSMARKET_PRIVACY)

    private suspend fun saveVersion(key: Preferences.Key<Int>, version: Int) {
        dataStore.edit { it[key] = version }
    }
}