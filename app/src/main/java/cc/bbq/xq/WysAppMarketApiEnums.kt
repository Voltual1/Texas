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
 * 已知类型：0 = 关键词搜索，1 = 分类搜索
 */
enum class SearchType(val value: Int) {
    KEYWORD(0),     // 关键词搜索
    CATEGORY(1),    // 分类搜索
    UNKNOWN(-1);    // 未知类型
    
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