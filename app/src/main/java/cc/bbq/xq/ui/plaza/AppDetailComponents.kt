//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.plaza

// --- 导入语句 ---

// 用于 @Composable 注解和基础 Compose UI 元素
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 用于 AppStore 枚举
import cc.bbq.xq.AppStore

// 用于数据模型
import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.LingMarketClient

// 用于时间格式化工具 (formatTimestamp)
import cc.bbq.xq.util.formatTimestamp

// 用于 Coil 图片加载 (如果 InfoRow 或其他组件需要 ImageRequest 等，虽然当前没用到，但为未来兼容性保留)
// import coil3.request.ImageRequest
// import coil3.request.CachePolicy

// --- Composable 组件 ---

@Composable
fun XiaoquSpaceAppInfo(appDetail: UnifiedAppDetail) {
    val raw = appDetail.raw as? cc.bbq.xq.KtorClient.AppDetail
    InfoRow(
        label = "应用类型",
        value = appDetail.type
    )
    InfoRow(
        label = "下载次数",
        value = "${appDetail.downloadCount} 次"
    )
    if (appDetail.size != null) {
        InfoRow(
            label = "安装包大小",
            value = appDetail.size
        )
    }
    InfoRow(
        label = "上传时间",
        value = raw?.create_time ?: "未知"
    )
    InfoRow(
        label = "更新时间",
        value = raw?.update_time ?: "未知"
    )
}

@Composable
fun SineShopAppInfo(appDetail: UnifiedAppDetail) {
    val raw = appDetail.raw as? cc.bbq.xq.SineShopClient.SineShopAppDetail
    val deviceInfo = getDeviceInfo(raw?.app_sdk_min ?: 0)
    InfoRow(
        label = "应用类型",
        value = appDetail.type
    )
    InfoRow(
        label = "版本类型",
        value = raw?.app_version_type ?: "未知"
    )

    // 支持系统信息（包含最低SDK、目标SDK和设备兼容性）
    val supportSystem = buildString {
        append("最低SDK: ${raw?.app_sdk_min ?: "未知"}")
        if (raw?.app_sdk_target != null && raw.app_sdk_target != raw.app_sdk_min) {
            append(" (目标SDK: ${raw.app_sdk_target})")
        }
        append(" • ")
        append(deviceInfo)
    }
    InfoRow(
        label = "支持系统",
        value = supportSystem
    )

    if (appDetail.size != null) {
        InfoRow(
            label = "安装包大小",
            value = appDetail.size
        )
    }
    InfoRow(
        label = "下载次数",
        value = "${appDetail.downloadCount} 次"
    )
    InfoRow(
        label = "应用开发者",
        value = raw?.app_developer ?: "未知"
    )
    InfoRow(
        label = "应用来源",
        value = raw?.app_source ?: "未知"
    )
    InfoRow(
        label = "上传时间",
        value = if (raw?.upload_time != null) formatTimestamp(raw.upload_time) else "未知"
    )
    InfoRow(
        label = "资料时间",
        value = if (raw?.update_time != null) formatTimestamp(raw.update_time) else "未知"
    )

    // 显示应用标签
    if (!raw?.tags.isNullOrEmpty()) {
        InfoRow(
            label = "应用标签",
            value = raw?.tags?.joinToString(", ") { it.name } ?: ""
        )
    }

    // 显示审核状态（如果有审核失败的情况）
    if (raw?.audit_status == 0 && !raw.audit_reason.isNullOrEmpty()) {
        InfoRow(
            label = "审核状态",
            value = raw.audit_reason
        )
    }
}

@Composable
fun LingMarketAppInfo(appDetail: UnifiedAppDetail) {
    val raw = appDetail.raw as? LingMarketClient.LingMarketApp

    // SDK 信息
    val minSdk = raw?.minSdk
    val targetSdk = raw?.targetSdk
    if (minSdk != null && targetSdk != null) {
        InfoRow(
            label = "SDK",
            value = "Min $minSdk / Target $targetSdk"
        )
    } else if (minSdk != null) {
        InfoRow(
            label = "SDK",
            value = "Min $minSdk"
        )
    }

    // 架构信息
    val architectures = raw?.architectures
    val archText = architectures?.joinToString(", ") ?: "未知"
    InfoRow(
        label = "Arch",
        value = archText
    )

    // 下载量
    InfoRow(
        label = "下载量",
        value = "${appDetail.downloadCount ?: 0}"
    )

    // 创建时间
    val createdAt = raw?.createdAt
    createdAt?.let {
        // 尝试格式化日期 (2026-01-14T12:54:00.499Z -> 2026-01-14)
        val formattedDate = try {
            it.substring(0, 10)
        } catch (e: Exception) {
            it
        }
        InfoRow(
            label = "创建于",
            value = formattedDate
        )
    }

    // 包名
    val packageName = raw?.packageName
    InfoRow(
        label = "包名",
        value = packageName ?: "未知"
    )

    // 应用类型
    InfoRow(
        label = "应用类型",
        value = appDetail.type
    )

    // 安装包大小
    if (appDetail.size != null) {
        InfoRow(
            label = "安装包大小",
            value = appDetail.size
        )
    }

    // 支持设备类型
    val supportedDevices = raw?.supportedDevices
    val devicesText = supportedDevices?.joinToString(", ") ?: "未知"
    if (devicesText.isNotEmpty() && devicesText != "未知") {
        InfoRow(
            label = "支持设备",
            value = devicesText
        )
    }

    // 支持的屏幕密度
    val supportedDensities = raw?.supportedDensities
    supportedDensities?.takeIf { it.isNotEmpty() }?.let { densities ->
        InfoRow(
            label = "屏幕密度",
            value = densities.joinToString(", ")
        )
    }

    // 最后更新时间
    val updatedAt = raw?.updatedAt
    updatedAt?.let {
        val formattedDate = try {
            it.substring(0, 10)
        } catch (e: Exception) {
            it
        }
        InfoRow(
            label = "最后更新",
            value = formattedDate
        )
    }
}

@Composable
fun WysAppMarketInfo(appDetail: UnifiedAppDetail) {
    // 直接使用 UnifiedAppDetail 中转换好的字段
    InfoRow(
        label = "包名",
        value = appDetail.packageName
    )
    InfoRow(
        label = "版本类型",
        value = appDetail.versionTypeDisplay
    )
    InfoRow(
        label = "最低版本",
        value = appDetail.minsdkDisplay
    )
    InfoRow(
        label = "目标版本",
        value = appDetail.targetsdkDisplay
    )
    InfoRow(
        label = "CPU架构",
        value = appDetail.cpuArchDisplay
    )
    InfoRow(
        label = "系统兼容",
        value = appDetail.osCompatibilityDisplay
    )
    InfoRow(
        label = "屏幕兼容",
        value = appDetail.displayCompatibilityDisplay
    )
    InfoRow(
        label = "浏览次数",
        value = "${appDetail.watchCount ?: 0}"
    )
    InfoRow(
        label = "下载次数",
        value = "${appDetail.downloadCount}"
    )
    InfoRow(
        label = "上传时间",
        value = if (appDetail.uploadTime > 0) formatTimestamp(appDetail.uploadTime) else "未知"
    )
    InfoRow(
        label = "上传留言",
        value = appDetail.upnote ?: "无"
    )
}

// --- 通用 UI 组件 ---

// 新增：信息行组件
@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrEmpty() && value != "未知" && value != "") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
    }
}

// 新增：获取设备兼容性信息
@Composable
fun getDeviceInfo(minSdk: Int?): String {
    // val context = LocalContext.current // 未使用，可移除
    val deviceSdk = android.os.Build.VERSION.SDK_INT

    return buildString {
        append("当前设备SDK:  $deviceSdk")
        if (minSdk != null && deviceSdk >= minSdk) {
            append(" • 兼容")
        } else {
            append(" • 不兼容")
        }
    }
}

// --- 注意 ---
// handleShare 函数不应该放在这里。它依赖于 ViewModel 状态 (appDetail, context, coroutineScope, snackbarHostState)
// 并且是 AppDetailScreen 的一部分逻辑。应该保留在 AppDetailScreen.kt 或其 ViewModel 中。