package com.memodiary.ui.screen.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TimelineViewModel : ViewModel() {

    private val repository = AppModule.repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Memos partitioned into ordered date buckets: "今天", "昨天", "更早".
     * Reacts automatically to search-query changes (300 ms debounce).
     */
    val groupedMemos: StateFlow<Map<String, List<Memo>>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllMemos()
            else repository.searchMemos(query)
        }
        .map { memos -> groupByDate(memos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteMemo(memoId: Long) {
        viewModelScope.launch {
            repository.deleteMemo(memoId)
        }
    }

    /** Groups memos into an insertion-ordered map so the UI renders Today → Yesterday → Earlier. */
    private fun groupByDate(memos: List<Memo>): Map<String, List<Memo>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val yesterdayStart = todayStart - 86_400_000L

        val result = linkedMapOf<String, List<Memo>>()
        val today = memos.filter { it.createdAt >= todayStart }
        val yesterday = memos.filter { it.createdAt in yesterdayStart until todayStart }
        val earlier = memos.filter { it.createdAt < yesterdayStart }

        if (today.isNotEmpty()) result["今天"] = today
        if (yesterday.isNotEmpty()) result["昨天"] = yesterday
        if (earlier.isNotEmpty()) result["更早"] = earlier
        return result
    }
}
