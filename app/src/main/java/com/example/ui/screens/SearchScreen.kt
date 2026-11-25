package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppNotificationSummary
import com.example.data.local.NotificationEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NotificationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    selectedAppPackage: String?,
    favoritesOnly: Boolean,
    appFilters: List<AppNotificationSummary>,
    results: List<NotificationEntity>,
    resolveAppIcon: (String) -> Drawable?,
    onQueryChange: (String) -> Unit,
    onSelectAppFilter: (String?) -> Unit,
    onToggleFavoritesOnly: (Boolean) -> Unit,
    onBack: () -> Unit,
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
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
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
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_search_input"),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
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
                            selected = favoritesOnly,
                            onClick = { onToggleFavoritesOnly(!favoritesOnly) },
                            label = { Text(stringResource(R.string.nav_favorites)) },
                            modifier = Modifier.testTag("search_favorites_chip")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAppPackage == null,
                            onClick = { onSelectAppFilter(null) },
                            label = { Text(stringResource(R.string.log_filter_all)) }
                        )
                    }
                    items(appFilters, key = { it.packageName }) { app ->
                        FilterChip(
                            selected = selectedAppPackage == app.packageName,
                            onClick = {
                                val next = if (selectedAppPackage == app.packageName) null else app.packageName
                                onSelectAppFilter(next)
                            },
                            label = { Text(app.appName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (results.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.SearchOff,
                    title = if (query.isBlank()) {
                        stringResource(R.string.search_empty_prompt)
                    } else {
                        stringResource(R.string.search_no_results)
                    },
                    subtitle = "يتم البحث محليًا داخل قاعدة بيانات الجهاز فقط.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "عدد النتائج: ${results.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                        )
                    }
                    items(results, key = { it.id }) { item ->
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
}
