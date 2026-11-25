package com.example.ui.screens

import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NotificationCard
import com.example.ui.viewmodel.LogUiState
import com.example.ui.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsLogScreen(
    uiState: LogUiState,
    resolveAppIcon: (String) -> Drawable?,
    onSelectAppFilter: (String?) -> Unit,
    onToggleSortOrder: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onToggleFavorite: (NotificationEntity) -> Unit,
    onDeleteNotification: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<NotificationEntity?>(null) }

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
                        text = stringResource(R.string.log_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = onToggleSortOrder,
                        modifier = Modifier.testTag("log_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(
                                if (uiState.sortOrder == SortOrder.NEWEST_FIRST) {
                                    R.string.log_sort_newest
                                } else {
                                    R.string.log_sort_oldest
                                }
                            )
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("log_search_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.appFilters.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedPackage == null,
                            onClick = { onSelectAppFilter(null) },
                            label = { Text(stringResource(R.string.log_filter_all)) },
                            modifier = Modifier.testTag("log_filter_all")
                        )
                    }
                    items(uiState.appFilters, key = { it.packageName }) { app ->
                        FilterChip(
                            selected = uiState.selectedPackage == app.packageName,
                            onClick = {
                                val next = if (uiState.selectedPackage == app.packageName) null else app.packageName
                                onSelectAppFilter(next)
                            },
                            label = { Text("${app.appName} (${app.count})") }
                        )
                    }
                }
            }

            if (uiState.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.MarkChatUnread,
                    title = stringResource(R.string.log_empty_title),
                    subtitle = stringResource(R.string.log_empty_subtitle),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        NotificationCard(
                            notification = item,
                            appIcon = resolveAppIcon(item.packageName),
                            onClick = { onOpenDetail(item.id) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onDelete = { pendingDelete = item },
                            modifier = Modifier.widthIn(max = 720.dp)
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNotification(target.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
