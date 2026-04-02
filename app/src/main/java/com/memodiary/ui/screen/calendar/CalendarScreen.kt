package com.memodiary.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memodiary.domain.model.MoodType
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_HEADERS = listOf("一", "二", "三", "四", "五", "六", "日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDayClick: (dateStartMs: Long) -> Unit,
    onAddMemo: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val yearMonth by viewModel.currentYearMonth.collectAsState()
    val memosByDay by viewModel.memosByDay.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    // DatePickerState for year/month/day jump
    val today = LocalDate.now()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = yearMonth.atDay(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val ld = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.goToDate(ld.year, ld.monthValue)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历") },
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
                Icon(Icons.Default.CalendarToday, contentDescription = "新建笔记")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // ── Month navigation header ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上月")
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        text = "${yearMonth.year}年${yearMonth.monthValue}月",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "跳转",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = { viewModel.goToNextMonth() }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下月")
                }
            }

            // ── Week day headers ─────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_HEADERS.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // ── Calendar grid ────────────────────────────────────────────
            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value - 1 // Mon=0
            val daysInMonth = yearMonth.lengthOfMonth()
            val todayDate = today
            val totalCells = firstDayOfWeek + daysInMonth

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Empty placeholder cells before the 1st
                items(firstDayOfWeek) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                // Day cells
                items(daysInMonth) { idx ->
                    val day = idx + 1
                    val dayMemos = memosByDay[day]
                    val isToday = yearMonth.year == todayDate.year &&
                            yearMonth.monthValue == todayDate.monthValue &&
                            day == todayDate.dayOfMonth
                    val hasMemo = !dayMemos.isNullOrEmpty()
                    val mood = if (hasMemo) viewModel.dominantMood(dayMemos!!) else MoodType.NONE

                    CalendarDayCell(
                        day = day,
                        isToday = isToday,
                        hasMemo = hasMemo,
                        mood = mood,
                        onClick = {
                            onDayClick(viewModel.dayStartMillis(day))
                        }
                    )
                }
                // Fill remaining cells so rows are complete
                val remaining = (7 - totalCells % 7) % 7
                items(remaining) { Box(modifier = Modifier.aspectRatio(1f)) }
            }

            // ── Mood legend ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                listOf(MoodType.HAPPY, MoodType.NEUTRAL, MoodType.SAD).forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(m.label, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("有笔记", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    hasMemo: Boolean,
    mood: MoodType,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) onPrimary else MaterialTheme.colorScheme.onBackground
                )
            )
            // Mood emoji or dot indicator
            when {
                mood != MoodType.NONE -> Text(mood.emoji, fontSize = 11.sp, lineHeight = 12.sp)
                hasMemo -> Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isToday) onPrimary.copy(alpha = 0.7f) else primary.copy(alpha = 0.6f))
                )
                else -> Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}
