package com.memodiary.ui.screen.edit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memodiary.di.AppModule

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
    val locationInfo by viewModel.locationInfo.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val manualAddress by viewModel.manualAddress.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) viewModel.fetchLocation()
    }

    LaunchedEffect(memoId) {
        if (memoId != null) {
            viewModel.loadMemo(memoId)
        } else {
            // Auto-fetch location for new memos
            if (AppModule.locationRepository.hasLocationPermission()) {
                viewModel.fetchLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
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

            // ── Location section ─────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "位置信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLocating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在获取位置...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (locationInfo != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = locationInfo?.address ?: "${locationInfo?.city}, ${locationInfo?.province}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = {
                            if (AppModule.locationRepository.hasLocationPermission()) {
                                viewModel.fetchLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新定位", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearLocation() },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("清除位置", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                // No location — show manual input
                OutlinedTextField(
                    value = manualAddress,
                    onValueChange = viewModel::onManualAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入地址（如：东京新宿区）") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = { viewModel.resolveManualAddress() },
                        enabled = manualAddress.isNotBlank(),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("解析地址", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (AppModule.locationRepository.hasLocationPermission()) {
                                viewModel.fetchLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("自动定位", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}