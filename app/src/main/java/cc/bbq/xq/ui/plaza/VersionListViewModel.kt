class VersionListViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

    private val _versions = MutableStateFlow<List<UnifiedAppItem>>(emptyList())
    val versions: StateFlow<List<UnifiedAppItem>> = _versions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var currentPackageName: String = ""
    private var currentStore: AppStore = AppStore.XIAOQU_SPACE
    
    // 记录当前页码，方便后续扩展“加载更多”功能
    private var currentPage = 1

    fun loadVersions(packageName: String, store: AppStore, page: Int = 1) {
        // 避免重复加载
        if (currentPackageName == packageName && currentStore == store && 
            _versions.value.isNotEmpty() && page == currentPage) {
            return
        }

        currentPackageName = packageName
        currentStore = store
        currentPage = page
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val repository = repositories[store]
                    ?: throw IllegalArgumentException("Unsupported store: $store")

                // 核心逻辑：根据商店类型决定调用哪个重载
                val result: Result<List<UnifiedAppItem>> = if (store == AppStore.WYSAPPMARKET) {
                    // 微思：不带 page 的重载
                    repository.getAppVersionsByPackageName(packageName)
                } else {
                    // 弦应用商店及其他：使用带 page 的重载，并只取 List 部分
                    repository.getAppVersionsByPackageName(packageName, page).map { it.first }
                }

                if (result.isSuccess) {
                    val newItems = result.getOrThrow()
                    // 如果是第一页则覆盖，如果是后续页则累加（目前 UI 暂未实现 LoadMore，先覆盖）
                    _versions.value = newItems
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load versions"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}