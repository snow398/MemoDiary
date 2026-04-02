package com.memodiary.ui.screen.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Create / edit screen for a memo.
 * [memoId] == null  → create new memo
 * [memoId] != null  → load and edit existing memo
 *
 * Uses [BasicTextField] with inline placeholders for a clean, iOS-style look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
    memoId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MemoEditViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(memoId) {
        if (memoId != null) viewModel.loadMemo(memoId)
    }

    val titleColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val placeholderAlpha = 0.35f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memoId == null) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveMemo(onSaved) },
                        enabled = !isSaving && title.isNotBlank()
                    ) {
                        Text(
                            text = "保存",
                            color = if (title.isNotBlank()) primaryColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Title field ──────────────────────────────────────────────
            BasicTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                textStyle = TextStyle(
                    color = titleColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "标题",
                            style = TextStyle(
                                color = titleColor.copy(alpha = placeholderAlpha),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    innerTextField()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // ── Content field ────────────────────────────────────────────
            BasicTextField(
                value = content,
                onValueChange = viewModel::onContentChange,
                textStyle = TextStyle(
                    color = titleColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .padding(vertical = 16.dp),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text(
                            text = "开始写下你的想法...",
                            style = TextStyle(
                                color = titleColor.copy(alpha = placeholderAlpha),
                                fontSize = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}