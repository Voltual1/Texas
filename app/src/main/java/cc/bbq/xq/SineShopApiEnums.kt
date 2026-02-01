package cc.bbq.xq

import java.util.ArrayList

/**
 * app_abi字段的位掩码解析逻辑
 */
enum class AbiFlag(val mask: Int, val label: String) {
    X86(1, "x86"),
    ARM32(2, "ARM32"),
    ARM64(4, "ARM64");

    companion object {
        fun parseAbi(abiValue: Int): String {
            // 特殊情况判断
            if (abiValue <= 0) return "无适配"
            if ((abiValue and 7) == 7) return "全兼容"

            val supportedAbis = ArrayList<String>(3)
            
            if ((abiValue and 4) != 0) {
                supportedAbis.add(ARM64.label)
            }
            if ((abiValue and 2) != 0) {
                supportedAbis.add(ARM32.label)
            }
            if ((abiValue and 1) != 0) {
                supportedAbis.add(X86.label)
            }

            // 使用 "/" 拼接字符串 
            return if (supportedAbis.isEmpty()) {
                "无适配"
            } else {
                supportedAbis.joinToString("/")
            }
        }
    }
}