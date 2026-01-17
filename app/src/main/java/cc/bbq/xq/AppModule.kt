//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq

import cc.bbq.xq.data.db.AppDatabase
import cc.bbq.xq.data.db.DownloadTaskDao  
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.repository.SineShopRepository
import cc.bbq.xq.data.repository.XiaoQuRepository
import cc.bbq.xq.data.db.DownloadTaskRepository
import cc.bbq.xq.ui.auth.LoginViewModel
import cc.bbq.xq.ui.billing.BillingViewModel
import cc.bbq.xq.ui.community.CommunityViewModel
import cc.bbq.xq.ui.community.FollowingPostsViewModel
import cc.bbq.xq.ui.community.HotPostsViewModel
import cc.bbq.xq.ui.community.MyLikesViewModel
import cc.bbq.xq.ui.payment.PaymentViewModel
import cc.bbq.xq.ui.user.MyReviewsViewModel
import cc.bbq.xq.ui.log.LogViewModel
import cc.bbq.xq.ui.user.UserListViewModel
import cc.bbq.xq.ui.message.MessageViewModel
import cc.bbq.xq.ui.plaza.AppDetailComposeViewModel
import cc.bbq.xq.ui.community.PostCreateViewModel
import cc.bbq.xq.ui.plaza.AppReleaseViewModel
import cc.bbq.xq.ui.plaza.PlazaViewModel
import cc.bbq.xq.ui.player.PlayerViewModel
import cc.bbq.xq.ui.settings.signin.SignInSettingsViewModel
import cc.bbq.xq.ui.search.SearchViewModel
import cc.bbq.xq.ui.user.MyPostsViewModel
import cc.bbq.xq.ui.user.UserDetailViewModel
import cc.bbq.xq.ui.settings.storage.StoreManagerViewModel 
import cc.bbq.xq.data.StorageSettingsDataStore 
import cc.bbq.xq.data.SearchHistoryDataStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import cc.bbq.xq.ui.community.BrowseHistoryViewModel
import cc.bbq.xq.ui.community.PostDetailViewModel
import cc.bbq.xq.ui.rank.RankingListViewModel
import cc.bbq.xq.ui.settings.update.UpdateSettingsViewModel
import cc.bbq.xq.ui.download.DownloadViewModel
import cc.bbq.xq.ui.home.HomeViewModel
import cc.bbq.xq.ui.plaza.VersionListViewModel
import cc.bbq.xq.data.UserFilterDataStore
import cc.bbq.xq.ui.user.MyCommentsViewModel
import cc.bbq.xq.data.repository.LingMarketRepository //新增灵应用商店仓库

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
    viewModel { UserDetailViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }
    viewModel { PostDetailViewModel(androidApplication()) }
    viewModel { RankingListViewModel() }
    viewModel { UpdateSettingsViewModel() }
    viewModel { SignInSettingsViewModel() }
    viewModel { HomeViewModel() }
    viewModel { VersionListViewModel(androidApplication(), get<SineShopRepository>()) }
    viewModel { DownloadViewModel(androidApplication(), get<DownloadTaskRepository>()) }
    viewModel { MyCommentsViewModel(androidApplication(), get()) }
    viewModel { MyReviewsViewModel(androidApplication(), get()) }

    // Singletons
//    single { AuthManager }AuthManager是object天生单例这里不再用koin管理
    
    single { UserFilterDataStore(get()) }
    
    // 数据库相关 - 添加 DownloadTaskDao 定义
    single { BBQApplication.instance.database }
    single { get<AppDatabase>().logDao() }  // 如果需要的话
    single { get<AppDatabase>().browseHistoryDao() }  // 如果需要的话
    single { get<AppDatabase>().networkCacheDao() }  // 如果需要的话
    single { get<AppDatabase>().postDraftDao() }  // 如果需要的话
    single { get<AppDatabase>().downloadTaskDao() }  // 关键：添加 DownloadTaskDao 的定义
    
    single { SearchHistoryDataStore(androidApplication()) }
    single { StorageSettingsDataStore(androidApplication()) }

    // Repositories - 修改 DownloadTaskRepository 的定义
    single { XiaoQuRepository(KtorClient.ApiServiceImpl) }
    single { SineShopRepository() }
    single { DownloadTaskRepository(get()) }  // 这里会自动使用上面定义的 DownloadTaskDao
    
    single { LingMarketRepository() } // 新增灵应用商店
    
    single<Map<AppStore, IAppStoreRepository>> {
        mapOf(
            AppStore.XIAOQU_SPACE to get<XiaoQuRepository>(),
            AppStore.SIENE_SHOP to get<SineShopRepository>(),
            AppStore.SINE_OPEN_MARKET to get<SineOpenMarketRepository>(),
            AppStore.LING_MARKET to get<LingMarketRepository>() // 新增
        )
    }
}