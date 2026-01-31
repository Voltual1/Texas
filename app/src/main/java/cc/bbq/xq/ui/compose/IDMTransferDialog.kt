package cc.bbq.xq.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun IDMTransferDialog(onDismiss: () -> Unit) {
    // 既然有 MarkDownText，我们可以直接在这里定义符合 MD 语法的文案
val noticeContent = """
        嘿孩子们，从浊燃18.4 版本开始，本项目的下载服务已经整体移交给 **1DM** 家族了。
        所以你需要安装1DM系列的应用哦
        **注意：** 是数字 **1**（一），不是字母 "I" 哦！
        或者自己核对一下应用包名是不是这几个咯

        **核对包名：**
        * `idm.internet.download.manager.plus`
        * `idm.internet.download.manager`
        * `idm.internet.download.manager.adm.lite`

        现在的逻辑很简单：浊燃只负责获取到下载url并通过 Intent 叫醒1DM，至于你设备上装的1DM是原版、Plus 版还是那种“懂的都懂”的修改版，我管不着，也不做了校验。

        **关于获取途径：**
        为了避嫌，你V哥在此处不提供安装包。如果你觉得免费版1DM广告多，我推荐你上Plus版本也就是 **1DM+**。至于 1DM+ 要付费？这个嘛……
        你可以去咨询 [Bing 大小姐](https://www.bing.com/search?q=1DM%2B)
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "下载服务变更",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 使用项目内置的 MarkDownText 处理正文
                // 嵌套在 Column 中并开启滚动，确保长文不溢出
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false) // 灵活高度，最高不超出屏幕
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkDownText(
                        content = noticeContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("知道咯")
                }
            }
        }
    }
}