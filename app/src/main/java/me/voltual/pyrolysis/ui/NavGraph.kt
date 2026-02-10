//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.pyrolysis.ui

// Android & Lifecycle
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// Compose Core
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState

// Navigation
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Koin
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// Project UI - Screens & ViewModels
import me.voltual.pyrolysis.AuthManager
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.ui.auth.LoginScreen
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingScreen
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.community.compose.BaseComposeListScreen
import me.voltual.pyrolysis.ui.community.compose.PostDetailScreen
import me.voltual.pyrolysis.ui.home.AboutScreen
import me.voltual.pyrolysis.ui.home.HomeDestination
import me.voltual.pyrolysis.ui.log.LogScreen
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.message.MessageCenterScreen
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.payment.PaymentCenterScreen
import me.voltual.pyrolysis.ui.payment.PaymentType
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.player.PlayerScreen
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.plaza.*
import me.voltual.pyrolysis.ui.rank.RankingListScreen
import me.voltual.pyrolysis.ui.search.SearchScreen
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.theme.ThemeCustomizeScreen
import me.voltual.pyrolysis.ui.user.*
import me.voltual.pyrolysis.ui.user.compose.UserListScreen
import me.voltual.pyrolysis.ui.update.UpdateScreen
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerScreen
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsScreen
import me.voltual.pyrolysis.ui.compose.IDMTransferDialog

// Animations & Utils
import me.voltual.pyrolysis.ui.animation.materialSharedAxisXIn
import me.voltual.pyrolysis.ui.animation.materialSharedAxisXOut
import me.voltual.pyrolysis.ui.animation.rememberSlideDistance
import me.voltual.pyrolysis.util.getPackageInfoCompat
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState, 
    modifier: Modifier = Modifier
) {
    val slideDistance = rememberSlideDistance()

    // 将 ViewModel 的创建移动到 AppNavHost 函数中
    val searchViewModel: SearchViewModel = org.koin.androidx.compose.koinViewModel()
    val postCreateViewModel: PostCreateViewModel = org.koin.androidx.compose.koinViewModel()
    val plazaViewModel: PlazaViewModel = org.koin.androidx.compose.koinViewModel { parametersOf(false) }
    val appReleaseViewModel: AppReleaseViewModel = org.koin.androidx.compose.koinViewModel()
    val messageViewModel: MessageViewModel = org.koin.androidx.compose.koinViewModel()
    val billingViewModel: BillingViewModel = org.koin.androidx.compose.koinViewModel()
    val paymentViewModel: PaymentViewModel = org.koin.androidx.compose.koinViewModel()
    val playerViewModel: PlayerViewModel = org.koin.androidx.compose.koinViewModel()
    val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
    val myLikesViewModel: MyLikesViewModel = org.koin.androidx.compose.koinViewModel()
    val hotPostsViewModel: HotPostsViewModel = org.koin.androidx.compose.koinViewModel()
    val followingPostsViewModel: FollowingPostsViewModel = org.koin.androidx.compose.koinViewModel()
    val myPostsViewModel: MyPostsViewModel = org.koin.androidx.compose.koinViewModel()
    val appDetailViewModel: AppDetailComposeViewModel = org.koin.androidx.compose.koinViewModel()
    val userProfileViewModel: UserProfileViewModel = koinViewModel()
   

    NavHost(
        navController = navController,
        startDestination = Home.route,
        modifier = modifier,
        enterTransition = { materialSharedAxisXIn(forward = true, slideDistance = slideDistance) },
        exitTransition = { materialSharedAxisXOut(forward = true, slideDistance = slideDistance) },
        popEnterTransition = { materialSharedAxisXIn(forward = false, slideDistance = slideDistance) },
        popExitTransition = { materialSharedAxisXOut(forward = false, slideDistance = slideDistance) }
    ) {
        // --- 核心屏幕 ---
        composable(route = Home.route) {
    HomeDestination(
        navController = navController,
        snackbarHostState = snackbarHostState
    )
}

        composable(route = Login.route) {
            val loginViewModel: LoginViewModel = org.koin.androidx.compose.koinViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize() 
            )
        }

        composable(route = About.route) {
    AboutScreen(
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState
    )
}

composable(
    route = Search.route, 
    arguments = Search.arguments
) { backStackEntry ->
    // 因为 userId 改为 String 类型，需要转换
    val userIdString = backStackEntry.arguments?.getString("userId")
    val userId = userIdString?.toLongOrNull()
    val nickname = backStackEntry.arguments?.getString("nickname")?.let {
        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
    }
    
    // 初始化 SearchViewModel 的用户筛选
    LaunchedEffect(userId, nickname) {
        if (userId != null && nickname != null) {
            searchViewModel.initFromNavArgs(userId, nickname)
        }
    }
    
    SearchScreen(
        viewModel = searchViewModel,
        onPostClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
        onLogClick = { navController.navigate(LogViewer.route) },
        modifier = Modifier.fillMaxSize()
    )
}
        
composable(route = SignInSettings.route ) {
    SignInSettingsScreen(
        snackbarHostState = remember { SnackbarHostState() }
    )
}

        composable(route = ThemeCustomize.route) {
            ThemeCustomizeScreen(
                modifier = Modifier.fillMaxSize() 
            )
        }
        
        composable(route = StoreManager.route) {
             StoreManagerScreen()
        }

        // --- 社区与帖子 ---
        composable(route = PostDetail(0).route, arguments = PostDetail.arguments) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong(AppDestination.ARG_POST_ID) ?: 0L
            PostDetailScreen(
                postId = postId,
                navController = navController,
                onPostDeleted = { navController.popBackStack() },
                snackbarHostState = snackbarHostState,
                modifier = Modifier.fillMaxSize() 
            )
        }

composable(route = CreatePost.route) {
    PostCreateScreen(
        viewModel = postCreateViewModel,
        navController = navController,
        snackbarHostState = snackbarHostState, 
        onBackClick = { navController.popBackStack() },
        mode = "create",
        refundAppName = "",
        refundAppId = 0L,
        refundVersionId = 0L,
        refundPayMoney = 0,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = CreateRefundPost(0, 0, "", 0).route, arguments = CreateRefundPost.arguments) { backStackEntry ->
    val args = backStackEntry.arguments!!
    PostCreateScreen(
        viewModel = postCreateViewModel,
        navController = navController,
        onBackClick = { navController.popBackStack() },
        snackbarHostState = snackbarHostState,
        mode = "refund",
        refundAppName = URLDecoder.decode(args.getString(AppDestination.ARG_APP_NAME, ""), StandardCharsets.UTF_8.toString()),
        refundAppId = args.getLong(AppDestination.ARG_APP_ID),
        refundVersionId = args.getLong(AppDestination.ARG_VERSION_ID),
        refundPayMoney = args.getInt(AppDestination.ARG_PAY_MONEY),
        modifier = Modifier.fillMaxSize()
    )
}

        composable(route = BrowseHistory.route) {
    BrowseHistoryScreen(
        onPostClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState
    )
}

        composable(route = ImagePreview("").route, arguments = ImagePreview.arguments) { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString(AppDestination.ARG_IMAGE_URL)?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            ImagePreviewScreen(
                imageUrl = imageUrl,
                snackbarHostState = snackbarHostState,
                onClose = { navController.popBackStack() }
            )
        }
        

composable(
    route = Download.route,
    // 优化：给该路由设置 0 毫秒的转场动画，避免跳转时的“白屏闪烁”
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None }
) {
    val context = LocalContext.current
    var showInstallDialog by remember { mutableStateOf(false) }

    val idmPackages = listOf(
        "idm.internet.download.manager.plus",
        "idm.internet.download.manager",
        "idm.internet.download.manager.adm.lite"
    )

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        var targetIntent: Intent? = null

        for (pkg in idmPackages) {
            targetIntent = pm.getLaunchIntentForPackage(pkg)
            if (targetIntent != null) break
        }

        if (targetIntent != null) {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(targetIntent)
            } catch (e: Exception) {
                // 异常处理
            }
            // 重点：立刻回退，用户几乎感知不到进入了这个路由
            navController.popBackStack()
        } else {
            showInstallDialog = true
        }
    }

    if (showInstallDialog) {
        IDMTransferDialog(
            onDismiss = {
                showInstallDialog = false
                navController.popBackStack()
            }
        )
    }
}
composable(route = MyComments.route) {
    MyCommentsScreen(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = MyReviews.route) {
    MyReviewsScreen(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = UserDetail(0).route, arguments = UserDetail.arguments) { backStackEntry ->
    val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
    val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name
    val store = try {
        AppStore.valueOf(storeName)
    } catch (e: IllegalArgumentException) {
        AppStore.XIAOQU_SPACE // 默认值
    }
    
    // 使用 koinViewModel() 而不是 viewModel()
    val viewModel: UserDetailViewModel = koinViewModel()
    
    // 简化的LaunchedEffect - 只设置用户ID
    LaunchedEffect(userId, store) {
        if (userId != -1L) {
            viewModel.loadUserDetails(userId, store)
        }
    }
    
    UserDetailScreen(
        viewModel = viewModel,
        onPostsClick = { 
            // 传入用户的昵称作为默认值
            val userData = viewModel.userData.value
            val nickname = userData?.displayName ?: "用户"
            navController.navigate(MyPosts(userId, nickname).createRoute()) 
        },
        onResourcesClick = { uid, targetStore ->
            // 接收 store 参数
            navController.navigate(ResourcePlaza(
                isMyResource = false, 
                userId = uid, 
                mode = "public", 
                storeName = targetStore.name
            ).createRoute())
        },
        onImagePreview = { imageUrl ->
            navController.navigate(ImagePreview(imageUrl).createRoute())
        },
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState,
        navController = navController // 传递 navController
    )
}

// 更新 MyPosts 路由处理
composable(
    route = MyPosts(0).route, 
    arguments = MyPosts.arguments
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
    val nickname = backStackEntry.arguments?.getString("nickname")?.let {
        if (it.isNotEmpty()) URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) else null
    }
    
    MyPostsScreen(
        viewModel = myPostsViewModel,
        userId = userId,
        nickname = nickname,
        navController = navController,
        snackbarHostState = snackbarHostState
    )
}

        // 关注列表
composable(route = FollowList.route) {
    val viewModel: UserListViewModel = koinViewModel()
    
    // 设置列表类型并手动初始化
    LaunchedEffect(Unit) {
        viewModel.setListType(UserListType.FOLLOWERS)
        viewModel.loadInitialData()
    }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserListScreen(
        users = state.users,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onLoadMore = { viewModel.loadNextPage() },
        onUserClick = { userId -> navController.navigate(UserDetail(userId).createRoute()) },
        modifier = Modifier.fillMaxSize()
    )
}

// 粉丝列表  
composable(route = FanList.route) {
    val viewModel: UserListViewModel = koinViewModel()
    
    // 设置列表类型并手动初始化
    LaunchedEffect(Unit) {
        viewModel.setListType(UserListType.FANS)
        viewModel.loadInitialData()
    }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserListScreen(
        users = state.users,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onLoadMore = { viewModel.loadNextPage() },
        onUserClick = { userId -> navController.navigate(UserDetail(userId).createRoute()) },
        modifier = Modifier.fillMaxSize()
    )
}

composable(route = AccountProfile.route, arguments = AccountProfileArgs.arguments) { backStackEntry ->
    val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name
    val store = try {
        AppStore.valueOf(storeName)
    } catch (e: IllegalArgumentException) {
        AppStore.XIAOQU_SPACE
    }    
    
    AccountProfileScreen(
        modifier = Modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState,
        viewModel = userProfileViewModel,
        store = store
    )
}

        // --- 资源广场 ---
composable(route = ResourcePlaza(false).route, arguments = ResourcePlaza.arguments) { backStackEntry ->
            val isMyResource = backStackEntry.arguments?.getBoolean(AppDestination.ARG_IS_MY_RESOURCE) ?: false
            val userId = backStackEntry.arguments?.getLong(AppDestination.ARG_USER_ID) ?: -1L
            val mode = backStackEntry.arguments?.getString("mode") ?: "public"
            val storeName = backStackEntry.arguments?.getString("store") ?: AppStore.XIAOQU_SPACE.name

            ResourcePlazaScreen(
                isMyResourceMode = isMyResource,
                mode = mode,
                storeName = storeName,
                navigateToAppDetail = { appId, versionId, store ->
                    navController.navigate(AppDetail(appId, versionId, store).createRoute())
                },
                userId = if (userId != -1L) userId.toString() else null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 应用详情页 ---
        composable(route = AppDetail("", 0, "").route, arguments = AppDetail.arguments) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString(AppDestination.ARG_APP_ID) ?: ""
            val versionId = backStackEntry.arguments?.getLong(AppDestination.ARG_VERSION_ID) ?: 0L
            val storeName = backStackEntry.arguments?.getString("storeName") ?: "XIAOQU_SPACE"

            AppDetailScreen(
                appId = appId,
                versionId = versionId,
                storeName = storeName,
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(route = CreateAppRelease.route) {
            AppReleaseScreen(
                viewModel = appReleaseViewModel,
                navController = navController, 
                snackbarHostState = snackbarHostState,
                modifier = Modifier.fillMaxSize()
            )
        }

composable(route = UpdateAppRelease("").route, arguments = UpdateAppRelease.arguments) { backStackEntry ->
    val appDetailJson = backStackEntry.arguments?.getString(AppDestination.ARG_APP_DETAIL_JSON)?.let {
        URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
    }
    if (!appDetailJson.isNullOrBlank()) {
        val appDetailToUpdate = KtorClient.JsonConverter.fromJson(appDetailJson)
        if (appDetailToUpdate != null) {
            appReleaseViewModel.populateFromAppDetail(appDetailToUpdate)
        }
    }
    AppReleaseScreen(
        viewModel = appReleaseViewModel,
        navController = navController,
        snackbarHostState = snackbarHostState,
        modifier = Modifier.fillMaxSize()
    )
}

        composable(route = LogViewer.route) {
            val logViewModel: LogViewModel = org.koin.androidx.compose.koinViewModel()
            LogScreen(
                viewModel = logViewModel,
                snackbarHostState = snackbarHostState, 
                modifier = Modifier.fillMaxSize() 
            )
        }

        // --- 消息、账单、支付 ---
        composable(route = MessageCenter.route) {
            MessageCenterScreen(
                viewModel = messageViewModel,
                onMessageClick = { postId -> navController.navigate(PostDetail(postId).createRoute()) },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(route = Billing.route) {
            LaunchedEffect(Unit) {
                billingViewModel.loadBilling()
            }
            BillingScreen(
                viewModel = billingViewModel//,
            )
        }

        composable(route = PaymentCenterAdvanced.route) {
            paymentViewModel.setPaymentInfo(type = PaymentType.POST_REWARD, locked = false)
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize(),
                navController = navController 
            )
        }

        composable(route = PaymentForApp(0, "", 0, 0, "", "").route, arguments = PaymentForApp.arguments) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            paymentViewModel.setPaymentInfo(
                type = PaymentType.APP_PURCHASE,
                appId = args.getLong(AppDestination.ARG_APP_ID),
                appName = URLDecoder.decode(args.getString(AppDestination.ARG_APP_NAME, ""), StandardCharsets.UTF_8.toString()),
                versionId = args.getLong(AppDestination.ARG_VERSION_ID),
                price = args.getInt(AppDestination.ARG_PRICE),
                iconUrl = URLDecoder.decode(args.getString(AppDestination.ARG_ICON_URL, ""), StandardCharsets.UTF_8.toString()),
                previewContent = URLDecoder.decode(args.getString(AppDestination.ARG_PREVIEW_CONTENT, ""), StandardCharsets.UTF_8.toString()),
                locked = true
            )
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize(),
                navController = navController
            )
        }

        composable(route = PaymentForPost(0, "", "", "", "", "").route, arguments = PaymentForPost.arguments) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            paymentViewModel.setPaymentInfo(
                type = PaymentType.POST_REWARD,
                postId = args.getLong(AppDestination.ARG_POST_ID),
                postTitle = URLDecoder.decode(args.getString(AppDestination.ARG_POST_TITLE, ""), StandardCharsets.UTF_8.toString()),
                previewContent = URLDecoder.decode(args.getString(AppDestination.ARG_PREVIEW_CONTENT, ""), StandardCharsets.UTF_8.toString()),
                authorName = URLDecoder.decode(args.getString(AppDestination.ARG_AUTHOR_NAME, ""), StandardCharsets.UTF_8.toString()),
                authorAvatar = URLDecoder.decode(args.getString(AppDestination.ARG_AUTHOR_AVATAR, ""), StandardCharsets.UTF_8.toString()),
                postTime = URLDecoder.decode(args.getString(AppDestination.ARG_POST_TIME, ""), StandardCharsets.UTF_8.toString()),
                locked = false
            )
            PaymentCenterScreen(
                viewModel = paymentViewModel,
                modifier = Modifier.fillMaxSize(),
                navController = navController 
            )
        }
       

composable(
    route = UpdateSettings.route
) {
    UpdateSettingsScreen(
        snackbarHostState = snackbarHostState
    )
}

        // --- 列表屏幕 ---
composable(Community.route) { CommunityScreen(navController, communityViewModel, snackbarHostState) }
composable(MyLikes.route) { MyLikesScreen(navController, myLikesViewModel, snackbarHostState) }
composable(HotPosts.route) { HotPostsScreen(navController, hotPostsViewModel, snackbarHostState) }
composable(FollowingPosts.route) { FollowingPostsScreen(navController, followingPostsViewModel, snackbarHostState) }

        // --- 其他 ---
        composable(route = RankingList.route) {
            RankingListScreen(navController = navController)
        }

        composable(route = Player("").route, arguments = Player.arguments) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString(AppDestination.ARG_BVID) ?: ""
            if (bvid.isNotEmpty()) {
                LaunchedEffect(bvid) {
                    playerViewModel.loadVideoData(bvid)
                }
            }
            PlayerScreen(viewModel = playerViewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController, viewModel: CommunityViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "社区",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState, 
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 正确解析路由参数
                    val parts = route.removePrefix("my_posts/").split("/")
                    val userIdStr = parts.firstOrNull()
                    val nickname = if (parts.size > 1) parts[1] else "我" // 提供默认昵称
                    
                    if (userIdStr != null) {
                        val userId = userIdStr.toLongOrNull()
                        if (userId != null) {
                            navController.navigate(MyPosts(userId, nickname).createRoute())
                        }
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLikesScreen(navController: NavController, viewModel: MyLikesViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "我喜欢的",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState, 
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 正确解析路由参数
                    val parts = route.removePrefix("my_posts/").split("/")
                    val userIdStr = parts.firstOrNull()
                    val nickname = if (parts.size > 1) parts[1] else "我" // 提供默认昵称
                    
                    if (userIdStr != null) {
                        val userId = userIdStr.toLongOrNull()
                        if (userId != null) {
                            navController.navigate(MyPosts(userId, nickname).createRoute())
                        }
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotPostsScreen(navController: NavController, viewModel: HotPostsViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "热点",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        onCreateClick = { navController.navigate(CreatePost.route) },
        snackbarHostState = snackbarHostState,
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 正确解析路由参数
                    val parts = route.removePrefix("my_posts/").split("/")
                    val userIdStr = parts.firstOrNull()
                    val nickname = if (parts.size > 1) parts[1] else "我" // 提供默认昵称
                    
                    if (userIdStr != null) {
                        val userId = userIdStr.toLongOrNull()
                        if (userId != null) {
                            navController.navigate(MyPosts(userId, nickname).createRoute())
                        }
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingPostsScreen(navController: NavController, viewModel: FollowingPostsViewModel,snackbarHostState: SnackbarHostState) {
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "关注的人",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navController.navigate(PostDetail(post.postid).createRoute()) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navController.navigate(Search.route) },
        snackbarHostState = snackbarHostState, 
        onCreateClick = { navController.navigate(CreatePost.route) },
        historyClick = { navController.navigate(BrowseHistory.route) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navController.navigate(MessageCenter.route) },
        onNavigate = { route ->
            when {
                route == "community" -> navController.navigate(Community.route)
                route == "hot_posts" -> navController.navigate(HotPosts.route)
                route == "following_posts" -> navController.navigate(FollowingPosts.route)
                route == "my_likes" -> navController.navigate(MyLikes.route)
                route.startsWith("my_posts/") -> {
                    // 正确解析路由参数
                    val parts = route.removePrefix("my_posts/").split("/")
                    val userIdStr = parts.firstOrNull()
                    val nickname = if (parts.size > 1) parts[1] else "我" // 提供默认昵称
                    
                    if (userIdStr != null) {
                        val userId = userIdStr.toLongOrNull()
                        if (userId != null) {
                            navController.navigate(MyPosts(userId, nickname).createRoute())
                        }
                    }
                }
                else -> {
                    // 其他路由
                    navController.navigate(route)
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}