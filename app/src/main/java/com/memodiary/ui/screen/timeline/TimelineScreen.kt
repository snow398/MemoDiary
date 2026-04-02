package com.memodiary.ui.screen.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memodiary.ui.components.MemoCard
import com.memodiary.ui.components.TimelineItem

/**
 * Main screen with:
 *  1. Search bar (filters in real time with 300 ms debounce)
 *  2. Memos grouped by date with sticky headers (今天 / 昨天 / 更早)
 *  3. Each card supports long-press → delete confirmation
 *  4. Cards have shadow elevation and fade-in entrance animation
 *  5. FAB to create a new memo
 *  6. Adapts to light / dark theme automatically
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onMemoClick: (Long) -> Unit,
    onAddMemo: () -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val groupedMemos by viewModel.groupedMemos.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "MemoDiary",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // ── Search bar ──────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("搜索笔记...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMemo,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (groupedMemos.isEmpty()) {
            // ── Empty state ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "未找到相关笔记" else "还没有笔记",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角 + 按钮创建第一条笔记",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            // ── Grouped memo list ────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 88.dp // keep content above the FAB
                )
            ) {
                groupedMemos.forEach { (group, memos) ->
                    // Sticky date-group header (今天 / 昨天 / 更早)
                    stickyHeader(key = "header_$group") {
                        TimelineItem(dateLabel = group)
                    }
                    // Memo cards — keyed so animations run once per unique id
                    items(memos, key = { it.id }) { memo ->
                        MemoCard(
                            memo = memo,
                            onClick = { onMemoClick(memo.id) },
                            onDelete = viewModel::deleteMemo
                        )
                    }
                }
            }
        }
    }
}
