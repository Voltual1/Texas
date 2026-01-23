//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.with
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.bbq.xq.R
import cc.bbq.xq.data.UserAgreementDataStore
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import cc.bbq.xq.ui.animation.materialSharedAxisX
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
    onDismissRequest: () -> Unit,
    onAgreed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agreementDataStore = remember { UserAgreementDataStore(context) }

    var currentAgreementIndex by remember { mutableStateOf(0) }
    val agreementContents = remember { mutableStateMapOf<Int, String>() }

    val agreementTitles = listOf(
        "《OpenQu 用户协议》",
        "《小趣空间用户协议》", 
        "《弦-应用商店用户协议》",
        "《弦-应用商店隐私政策》"
    )

    val agreementResourceIds = listOf(
        R.raw.useragreement,
        R.raw.xiaoquuseragreement,
        R.raw.sineuseragreement,
        R.raw.sineprivacypolicy
    )

    LaunchedEffect(Unit) {
        agreementResourceIds.forEachIndexed { index, resId ->
            val content = withContext(Dispatchers.IO) {
                loadRawResourceText(context, resId)
            }
            agreementContents[index] = content
        }
    }

    var animationForward by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { /* 禁止点击外部取消 */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                // 限制最大高度为屏幕的 85%，防止小屏手机下按钮溢出屏幕
                .fillMaxHeight(0.85f)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge // 使用更现代的圆角
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 固定头部：主标题
                Text(
                    text = "服务协议与隐私政策",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // 弹性区域：包含协议标题和协议文本
                // 使用 weight(1f) 确保这部分占据剩余空间，当内容过多时内部滚动
                Box(
                    modifier = Modifier
                        .weight(1f) 
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentAgreementIndex,
                        transitionSpec = {
                            materialSharedAxisX(
                                forward = animationForward,
                                slideDistance = 30,
                                durationMillis = 400
                            )
                        },
                        label = "协议切换动画"
                    ) { targetIndex ->
                        val currentContent = agreementContents[targetIndex] ?: "正在加载协议内容..."
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = agreementTitles[targetIndex],
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 内部滚动区域
                            val scrollState = rememberScrollState()
                            // 每次切换协议时重置滚动位置
                            LaunchedEffect(targetIndex) {
                                scrollState.scrollTo(0)
                            }

                            MarkDownText(
                                content = currentContent,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 固定底部：按钮区域
                // 增加 Divider 分割感，提示上方可滑动
                Divider(modifier = Modifier.padding(bottom = 12.dp), alpha = 0.1f)

                Column(modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider(
        modifier = Modifier.padding(bottom = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 次要操作：使用 FilledTonalButton，视觉压力较小
        if (currentAgreementIndex > 0) {
            FilledTonalButton(
                onClick = {
                    animationForward = false
                    currentAgreementIndex--
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium // M3 默认通常是圆角矩形
            ) {
                Text(text = "上一个")
            }
        } else {
            // 如果没有“上一个”，可以放一个占位符或者让“同意”按钮独占
            // 这里选择让“同意”按钮在第一页也保持比例，或者你可以去掉这部分让同意按钮全宽
            Spacer(modifier = Modifier.weight(1f))
        }

        // 主要操作：使用 Filled Button，背景色为 Primary
        Button(
            onClick = {
                scope.launch {
                    saveAgreementState(agreementDataStore, currentAgreementIndex)
                    if (currentAgreementIndex < agreementTitles.size - 1) {
                        animationForward = true
                        currentAgreementIndex++
                    } else {
                        onAgreed()
                    }
                }
            },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (currentAgreementIndex < agreementTitles.size - 1) "同意并继续" else "确认合规",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
                    }
                }
            }
        }
    }
}

// 抽离存储逻辑提高可读性
private suspend fun saveAgreementState(ds: UserAgreementDataStore, index: Int) {
    when (index) {
        0 -> ds.setUserAgreementAccepted(true)
        1 -> ds.setXiaoquUserAgreementAccepted(true)
        2 -> ds.setSineUserAgreementAccepted(true)
        3 -> ds.setSinePrivacyPolicyAccepted(true)
    }
}

// 修复：移除非Composable注解，改为普通函数
private fun loadRawResourceText(context: android.content.Context, resId: Int): String {
    return try {
        context.resources.openRawResource(resId).use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    } catch (e: Exception) {
        "加载协议内容失败: ${e.message}"
    }
}