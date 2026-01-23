// /app/src/main/java/cc/bbq/xq/ui/user/AccountProfileScreen.kt
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
    
    // 更新本地状态当 ViewModel 状态变化时
    LaunchedEffect(uiState, deviceName) {
        when (val state = uiState) {
            is UserProfileUiState.Success -> {
                val userDetail = state.userDetail
                when (state.store) {
                    AppStore.XIAOQU_SPACE -> {
                        nickname = userDetail?.displayName ?: ""
                        displayName = userDetail?.displayName ?: ""
                    }
                    AppStore.SIENE_SHOP -> {
                        displayName = userDetail?.displayName ?: ""
                        description = userDetail?.description ?: ""
                    }
                    else -> {
                        // 其他平台不需要设置
                    }
                }
            }
            else -> {
                // 处理其他状态
            }
        }
        localDeviceName = deviceName
    }
    
    // 错误处理
    LaunchedEffect(uiState) {
        if (uiState is UserProfileUiState.Error) {
            val errorState = uiState as UserProfileUiState.Error
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorState.message,
                    duration = SnackbarDuration.Short
                )
            }
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
                AppStore.SIENE_SHOP -> {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("外显名称") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("个人描述") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    // 其他平台不需要显示输入字段
                }
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
                store = store,
                onAvatarSelected = { uri ->
                    coroutineScope.launch {
                        uploadAvatar(
                            context = context,
                            uri = uri,
                            store = store,
                            viewModel = viewModel,
                            onProgress = { message ->
                                showProgressDialog = true
                                progressMessage = message
                            },
                            onComplete = {
                                showProgressDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "头像上传成功",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onError = { error ->
                                showProgressDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = error,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val params = UpdateUserProfileParams(
                        nickname = if (store == AppStore.XIAOQU_SPACE) nickname else null,
                        qqNumber = if (store == AppStore.XIAOQU_SPACE) qqNumber else null,
                        displayName = if (store == AppStore.SIENE_SHOP) displayName else null,
                        description = if (store == AppStore.SIENE_SHOP) description else null,
                        deviceName = localDeviceName
                    )
                    
                    viewModel.updateUserProfile(
                        store = store,
                        params = params,
                        onSuccess = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "修改保存成功",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onError = { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = error,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is UserProfileUiState.Loading
            ) {
                if (uiState is UserProfileUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存修改")
                }
            }
        }

        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
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
    store: AppStore,
    onAvatarSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                onAvatarSelected(uri)
            }
        }
    }

    val startImagePicker = {
        ImagePicker.with(context as Activity)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                launcher.launch(intent)
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when {
            avatarUri != null -> {
                Image(
                    painter = rememberAsyncImagePainter(model = avatarUri),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            userDetail?.avatarUrl != null && userDetail.avatarUrl.isNotEmpty() -> {
                Image(
                    painter = rememberAsyncImagePainter(model = userDetail.avatarUrl),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "选择头像",
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { startImagePicker() }) {
            Text("选择头像")
        }
    }
}