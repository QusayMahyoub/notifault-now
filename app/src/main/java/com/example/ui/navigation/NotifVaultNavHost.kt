package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.ThemeMode
import com.example.service.NotificationAccessHelper
import com.example.ui.components.PermissionGuideDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.MessageDetailScreen
import com.example.ui.screens.MonitoredAppsScreen
import com.example.ui.screens.NotificationsLogScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.AppsFilter
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SortOrder

object NavRoutes {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val LOG = "log"
    const val APPS = "apps"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val DETAIL = "detail/{notificationId}"

    fun detailRoute(id: Long): String = "detail/$id"
}

private data class BottomNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    BottomNavDestination(
        route = NavRoutes.DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    BottomNavDestination(
        route = NavRoutes.LOG,
        labelRes = R.string.nav_log,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_log"
    ),
    BottomNavDestination(
        route = NavRoutes.APPS,
        labelRes = R.string.nav_apps,
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        testTag = "nav_apps"
    ),
    BottomNavDestination(
        route = NavRoutes.FAVORITES,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarOutline,
        testTag = "nav_favorites"
    ),
    BottomNavDestination(
        route = NavRoutes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun NotifVaultNavHost(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    skipSplash: Boolean = false
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showPermissionGuide by remember { mutableStateOf(false) }

    val isPermissionEnabled by viewModel.isNotificationAccessEnabled.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val monitoredPackages by viewModel.monitoredPackages.collectAsStateWithLifecycle()
    val excludedPackages by viewModel.excludedPackages.collectAsStateWithLifecycle()
    val recentNotifications by viewModel.recentNotifications.collectAsStateWithLifecycle()
    val logUiState by viewModel.logUiState.collectAsStateWithLifecycle()
    val favoriteNotifications by viewModel.favoriteNotifications.collectAsStateWithLifecycle()
    val appsUiState by viewModel.appsUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchAppFilter by viewModel.searchAppFilter.collectAsStateWithLifecycle()
    val searchFavoritesOnly by viewModel.searchFavoritesOnly.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val ignoreSensitiveOtp by viewModel.ignoreSensitiveOtp.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in TopLevelDestinations.map { it.route }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 640.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTopLevelNav && !useNavigationRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { outerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(outerPadding)
                ) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail(
                            windowInsets = WindowInsets.safeDrawing
                        ) {
                            TopLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = if (skipSplash) NavRoutes.DASHBOARD else NavRoutes.SPLASH
                        ) {
                            composable(NavRoutes.SPLASH) {
                                SplashScreen(
                                    onFinished = {
                                        navController.navigate(NavRoutes.DASHBOARD) {
                                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(NavRoutes.DASHBOARD) {
                                DashboardScreen(
                                    isPermissionEnabled = isPermissionEnabled,
                                    isDarkTheme = isDarkTheme,
                                    totalSavedCount = totalCount,
                                    monitoredAppsCount = monitoredPackages.size,
                                    favoritesCount = favoriteCount,
                                    recentNotifications = recentNotifications,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onRequestPermissionClick = { showPermissionGuide = true },
                                    onOpenLog = { navigateToTab(NavRoutes.LOG) },
                                    onOpenMonitoredApps = { navigateToTab(NavRoutes.APPS) },
                                    onOpenFavorites = { navigateToTab(NavRoutes.FAVORITES) },
                                    onOpenSearch = { navController.navigate(NavRoutes.SEARCH) },
                                    onOpenDetail = { id -> navController.navigate(NavRoutes.detailRoute(id)) },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item.id, item.isFavorite) }
                                )
                            }

                            composable(NavRoutes.LOG) {
                                NotificationsLogScreen(
                                    uiState = logUiState,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onSelectAppFilter = viewModel::setLogAppFilter,
                                    onToggleSortOrder = {
                                        val next = if (logUiState.sortOrder == SortOrder.NEWEST_FIRST) {
                                            SortOrder.OLDEST_FIRST
                                        } else {
                                            SortOrder.NEWEST_FIRST
                                        }
                                        viewModel.setSortOrder(next)
                                    },
                                    onOpenSearch = { navController.navigate(NavRoutes.SEARCH) },
                                    onOpenDetail = { id -> navController.navigate(NavRoutes.detailRoute(id)) },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item.id, item.isFavorite) },
                                    onDeleteNotification = viewModel::deleteNotification
                                )
                            }

                            composable(NavRoutes.APPS) {
                                MonitoredAppsScreen(
                                    uiState = appsUiState,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onSearchQueryChange = viewModel::updateAppsSearchQuery,
                                    onFilterChange = viewModel::setAppsFilter,
                                    onToggleMonitored = viewModel::toggleMonitoredApp,
                                    onToggleExcluded = viewModel::toggleExcludedApp
                                )
                            }

                            composable(NavRoutes.FAVORITES) {
                                FavoritesScreen(
                                    favorites = favoriteNotifications,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onOpenDetail = { id -> navController.navigate(NavRoutes.detailRoute(id)) },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item.id, item.isFavorite) },
                                    onDeleteNotification = viewModel::deleteNotification
                                )
                            }

                            composable(NavRoutes.SETTINGS) {
                                SettingsScreen(
                                    isPermissionEnabled = isPermissionEnabled,
                                    ignoreSensitiveOtp = ignoreSensitiveOtp,
                                    themeMode = themeMode,
                                    totalSavedCount = totalCount,
                                    excludedAppsCount = excludedPackages.size,
                                    onRequestPermissionClick = { showPermissionGuide = true },
                                    onOpenExcludedApps = {
                                        viewModel.setAppsFilter(AppsFilter.EXCLUDED)
                                        navigateToTab(NavRoutes.APPS)
                                    },
                                    onToggleIgnoreOtp = viewModel::setIgnoreSensitiveOtp,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onDeleteAllData = viewModel::deleteAllNotifications
                                )
                            }

                            composable(NavRoutes.SEARCH) {
                                SearchScreen(
                                    query = searchQuery,
                                    selectedAppPackage = searchAppFilter,
                                    favoritesOnly = searchFavoritesOnly,
                                    appFilters = logUiState.appFilters,
                                    results = searchResults,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onSelectAppFilter = viewModel::setSearchAppFilter,
                                    onToggleFavoritesOnly = viewModel::setSearchFavoritesOnly,
                                    onBack = { navController.popBackStack() },
                                    onOpenDetail = { id -> navController.navigate(NavRoutes.detailRoute(id)) },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item.id, item.isFavorite) }
                                )
                            }

                            composable(
                                route = NavRoutes.DETAIL,
                                arguments = listOf(navArgument("notificationId") { type = NavType.LongType })
                            ) { entry ->
                                val id = entry.arguments?.getLong("notificationId") ?: -1L
                                val notification by viewModel.getNotificationById(id)
                                    .collectAsStateWithLifecycle(initialValue = null)
                                MessageDetailScreen(
                                    notification = notification,
                                    resolveAppIcon = viewModel::resolveAppIcon,
                                    onBack = { navController.popBackStack() },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item.id, item.isFavorite) },
                                    onDeleteConfirmed = viewModel::deleteNotification
                                )
                            }
                        }
                    }
                }
            }

            if (showPermissionGuide) {
                PermissionGuideDialog(
                    onConfirmGoToSettings = {
                        showPermissionGuide = false
                        NotificationAccessHelper.openNotificationListenerSettings(context)
                    },
                    onDismiss = { showPermissionGuide = false }
                )
            }
        }
    }
}
