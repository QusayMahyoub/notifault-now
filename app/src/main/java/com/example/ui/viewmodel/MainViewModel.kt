package com.example.ui.viewmodel

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.AppNotificationSummary
import com.example.data.local.NotificationEntity
import com.example.data.local.ThemeMode
import com.example.data.repository.InstalledAppsProvider
import com.example.data.repository.MonitoredAppInfo
import com.example.data.repository.NotificationRepository
import com.example.service.NotificationAccessHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

enum class AppsFilter {
    ALL,
    MONITORED,
    EXCLUDED
}

data class LogUiState(
    val items: List<NotificationEntity> = emptyList(),
    val appFilters: List<AppNotificationSummary> = emptyList(),
    val selectedPackage: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)

data class AppEntryUi(
    val info: MonitoredAppInfo,
    val isMonitored: Boolean,
    val isExcluded: Boolean
)

data class AppsUiState(
    val entries: List<AppEntryUi> = emptyList(),
    val searchQuery: String = "",
    val filter: AppsFilter = AppsFilter.ALL,
    val isLoading: Boolean = true
)

class MainViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val repository: NotificationRepository = container.notificationRepository
    private val preferences = container.preferencesManager
    private val appsProvider: InstalledAppsProvider = container.installedAppsProvider

    private val _isNotificationAccessEnabled = MutableStateFlow(
        NotificationAccessHelper.isNotificationAccessEnabled(application)
    )
    val isNotificationAccessEnabled: StateFlow<Boolean> = _isNotificationAccessEnabled.asStateFlow()

    val totalCount: StateFlow<Int> = repository.totalCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = 0
    )

    val favoriteCount: StateFlow<Int> = repository.favoriteCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = 0
    )

    val monitoredPackages: StateFlow<Set<String>> = preferences.monitoredPackages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptySet()
    )

    val excludedPackages: StateFlow<Set<String>> = preferences.excludedPackages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptySet()
    )

    val ignoreSensitiveOtp: StateFlow<Boolean> = preferences.ignoreSensitiveOtp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = true
    )

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ThemeMode.SYSTEM
    )

    val recentNotifications: StateFlow<List<NotificationEntity>> =
        repository.getRecentNotifications(limit = 5).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val favoriteNotifications: StateFlow<List<NotificationEntity>> =
        repository.favorites.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _selectedLogPackage = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val orderedNotifications = _sortOrder.flatMapLatest { order ->
        when (order) {
            SortOrder.NEWEST_FIRST -> repository.allNewestFirst
            SortOrder.OLDEST_FIRST -> repository.allOldestFirst
        }
    }

    val logUiState: StateFlow<LogUiState> = combine(
        orderedNotifications,
        repository.distinctApps,
        _selectedLogPackage,
        _sortOrder
    ) { items, appFilters, selectedPkg, sort ->
        val filtered = if (selectedPkg == null) {
            items
        } else {
            items.filter { it.packageName == selectedPkg }
        }
        LogUiState(
            items = filtered,
            appFilters = appFilters,
            selectedPackage = selectedPkg,
            sortOrder = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LogUiState()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchAppFilter = MutableStateFlow<String?>(null)
    val searchAppFilter: StateFlow<String?> = _searchAppFilter.asStateFlow()

    private val _searchFavoritesOnly = MutableStateFlow(false)
    val searchFavoritesOnly: StateFlow<Boolean> = _searchFavoritesOnly.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<NotificationEntity>> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchNotifications(query) },
        _searchAppFilter,
        _searchFavoritesOnly
    ) { results, appFilter, favOnly ->
        results.filter { item ->
            val matchesApp = appFilter == null || item.packageName == appFilter
            val matchesFav = !favOnly || item.isFavorite
            matchesApp && matchesFav
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    private val _discoveredApps = MutableStateFlow<List<MonitoredAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _appsSearchQuery = MutableStateFlow("")
    private val _appsFilter = MutableStateFlow(AppsFilter.ALL)

    val appsUiState: StateFlow<AppsUiState> = combine(
        _discoveredApps,
        monitoredPackages,
        excludedPackages,
        _appsSearchQuery,
        _appsFilter,
        _isLoadingApps
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val apps = values[0] as List<MonitoredAppInfo>
        @Suppress("UNCHECKED_CAST")
        val monitored = values[1] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val excluded = values[2] as Set<String>
        val query = values[3] as String
        val filter = values[4] as AppsFilter
        val loading = values[5] as Boolean

        val trimmed = query.trim().lowercase()
        val entries = apps.map { info ->
            AppEntryUi(
                info = info,
                isMonitored = info.packageName in monitored,
                isExcluded = info.packageName in excluded
            )
        }.filter { entry ->
            val matchesSearch = trimmed.isEmpty() ||
                entry.info.appName.lowercase().contains(trimmed) ||
                entry.info.packageName.lowercase().contains(trimmed)
            val matchesTab = when (filter) {
                AppsFilter.ALL -> true
                AppsFilter.MONITORED -> entry.isMonitored
                AppsFilter.EXCLUDED -> entry.isExcluded
            }
            matchesSearch && matchesTab
        }

        AppsUiState(
            entries = entries,
            searchQuery = query,
            filter = filter,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AppsUiState()
    )

    init {
        refreshInstalledApps()
    }

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        val enabled = NotificationAccessHelper.isNotificationAccessEnabled(context)
        _isNotificationAccessEnabled.value = enabled
        if (enabled) {
            NotificationAccessHelper.requestRebindIfEnabled(context)
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _discoveredApps.value = appsProvider.loadAvailableApps()
            _isLoadingApps.value = false
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setLogAppFilter(packageName: String?) {
        _selectedLogPackage.value = packageName
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchAppFilter(packageName: String?) {
        _searchAppFilter.value = packageName
    }

    fun setSearchFavoritesOnly(favoritesOnly: Boolean) {
        _searchFavoritesOnly.value = favoritesOnly
    }

    fun updateAppsSearchQuery(query: String) {
        _appsSearchQuery.value = query
    }

    fun setAppsFilter(filter: AppsFilter) {
        _appsFilter.value = filter
    }

    fun toggleMonitoredApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            preferences.setPackageMonitored(packageName, enabled)
        }
    }

    fun toggleExcludedApp(packageName: String, excluded: Boolean) {
        viewModelScope.launch {
            preferences.setPackageExcluded(packageName, excluded)
        }
    }

    fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, currentFavorite)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun setIgnoreSensitiveOtp(ignore: Boolean) {
        viewModelScope.launch {
            preferences.setIgnoreSensitiveOtp(ignore)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun getNotificationById(id: Long): Flow<NotificationEntity?> =
        repository.getNotificationById(id)

    fun resolveAppIcon(packageName: String): Drawable? =
        appsProvider.getAppIconOrNull(packageName)

    companion object {
        fun factory(application: Application, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application, container) as T
                }
            }
    }
}
