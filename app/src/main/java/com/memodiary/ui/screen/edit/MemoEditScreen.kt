package com.memodiary.ui.screen.edit

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.memodiary.data.image.ImageStorage
import com.memodiary.di.AppModule
import com.memodiary.domain.model.MoodType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
    memoId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MemoEditViewModel = viewModel()
) {
    val context = LocalContext.current

    val title        by viewModel.title.collectAsState()
    val content      by viewModel.content.collectAsState()
    val isSaving     by viewModel.isSaving.collectAsState()
    val locationInfo by viewModel.locationInfo.collectAsState()
    val isLocating   by viewModel.isLocating.collectAsState()
    val manualAddress by viewModel.manualAddress.collectAsState()
    val mood         by viewModel.mood.collectAsState()
    val imagePaths   by viewModel.imagePaths.collectAsState()

    // ── Camera setup ─────────────────────────────────────────────────────
    var cameraFileRef by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraFileRef?.absolutePath?.let { viewModel.addCameraImage(context, it) }
        }
    }
    fun launchCamera() {
        val file = ImageStorage.createCameraFile(context)
        cameraFileRef = file
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        cameraLauncher.launch(uri)
    }

    // ── Gallery picker ────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { viewModel.addImageFromUri(context, it) } }

    // ── Camera permission ─────────────────────────────────────────────────
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    // ── Location permission ───────────────────────────────────────────────
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms -> if (perms.values.any { it }) viewModel.fetchLocation() }

    LaunchedEffect(memoId) {
        if (memoId != null) {
            viewModel.loadMemo(memoId)
        } else {
            if (AppModule.locationRepository.hasLocationPermission()) viewModel.fetchLocation()
            else locationPermLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    val titleColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val placeholderAlpha = 0.35f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memoId == null) "新建笔记" else "编辑笔记") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "关闭") } },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveMemo(onSaved) },
                        enabled = !isSaving && title.isNotBlank()
                    ) {
                        Text("保存", color = if (title.isNotBlank()) primaryColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // ── Title ─────────────────────────────────────────────────────
            BasicTextField(
                value = title, onValueChange = viewModel::onTitleChange,
                textStyle = TextStyle(color = titleColor, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(primaryColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("标题", style = TextStyle(
                        color = titleColor.copy(alpha = placeholderAlpha), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    inner()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // ── Content ───────────────────────────────────────────────────
            BasicTextField(
                value = content, onValueChange = viewModel::onContentChange,
                textStyle = TextStyle(color = titleColor, fontSize = 16.sp, lineHeight = 24.sp),
                cursorBrush = SolidColor(primaryColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).padding(vertical = 16.dp),
                decorationBox = { inner ->
                    if (content.isEmpty()) Text("开始写下你的想法...", style = TextStyle(
                        color = titleColor.copy(alpha = placeholderAlpha), fontSize = 16.sp))
                    inner()
                }
            )

            // ── Mood section ──────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("心情", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MoodType.HAPPY, MoodType.NEUTRAL, MoodType.SAD, MoodType.NONE).forEach { m ->
                    val selected = mood == m
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onMoodChange(m) },
                        label = {
                            Text(
                                text = if (m.emoji.isEmpty()) m.label else "${m.emoji} ${m.label}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // ── Image section ─────────────────────────────────────────────
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("图片", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnails of selected images
            if (imagePaths.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imagePaths.forEach { path ->
                        Box(modifier = Modifier.size(88.dp)) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(path) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, contentDescription = "删除",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("从相册选择", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("拍照", style = MaterialTheme.typography.labelMedium)
                }
            }

            // ── Location section ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("位置信息", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLocating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在获取位置...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (locationInfo != null) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayAddr = locationInfo?.let { info ->
                            info.address
                                ?: listOfNotNull(info.city, info.province).joinToString(", ").ifBlank { null }
                                ?: "%.5f, %.5f".format(info.latitude, info.longitude)
                        } ?: ""
                        Text(displayAddr,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (AppModule.locationRepository.hasLocationPermission()) viewModel.fetchLocation()
                        else locationPermLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }, modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重新定位", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { viewModel.clearLocation() }, modifier = Modifier.height(36.dp)) {
                        Text("清除位置", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                OutlinedTextField(
                    value = manualAddress, onValueChange = viewModel::onManualAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入地址（如：东京新宿区）") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.resolveManualAddress() },
                        enabled = manualAddress.isNotBlank(), modifier = Modifier.height(36.dp)) {
                        Text("解析地址", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = {
                        if (AppModule.locationRepository.hasLocationPermission()) viewModel.fetchLocation()
                        else locationPermLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }, modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("自动定位", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

}
