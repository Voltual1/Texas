//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.user

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.DeviceNameDataStore
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.UnifiedUserDetail
import cc.bbq.xq.data.unified.UpdateUserProfileParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UserProfileViewModel(
    private val repositories: Map<AppStore, IAppStoreRepository>,
    private val deviceNameDataStore: DeviceNameDataStore
) : ViewModel() {
    
    // UI 状态
    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState
    
    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName
    
    // 加载用户信息
    fun loadUserProfile(store: AppStore) {
        viewModelScope.launch {
            _uiState.value = UserProfileUiState.Loading
            
            try {
                // 加载设备名称
                loadDeviceName()
                
                // 加载用户信息
                val repository = repositories[store]
                if (repository == null) {
                    _uiState.value = UserProfileUiState.Error("不支持的平台: $store")
                    return@launch
                }
                
                val userResult = repository.getCurrentUserDetail()
                if (userResult.isSuccess) {
                    _uiState.value = UserProfileUiState.Success(
                        userDetail = userResult.getOrNull(),
                        store = store
                    )
                } else {
                    _uiState.value = UserProfileUiState.Error("加载用户信息失败: ${userResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error("加载失败: ${e.message}")
            }
        }
    }
    
    // 加载设备名称
    private suspend fun loadDeviceName() {
        _deviceName.value = deviceNameDataStore.deviceNameFlow.first()
    }
    
    // 保存设备名称
    fun saveDeviceName(newDeviceName: String) {
        viewModelScope.launch {
            deviceNameDataStore.saveDeviceName(newDeviceName)
            _deviceName.value = newDeviceName
        }
    }
    
    // 更新用户资料
    fun updateUserProfile(
        store: AppStore,
        params: UpdateUserProfileParams,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val repository = repositories[store]
                if (repository == null) {
                    onError("不支持的平台: $store")
                    return@launch
                }
                
                val result = repository.updateUserProfile(params)
                if (result.isSuccess) {
                    // 保存设备名称（如果是本地设置）
                    if (!params.deviceName.isNullOrEmpty()) {
                        saveDeviceName(params.deviceName)
                    }
                    
                    // 重新加载用户信息
                    loadUserProfile(store)
                    onSuccess()
                } else {
                    onError("更新失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onError("更新失败: ${e.message}")
            }
        }
    }
    
    // 上传头像
    fun uploadAvatar(
        store: AppStore,
        imageFile: File,
        onProgress: (String) -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                onProgress("上传头像中...")
                
                val repository = repositories[store]
                if (repository == null) {
                    onError("不支持的平台: $store")
                    return@launch
                }
                
                val imageBytes = imageFile.readBytes()
                val result = repository.uploadAvatar(imageBytes, imageFile.name)
                
                if (result.isSuccess) {
                    // 重新加载用户信息以更新头像
                    loadUserProfile(store)
                    onSuccess()
                } else {
                    onError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onError("上传错误: ${e.message}")
            }
        }
    }
}

// UI 状态密封类
sealed class UserProfileUiState {
    object Loading : UserProfileUiState()
    data class Success(
        val userDetail: UnifiedUserDetail?,
        val store: AppStore
    ) : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
}