package com.memodiary.ui.screen.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import com.memodiary.domain.model.MoodType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel : ViewModel() {

    private val repository = AppModule.repository
    private val zone = ZoneId.systemDefault()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth

    /** Day-of-month (1..31) → list of memos for that day in the current month. */
    val memosByDay: StateFlow<Map<Int, List<Memo>>> = _currentYearMonth
        .flatMapLatest { ym ->
            val startMs = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endMs   = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            repository.getMemosByDateRange(startMs, endMs)
        }
        .map { memos ->
            memos.groupBy { memo ->
                Instant.ofEpochMilli(memo.createdAt)
                    .atZone(zone)
                    .dayOfMonth
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun goToPreviousMonth() { _currentYearMonth.update { it.minusMonths(1) } }
    fun goToNextMonth()     { _currentYearMonth.update { it.plusMonths(1) } }

    fun goToDate(year: Int, month: Int) {
        _currentYearMonth.value = YearMonth.of(year, month)
    }

    /** Returns the "dominant" mood for a list of memos on a single day. */
    fun dominantMood(memos: List<Memo>): MoodType {
        val moods = memos.map { it.mood }.filter { it != MoodType.NONE }
        return when {
            moods.isEmpty() -> MoodType.NONE
            moods.any { it == MoodType.HAPPY } -> MoodType.HAPPY
            moods.any { it == MoodType.SAD }   -> MoodType.SAD
            else -> MoodType.NEUTRAL
        }
    }

    /** Start-of-day epoch millis for a given day in the current month. */
    fun dayStartMillis(day: Int): Long =
        _currentYearMonth.value.atDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
}
