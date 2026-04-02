package com.memodiary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memodiary.domain.model.Memo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Memo list card featuring:
 *  - 3 dp card shadow that deepens on press
 *  - Tap  → [onClick]
 *  - Long-press → delete confirmation dialog → [onDelete]
 *  - Fade-in + slide-up entrance animation (runs once per unique memo id)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Animate into view the first time this card is composed
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(memo.id) { visible = true }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("删除笔记") },
            text = {
                Text("确定要删除「${memo.title.ifBlank { "此笔记" }}」吗？\n此操作无法撤销。")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(memo.id)
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showDeleteDialog = true }
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = memo.title.ifBlank { "无标题" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Content preview (first 2 lines)
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Creation timestamp
                Text(
                    text = formatDateTime(memo.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINESE)
    return sdf.format(Date(epochMillis))
}