package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NotificationCard
import com.example.ui.components.PermissionStatusCard
import com.example.ui.components.VaultLogoBadge
import com.example.ui.theme.VaultStarGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isPermissionEnabled: Boolean,
    isDarkTheme: Boolean,
    totalSavedCount: Int,
    monitoredAppsCount: Int,
    favoritesCount: Int,
    recentNotifications: List<NotificationEntity>,
    resolveAppIcon: (String) -> Drawable?,
    onRequestPermissionClick: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenMonitoredApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onToggleFavorite: (NotificationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultLogoBadge(size = 40.dp)
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.dashboard_offline_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.nav_search)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PermissionStatusCard(
                        isEnabled = isPermissionEnabled,
                        isDarkTheme = isDarkTheme,
                        onActionClick = onRequestPermissionClick
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            label = stringResource(R.string.dashboard_saved_count_label),
                            value = totalSavedCount.toString(),
                            icon = Icons.Filled.NotificationsNone,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onOpenLog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_saved_count")
                        )
                        StatMetricCard(
                            label = stringResource(R.string.dashboard_monitored_count_label),
                            value = monitoredAppsCount.toString(),
                            icon = Icons.Filled.Apps,
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenMonitoredApps,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_monitored_count")
                        )
                        StatMetricCard(
                            label = stringResource(R.string.dashboard_favorites_count_label),
                            value = favoritesCount.toString(),
                            icon = Icons.Filled.Star,
                            tint = VaultStarGold,
                            onClick = onOpenFavorites,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_favorites_count")
                        )
                    }

                    Button(
                        onClick = onOpenLog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("dashboard_open_log_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dashboard_view_all_log),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_recent_messages),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (recentNotifications.isNotEmpty()) {
                            TextButton(onClick = onOpenLog) {
                                Text(text = stringResource(R.string.dashboard_view_all_log))
                            }
                        }
                    }
                }
            }

            if (recentNotifications.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.widthIn(max = 720.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        EmptyStateView(
                            icon = Icons.Outlined.Inbox,
                            title = stringResource(R.string.dashboard_recent_empty_title),
                            subtitle = stringResource(R.string.dashboard_recent_empty_subtitle),
                            actionButton = {
                                TextButton(onClick = onOpenMonitoredApps) {
                                    Text(text = stringResource(R.string.apps_title))
                                }
                            }
                        )
                    }
                }
            } else {
                items(
                    items = recentNotifications,
                    key = { it.id }
                ) { item ->
                    NotificationCard(
                        notification = item,
                        appIcon = resolveAppIcon(item.packageName),
                        onClick = { onOpenDetail(item.id) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        modifier = Modifier.widthIn(max = 720.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
