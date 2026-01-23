package cc.bbq.xq.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.DeviceNameDataStore
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.UnifiedUserDetail
import cc.bbq.xq.data.unified.UpdateUserProfileParams
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

@KoinViewModel
class UserProfileViewModel(
    private val repositories: Map<AppStore, IAppStoreRepository>,
    private val deviceNameDataStore: DeviceNameDataStore
) : ViewModel() {

    data class UserProfileUiState(
        val isLoading: Boolean = false,
        val userDetail: UnifiedUserDetail? = null,
        val deviceName: String = "",
        val error: String? = null,
        val isUploading: Boolean = false
    )

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(store: AppStore) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 并行获取 DataStore 和网络数据
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
                    throw userResult.exceptionOrNull() ?: Exception("加载用户信息失败")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateProfile(
        store: AppStore,
        params: UpdateUserProfileParams,
        onResult: (Boolean, String) -> Unit // 两个参数：成功标志和消息
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val repository = repositories[store] ?: return@launch onResult(false, "不支持的平台")
                val result = repository.updateUserProfile(params)
                
                if (result.isSuccess) {
                    params.deviceName?.let { deviceNameDataStore.saveDeviceName(it) }
                    loadUserProfile(store)
                    onResult(true, "保存成功")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "更新失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "操作异常")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun uploadAvatar(
        store: AppStore,
        imageFile: File,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val repository = repositories[store] ?: return@launch onResult(false, "不支持的平台")
                val result = repository.uploadAvatar(imageFile.readBytes(), imageFile.name)
                
                if (result.isSuccess) {
                    loadUserProfile(store)
                    onResult(true, "头像上传成功")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "上传失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "文件处理失败")
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }
}