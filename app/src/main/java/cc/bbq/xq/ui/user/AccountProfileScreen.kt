package cc.bbq.xq.ui.user

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.unified.UpdateUserProfileParams
import cc.bbq.xq.ui.theme.BBQSnackbarHost
import cc.bbq.xq.util.FileUtil
import coil3.compose.rememberAsyncImagePainter
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AccountProfileScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    store: AppStore = AppStore.XIAOQU_SPACE,
    viewModel: UserProfileViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 观察 ViewModel 状态
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()

    // 本地 UI 状态
    var nickname by rememberSaveable { mutableStateOf("") }
    var qqNumber by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var localDeviceName by rememberSaveable { mutableStateOf("") }
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }
    var progressMessage by rememberSaveable { mutableStateOf("") }

    // 初始化加载
    LaunchedEffect(store) {
        viewModel.loadUserProfile(store)
    }

    // 更新本地状态
    LaunchedEffect(uiState, deviceName) {
        val state = uiState
        if (state is UserProfileUiState.Success) {
            val userDetail = state.userDetail
            when (state.store) {
                AppStore.XIAOQU_SPACE -> {
                    nickname = userDetail?.displayName ?: ""
                    // QQ号通常不在 userDetail 直接返回，或根据你的 API 结构赋值
                }
                AppStore.SIENE_SHOP, AppStore.LING_MARKET -> {
                    displayName = userDetail?.displayName ?: ""
                    description = userDetail?.description ?: ""
                }
                else -> {}
            }
        }
        localDeviceName = deviceName
    }

    // 错误处理
    LaunchedEffect(uiState) {
        if (uiState is UserProfileUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UserProfileUiState.Error).message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { BBQSnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 表单部分 ---
            when (store) {
                AppStore.XIAOQU_SPACE -> {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("修改昵称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = qqNumber,
                        onValueChange = { qqNumber = it },
                        label = { Text("修改QQ号") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AppStore.SIENE_SHOP, AppStore.LING_MARKET -> {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(if (store == AppStore.LING_MARKET) "昵称" else "外显名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (store == AppStore.LING_MARKET) "个性签名" else "个人描述") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = localDeviceName,
                onValueChange = { localDeviceName = it },
                label = { Text("设备名称") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AvatarUploadSection(
                avatarUri = avatarUri,
                userDetail = (uiState as? UserProfileUiState.Success)?.userDetail,
                onAvatarSelected = { uri ->
                    avatarUri = uri
                    coroutineScope.launch {
                        uploadAvatar(
                            context = context,
                            uri = uri,
                            store = store,
                            viewModel = viewModel,
                            onProgress = { 
                                progressMessage = it
                                showProgressDialog = true 
                            },
                            onComplete = {
                                showProgressDialog = false
                                snackbarHostState.showSnackbar("头像上传成功")
                            },
                            onError = { 
                                showProgressDialog = false
                                snackbarHostState.showSnackbar(it)
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 保存按钮 
            Button(
                onClick = {
                    val params = when (store) {
                        AppStore.XIAOQU_SPACE -> UpdateUserProfileParams(
                            nickname = nickname,
                            qqNumber = qqNumber,
                            deviceName = localDeviceName
                        )
                        AppStore.SIENE_SHOP -> UpdateUserProfileParams(
                            displayName = displayName,
                            description = description,
                            deviceName = localDeviceName
                        )
                        AppStore.LING_MARKET -> UpdateUserProfileParams(
                            nickname = displayName,
                            description = description,
                            deviceName = localDeviceName
                        )
                        else -> UpdateUserProfileParams(deviceName = localDeviceName)
                    }

                    viewModel.updateUserProfile(
                        store = store,
                        params = params,
                        onSuccess = {
                            coroutineScope.launch { snackbarHostState.showSnackbar("修改保存成功") }
                        },
                        onError = { error ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is UserProfileUiState.Loading
            ) {
                if (uiState is UserProfileUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存修改")
                }
            }
        }

        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("上传中") },
                text = { Text(progressMessage) },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun AvatarUploadSection(
    avatarUri: Uri?,
    userDetail: cc.bbq.xq.data.unified.UnifiedUserDetail?,
    onAvatarSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { onAvatarSelected(it) }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        val painter = when {
            avatarUri != null -> rememberAsyncImagePainter(avatarUri)
            !userDetail?.avatarUrl.isNullOrEmpty() -> rememberAsyncImagePainter(userDetail?.avatarUrl)
            else -> null
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "用户头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(80.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            ImagePicker.with(context as Activity)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent { launcher.launch(it) }
        }) {
            Text("选择头像")
        }
    }
}

suspend fun uploadAvatar(
    context: Context,
    uri: Uri,
    store: AppStore,
    viewModel: UserProfileViewModel,
    onProgress: (String) -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        onProgress("正在获取文件...")
        val realPath = FileUtil.getRealPathFromURI(context, uri)
        if (realPath == null) {
            onError("无法获取图片路径")
            return
        }
        viewModel.uploadAvatar(
            store = store,
            imageFile = File(realPath),
            onProgress = onProgress,
            onSuccess = onComplete,
            onError = onError
        )
    } catch (e: Exception) {
        onError("上传错误: ${e.message}")
    }
}