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
import androidx.compose.material.icons.filled.ContentPaste
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
import cc.bbq.xq.data.DeviceConfig
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
    
    // 设备配置状态本地临时变量
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.userDetail, state.deviceConfig) {
        state.userDetail?.let {
            nickname = it.displayName ?: ""
            description = it.description ?: ""
        }
        brand = state.deviceConfig.brand
        model = state.deviceConfig.model
    }

    LaunchedEffect(store) {
        viewModel.loadUserProfile(store)
    }

    if (showImportDialog) {
        ImportConfigDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { json ->
                viewModel.importDeviceConfig(json) { success ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (success) "导入成功" else "解析失败，请检查格式")
                    }
                }
                showImportDialog = false
            }
        )
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
                qqNumber = qqNumber,
                onQqChange = { qqNumber = it },
                description = description,
                onDescriptionChange = { description = it },
                brand = brand,
                onBrandChange = { brand = it },
                model = model,
                onModelChange = { model = it },
                onImportClick = { showImportDialog = true }
            )

            Button(
                onClick = {
                    val params = UpdateUserProfileParams(
                        nickname = nickname,
                        description = description,
                        deviceName = model 
                    )
                    val newConfig = state.deviceConfig.copy(brand = brand, model = model)
                    viewModel.updateProfile(store, params, newConfig) { _, msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("保存全部修改")
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

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
    brand: String,
    onBrandChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    onImportClick: () -> Unit,
    allDevices: List<DeviceConfig>,
    currentDevice: DeviceConfig,
    onDeviceSelect: (DeviceConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("账户信息", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("昵称") },
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
                label = { Text("个性签名") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("设备伪装管理", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onImportClick) {
                Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("导入 Guise")
            }
        }

        // 机型切换下拉菜单
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentDevice.alias.ifEmpty { currentDevice.model },
                onValueChange = {},
                readOnly = true,
                label = { Text("当前选定的机型模板") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allDevices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text("${device.alias} (${device.model})") },
                        onClick = {
                            onDeviceSelect(device)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = brand,
            onValueChange = onBrandChange,
            label = { Text("品牌 (Brand)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("型号 (Model)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ImportConfigDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从 JSON 导入机型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("粘贴 Guise 的 configuration 字段内容：", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("{\"brand\":\"...\", \"model\":\"...\"}") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 辅助组件保持...
@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}