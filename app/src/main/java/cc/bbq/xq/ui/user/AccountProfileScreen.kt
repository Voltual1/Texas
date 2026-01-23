package cc.bbq.xq.ui.user

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AccountProfileScreen(
    snackbarHostState: SnackbarHostState,
    store: AppStore,
        modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var nickname by remember { mutableStateOf("") }
var qqNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var localDeviceName by remember { mutableStateOf("") }

    LaunchedEffect(state.userDetail, state.deviceName) {
        state.userDetail?.let {
            nickname = it.displayName ?: ""
            description = it.description ?: ""
        }
        localDeviceName = state.deviceName
    }

    LaunchedEffect(store) {
        viewModel.loadUserProfile(store)
    }

    Scaffold(
        snackbarHost = { BBQSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            
            AvatarSection(
                currentUrl = state.userDetail?.avatarUrl,
                onImageSelected = { uri ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val path = FileUtil.getRealPathFromURI(context, uri)
                        if (path != null) {
                            // 修复：这里改为两个参数 (success, msg)
                            viewModel.uploadAvatar(store, File(path)) { _, msg ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(32.dp))

            ProfileFields(
    store = store,
    nickname = nickname,
    onNicknameChange = { nickname = it },
    qqNumber = qqNumber,           // <--- 补上这一行
    onQqChange = { qqNumber = it }, // <--- 补上这一行
    description = description,
    onDescriptionChange = { description = it },
    deviceName = localDeviceName,
    onDeviceNameChange = { localDeviceName = it }
)

            Button(
                onClick = {
                    val params = UpdateUserProfileParams(
                        nickname = nickname,
                        description = description,
                        deviceName = localDeviceName
                    )
                    // 修复：这里改为两个参数 (success, msg)
                    viewModel.updateProfile(store, params) { _, msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text("保存修改")
            }
        }
    }
}

@Composable
fun AvatarSection(
    currentUrl: String?,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { onImageSelected(it) }
        }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        val painter = rememberAsyncImagePainter(currentUrl)
        
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            if (!currentUrl.isNullOrEmpty()) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 悬浮相机按钮
        SmallFloatingActionButton(
            onClick = {
                ImagePicker.with(context as Activity)
                    .cropSquare()
                    .compress(1024)
                    .maxResultSize(512, 512)
                    .createIntent { launcher.launch(it) }
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "更换头像", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ProfileFields(
    store: AppStore,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    qqNumber: String,
    onQqChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    deviceName: String,
    onDeviceNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 根据不同平台显示不同的 UI 标签
        val nicknameLabel = when (store) {
            AppStore.XIAOQU_SPACE -> "修改昵称"
            AppStore.LING_MARKET -> "市场昵称"
            else -> "外显名称"
        }

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text(nicknameLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (store == AppStore.XIAOQU_SPACE) {
            OutlinedTextField(
                value = qqNumber,
                onValueChange = onQqChange,
                label = { Text("QQ 号码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (store == AppStore.SIENE_SHOP || store == AppStore.LING_MARKET) {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("个性签名 / 描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

        OutlinedTextField(
            value = deviceName,
            onValueChange = onDeviceNameChange,
            label = { Text("设备名称（仅本地存储不会云同步）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("该名称仅用于区分您的不同设备") }
        )
    }
}

// 辅助组件：带尺寸限制的 Loading
@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}