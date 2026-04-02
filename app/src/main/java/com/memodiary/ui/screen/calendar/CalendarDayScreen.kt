package com.memodiary.ui.screen.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import com.memodiary.domain.model.MoodType
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayScreen(
    dayStartMs: Long,
    onBack: () -> Unit,
    onMemoClick: (Long) -> Unit,
    onAddMemo: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val dayEnd = dayStartMs + 86_400_000L // +24h
    val localDate = Instant.ofEpochMilli(dayStartMs).atZone(zone).toLocalDate()
    val title = "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日"

    val memos by AppModule.repository
        .getMemosByDateRange(dayStartMs, dayEnd)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMemo,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (memos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("这天还没有笔记", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "点击右下角 + 新建一条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(memos, key = { it.id }) { memo ->
                    DayMemoCard(memo = memo, onClick = { onMemoClick(memo.id) })
                }
            }
        }
    }
}

@Composable
private fun DayMemoCard(memo: Memo, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(memo.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = tween(250)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = memo.title.ifBlank { "无标题" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = memo.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(memo.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (memo.mood != MoodType.NONE) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(memo.mood.emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
