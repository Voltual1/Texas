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

    // 1. 聚合状态：所有 UI 关心的属性都在这里
    data class UserProfileUiState(
        val isLoading: Boolean = false,
        val userDetail: UnifiedUserDetail? = null,
        val deviceName: String = "",
        val error: String? = null,
        val isUploading: Boolean = false
    )

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // 2. 统一初始化入口
    fun loadUserProfile(store: AppStore) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 并发执行：同时获取设备名和用户信息
                val deviceNameDeferred = async { deviceNameDataStore.deviceNameFlow.first() }
                val repository = repositories[store] ?: throw Exception("不支持的平台")
                val userResult = repository.getCurrentUserDetail()

                if (userResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userDetail = userResult.getOrNull(),
                            deviceName = deviceNameDeferred.await()
                        )
                    }
                } else {
                    throw userResult.exceptionOrNull() ?: Exception("未知错误")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // 3. 业务动作：更新资料
    // 使用回调或 Result 返回结果，方便 UI 弹出 Snackbar
    fun updateProfile(
        store: AppStore, 
        params: UpdateUserProfileParams, 
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val repository = repositories[store] ?: return@launch onResult(false, "不支持的平台")
                val result = repository.updateUserProfile(params)
                
                if (result.isSuccess) {
                    // 如果改了设备名，同步到 DataStore
                    params.deviceName?.let { deviceNameDataStore.saveDeviceName(it) }
                    
                    // 刷新最新数据
                    loadUserProfile(store)
                    onResult(true, "修改成功")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "更新失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络异常")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 4. 业务动作：上传头像
    fun uploadAvatar(
        store: AppStore, 
        imageFile: File, 
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val repository = repositories[store] ?: return@launch onResult(false, "不支持的平台")
                
                // IO 操作已经在协程中，readBytes() 是安全的
                val result = repository.uploadAvatar(imageFile.readBytes(), imageFile.name)
                
                if (result.isSuccess) {
                    loadUserProfile(store)
                    onResult(true, "头像上传成功")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "上传失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "文件读取失败")
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }
}