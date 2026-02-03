package cc.bbq.xq

/**
 * API 基础路径枚举
 */
enum class ApiEndpoint(val path: String) {
    SEARCH("/market/search/"),
    APP_LIST("/market/app/list/"),
    APP_INFO("/market/app/info/")
}

/**
 * 应用列表类型（用于 /market/app/list/ 接口的 type 参数）
 * 已知类型：0 = 最新上架，2 = 最多点击
 */
enum class AppListType(val value: Int) {
    LATEST(0),      // 最新上架
    MOST_VIEWED(2), // 最多点击
    UNKNOWN(-1);    // 未知类型
    
    companion object {
        fun fromValue(value: Int): AppListType {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 搜索类型（用于 /market/search/ 接口的 type 参数）
 */
enum class SearchType(val value: Int) {
    KEYWORD(0),     // 关键词搜索
    CATEGORY(1),    // 分类搜索
    PACKAGE_NAME(3), // 新增：包名搜索（用于获取版本列表）
    UNKNOWN(-1);
    
    companion object {
        fun fromValue(value: Int): SearchType {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 应用版本类型（对应响应数据中的 type 字段）
 * 已知类型：0 = 官方版，1 = 破解版，3 = 修改版，4 = 提取版，5 = 汉化版
 */
enum class AppVersionType(val value: Int, val displayName: String) {
    OFFICIAL(0, "官方版"),
    CRACKED(1, "破解版"),
    MODIFIED(3, "修改版"),
    EXTRACTED(4, "提取版"),
    LOCALIZED(5, "汉化版"),
    UNKNOWN(-1, "未知版本");
    
    companion object {
        fun fromValue(value: Int): AppVersionType {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
        
        fun fromDisplayName(name: String): AppVersionType {
            return values().firstOrNull { it.displayName == name } ?: UNKNOWN
        }
    }
}

/**
 * CPU 架构兼容性（对应详情响应中的 cpu 字段）
 * 已知：0 = N/A(未知), 3 = ARM/ARM64, 7 = 全兼容
 */
enum class CpuArch(val value: Int, val displayName: String) {
    UNKNOWN(0, "未知"),
    ARM_ARM64(3, "ARM/ARM64"),
    UNIVERSAL(7, "全兼容"),
    OTHER(-1, "未知");
    
    companion object {
        fun fromValue(value: Int): CpuArch {
            return values().firstOrNull { it.value == value } ?: OTHER
        }
    }
}

/**
 * 系统兼容性（对应详情响应中的 sys 字段）
 * 已知：0 = Android + WearOS全兼容, 1 = 仅 Android, 2 = 仅 WearOS, 3 = Android + WearOS良好
 */
enum class OsCompatibility(val value: Int, val displayName: String) {
    ANDROID_WEAROS(0, "Android + WearOS全兼容"),
    ANDROID_ONLY(1, "仅 Android"),
    WEAROS_ONLY(2, "仅 WearOS"),
    ANDROID_WEAROS_GOOD(3, "Android + WearOS良好"),
    UNKNOWN(-1, "未知");
    
    companion object {
        fun fromValue(value: Int): OsCompatibility {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 屏幕兼容性（对应详情响应中的 display 字段）
 * 已知：0 = 全兼容, 1 = 仅方屏, 2 = 仅圆屏, 3 = 适配良好
 */
enum class DisplayCompatibility(val value: Int, val displayName: String) {
    UNIVERSAL(0, "全兼容"),
    SQUARE_ONLY(1, "仅方屏"),
    ROUND_ONLY(2, "仅圆屏"),
    WELL_ADAPTED(3, "适配良好"),
    UNKNOWN(-1, "未知");
    
    companion object {
        fun fromValue(value: Int): DisplayCompatibility {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * API 响应状态码（对应响应中的 code 字段）
 * 注意：这个 code 是业务状态码，不是 HTTP 状态码
 */
enum class ApiResponseCode(val code: Int, val message: String) {
    SUCCESS(200, "成功"),
    NOT_FOUND(404, "未检索到应用"),
    UNKNOWN(-1, "未知错误");
    
    companion object {
        fun fromCode(code: Int): ApiResponseCode {
            return values().firstOrNull { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * 应用分类/家族（对应详情响应中的 family 字段）
 * 所有已知分类：
 * "应用商店" "其它软件" "人工智能" "社交互动" "游戏娱乐" "视听娱乐"
 * "生活服务" "旅行交通" "金融购物" "工具效率" "学习教育" "图书阅读"
 * "摄影摄像" "系统优化" "个性主题" "进阶搞机"
 */
enum class AppFamily(val displayName: String) {
    // 以下是所有已知分类
    APP_STORE("应用商店"),
    OTHER_SOFTWARE("其它软件"),
    ARTIFICIAL_INTELLIGENCE("人工智能"),
    SOCIAL_INTERACTION("社交互动"),
    GAME_ENTERTAINMENT("游戏娱乐"),
    AUDIO_VISUAL_ENTERTAINMENT("视听娱乐"),
    LIFE_SERVICES("生活服务"),
    TRAVEL_TRANSPORTATION("旅行交通"),
    FINANCE_SHOPPING("金融购物"),
    TOOL_EFFICIENCY("工具效率"),
    STUDY_EDUCATION("学习教育"),
    BOOK_READING("图书阅读"),
    PHOTOGRAPHY_CAMERA("摄影摄像"),
    SYSTEM_OPTIMIZATION("系统优化"),
    PERSONAL_THEME("个性主题"),
    ADVANCED_TECHNOLOGY("进阶搞机"),
    
    // 未知分类放在最后
    UNKNOWN("未知分类");
    
    companion object {
        fun fromDisplayName(name: String): AppFamily {
            return values().firstOrNull { it.displayName == name } ?: UNKNOWN
        }
        
        /**
         * 获取所有有效的分类（排除未知分类）
         */
        fun getAllValidCategories(): List<AppFamily> {
            return values().filter { it != UNKNOWN }
        }
        
        /**
         * 获取所有分类的显示名称
         */
        fun getAllDisplayNames(): List<String> {
            return getAllValidCategories().map { it.displayName }
        }
    }
}

/**
 * Android SDK 版本（对应 minsdk 和 targetsdk 字段）
 * 为了用户友好，使用 "Android X.X (版本名称)" 的格式
 */
enum class AndroidSdkVersion(val apiLevel: Int, val displayName: String) {
    JELLY_BEAN_MR1(17, "Android 4.2 (Jelly Bean)"),
    JELLY_BEAN_MR2(18, "Android 4.3 (Jelly Bean)"),
    KITKAT(19, "Android 4.4 (KitKat)"),
    KITKAT_WATCH(20, "Android 4.4W (KitKat Wear)"),
    LOLLIPOP(21, "Android 5.0 (Lollipop)"),
    LOLLIPOP_MR1(22, "Android 5.1 (Lollipop)"),
    MARSHMALLOW(23, "Android 6.0 (Marshmallow)"),
    NOUGAT(24, "Android 7.0 (Nougat)"),
    NOUGAT_MR1(25, "Android 7.1 (Nougat)"),
    OREO(26, "Android 8.0 (Oreo)"),
    OREO_MR1(27, "Android 8.1 (Oreo)"),
    PIE(28, "Android 9.0 (Pie)"),
    ANDROID_10(29, "Android 10 (Q)"),
    ANDROID_11(30, "Android 11 (R)"),
    ANDROID_12(31, "Android 12 (Snow Cone)"),
    ANDROID_12_L(32, "Android 12L (Snow Cone)"),
    ANDROID_13(33, "Android 13 (Tiramisu)"),
    ANDROID_14(34, "Android 14 (Upside Down Cake)"),
    UNKNOWN(-1, "未知 Android 版本");
    
    companion object {
        fun fromApiLevel(apiLevel: Int): AndroidSdkVersion {
            return values().firstOrNull { it.apiLevel == apiLevel } ?: UNKNOWN
        }
    }
}