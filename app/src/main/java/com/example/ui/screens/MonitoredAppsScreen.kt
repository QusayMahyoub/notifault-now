package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AppIconAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.AppEntryUi
import com.example.ui.viewmodel.AppsFilter
import com.example.ui.viewmodel.AppsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoredAppsScreen(
    uiState: AppsUiState,
    resolveAppIcon: (String) -> Drawable?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (AppsFilter) -> Unit,
    onToggleMonitored: (String, Boolean) -> Unit,
    onToggleExcluded: (String, Boolean) -> Unit,
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
                    Text(
                        text = stringResource(R.string.apps_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.apps_opt_in_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input"),
                    placeholder = { Text(stringResource(R.string.apps_search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "مسح البحث"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.filter == AppsFilter.ALL,
                            onClick = { onFilterChange(AppsFilter.ALL) },
                            label = { Text(stringResource(R.string.apps_filter_all)) },
                            modifier = Modifier.testTag("apps_filter_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filter == AppsFilter.MONITORED,
                            onClick = { onFilterChange(AppsFilter.MONITORED) },
                            label = { Text(stringResource(R.string.apps_filter_monitored)) },
                            modifier = Modifier.testTag("apps_filter_monitored")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filter == AppsFilter.EXCLUDED,
                            onClick = { onFilterChange(AppsFilter.EXCLUDED) },
                            label = { Text(stringResource(R.string.apps_filter_excluded)) },
                            modifier = Modifier.testTag("apps_filter_excluded")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.entries.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Apps,
                    title = "لا توجد تطبيقات مطابقة",
                    subtitle = "جرّب تغيير عبارة البحث أو التصفية المختارة.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                val featured = uiState.entries.filter { it.info.isFeaturedMessaging }
                val others = uiState.entries.filterNot { it.info.isFeaturedMessaging }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (featured.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.apps_featured_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                        items(featured, key = { "feat_${it.info.packageName}" }) { entry ->
                            MonitoredAppRowCard(
                                entry = entry,
                                iconDrawable = resolveAppIcon(entry.info.packageName),
                                onToggleMonitored = { enabled ->
                                    onToggleMonitored(entry.info.packageName, enabled)
                                },
                                onToggleExcluded = { excluded ->
                                    onToggleExcluded(entry.info.packageName, excluded)
                                },
                                modifier = Modifier.widthIn(max = 720.dp)
                            )
                        }
                    }

                    if (others.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.apps_installed_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(others, key = { "other_${it.info.packageName}" }) { entry ->
                            MonitoredAppRowCard(
                                entry = entry,
                                iconDrawable = resolveAppIcon(entry.info.packageName),
                                onToggleMonitored = { enabled ->
                                    onToggleMonitored(entry.info.packageName, enabled)
                                },
                                onToggleExcluded = { excluded ->
                                    onToggleExcluded(entry.info.packageName, excluded)
                                },
                                modifier = Modifier.widthIn(max = 720.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoredAppRowCard(
    entry: AppEntryUi,
    iconDrawable: Drawable?,
    onToggleMonitored: (Boolean) -> Unit,
    onToggleExcluded: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_card_${entry.info.packageName}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (entry.isMonitored) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconAvatar(
                appName = entry.info.appName,
                iconDrawable = iconDrawable,
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.info.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.info.isFeaturedMessaging) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (entry.info.isInstalled) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = stringResource(
                                    if (entry.info.isInstalled) {
                                        R.string.apps_installed_badge
                                    } else {
                                        R.string.apps_not_installed_badge
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.info.isInstalled) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (entry.isExcluded) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = stringResource(R.string.apps_excluded_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.info.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = { onToggleExcluded(!entry.isExcluded) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("exclude_app_${entry.info.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = stringResource(R.string.apps_exclude_action),
                    tint = if (entry.isExcluded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Switch(
                checked = entry.isMonitored,
                onCheckedChange = onToggleMonitored,
                modifier = Modifier.testTag("monitor_switch_${entry.info.packageName}")
            )
        }
    }
}
