//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package com.texas.pyrolysis

import com.texas.pyrolysis.data.db.AppDatabase
import com.texas.pyrolysis.data.repository.IAppStoreRepository
import com.texas.pyrolysis.data.repository.SineShopRepository
import com.texas.pyrolysis.data.repository.WysAppCrawlerRepository
import com.texas.pyrolysis.data.repository.XiaoQuRepository
import com.texas.pyrolysis.data.repository.SineOpenMarketRepository // 添加这个导入
//import com.texas.pyrolysis.data.db.DownloadTaskRepository
import com.texas.pyrolysis.ui.auth.LoginViewModel
import com.texas.pyrolysis.ui.billing.BillingViewModel
import org.koin.android.ext.koin.androidContext
import com.texas.pyrolysis.data.repository.WysAppMarketRepository
import com.texas.pyrolysis.ui.community.CommunityViewModel
import com.texas.pyrolysis.ui.plaza.VersionListViewModel // 新增导入
import com.texas.pyrolysis.ui.community.FollowingPostsViewModel
import com.texas.pyrolysis.ui.community.HotPostsViewModel
import com.texas.pyrolysis.data.DeviceNameDataStore
import com.texas.pyrolysis.ui.user.UserProfileViewModel
import com.texas.pyrolysis.ui.community.MyLikesViewModel
import com.texas.pyrolysis.ui.payment.PaymentViewModel
import com.texas.pyrolysis.ui.user.MyReviewsViewModel
import com.texas.pyrolysis.ui.log.LogViewModel
import com.texas.pyrolysis.ui.user.UserListViewModel
import com.texas.pyrolysis.ui.message.MessageViewModel
import com.texas.pyrolysis.ui.plaza.AppDetailComposeViewModel
import com.texas.pyrolysis.ui.community.PostCreateViewModel
import com.texas.pyrolysis.ui.plaza.AppReleaseViewModel
import com.texas.pyrolysis.ui.plaza.PlazaViewModel
import com.texas.pyrolysis.ui.plaza.CrawlerViewModel
import com.texas.pyrolysis.ui.player.PlayerViewModel
import com.texas.pyrolysis.ui.settings.signin.SignInSettingsViewModel
import com.texas.pyrolysis.ui.search.SearchViewModel
import com.texas.pyrolysis.ui.user.MyPostsViewModel
import com.texas.pyrolysis.ui.user.UserDetailViewModel
import com.texas.pyrolysis.ui.settings.storage.StoreManagerViewModel 
import com.texas.pyrolysis.data.StorageSettingsDataStore 
import com.texas.pyrolysis.data.SearchHistoryDataStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.texas.pyrolysis.ui.community.BrowseHistoryViewModel
import com.texas.pyrolysis.ui.community.PostDetailViewModel
import com.texas.pyrolysis.ui.rank.RankingListViewModel
import com.texas.pyrolysis.ui.settings.update.UpdateSettingsViewModel
//import com.texas.pyrolysis.ui.download.DownloadViewModel
import com.texas.pyrolysis.ui.home.HomeViewModel
//import com.texas.pyrolysis.ui.plaza.VersionListViewModel
import com.texas.pyrolysis.data.UserFilterDataStore
import com.texas.pyrolysis.data.UserAgreementDataStore
import com.texas.pyrolysis.ui.user.MyCommentsViewModel
import com.texas.pyrolysis.data.repository.LingMarketRepository //新增灵应用商店仓库

val appModule = module {
    // ViewModel definitions
    viewModel { LoginViewModel(androidApplication()) }
    viewModel { BillingViewModel(androidApplication()) }
    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(androidApplication()) }
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(androidApplication()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication()) }
    
    // 修正：注入 repositories 参数
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) }
    
    viewModel { AppReleaseViewModel(androidApplication()) }
    
    viewModel { PlazaViewModel(androidApplication(), get()) }
    
    viewModel { PlayerViewModel(androidApplication()) }
    
    viewModel { SearchViewModel(get(), get()) }
    
    viewModel { UserListViewModel(androidApplication()) }
    viewModel { PostCreateViewModel(androidApplication()) }
    viewModel { MyPostsViewModel(get()) }
    viewModel { PaymentViewModel(androidApplication()) }
    viewModel { VersionListViewModel(androidApplication(), get()) } // 注册 ViewModel
    viewModel { UserDetailViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }
    viewModel { PostDetailViewModel(androidApplication()) }
    viewModel { RankingListViewModel() }
    viewModel { CrawlerViewModel(get()) }
    viewModel { UpdateSettingsViewModel() }
    viewModel { SignInSettingsViewModel() }
    viewModel { HomeViewModel() }
//    viewModel { VersionListViewModel(androidApplication(), get<SineShopRepository>()) }
//    viewModel { DownloadViewModel(androidApplication(), get<DownloadTaskRepository>()) }
    viewModel { MyCommentsViewModel(androidApplication(), get()) }
    viewModel { MyReviewsViewModel(androidApplication(), get()) }

    // Singletons
//    single { AuthManager }AuthManager是object天生单例这里不再用koin管理
    
    single { UserFilterDataStore(get()) }
    
single { UserAgreementDataStore(androidContext()) }
    
    // 数据库相关 - 添加 DownloadTaskDao 定义
    single { BBQApplication.instance.database }
    single { get<AppDatabase>().logDao() }  // 如果需要的话
    single { get<AppDatabase>().browseHistoryDao() }  // 如果需要的话
    single { get<AppDatabase>().networkCacheDao() }  // 如果需要的话
    single { get<AppDatabase>().postDraftDao() }  
    single { get<AppDatabase>().crawledAppDao() }
single { WysAppCrawlerRepository(get(), get(), androidContext()) }
//    single { get<AppDatabase>().downloadTaskDao() }  // 关键：添加 DownloadTaskDao 的定义
    
    single { SearchHistoryDataStore(androidApplication()) }
    single { StorageSettingsDataStore(androidApplication()) }
    viewModel { UserProfileViewModel(get(), get()) }
    
    single { DeviceNameDataStore(androidContext()) }

    // Repositories - 修改 DownloadTaskRepository 的定义
    single { XiaoQuRepository(KtorClient.ApiServiceImpl) }
    single { SineShopRepository() }
    single { SineOpenMarketRepository() } // 添加 SINE_OPEN_MARKET 的仓库
//    single { DownloadTaskRepository(get()) }  // 这里会自动使用上面定义的 DownloadTaskDao

// 这里的 get() 会自动匹配到上面的 DeviceNameDataStore
single { WysAppMarketRepository(get()) }
    
    single { LingMarketRepository() } // 新增灵应用商店
    
    // 修正：显式指定 Map 的类型参数
    single<Map<AppStore, IAppStoreRepository>> {
    val map = mutableMapOf<AppStore, IAppStoreRepository>()
    map[AppStore.XIAOQU_SPACE] = get<XiaoQuRepository>()
    map[AppStore.SIENE_SHOP] = get<SineShopRepository>()
    map[AppStore.SINE_OPEN_MARKET] = get<SineOpenMarketRepository>()
    map[AppStore.LING_MARKET] = get<LingMarketRepository>()
    map[AppStore.WYSAPPMARKET] = get<WysAppMarketRepository>() // 新增
    map
}
}