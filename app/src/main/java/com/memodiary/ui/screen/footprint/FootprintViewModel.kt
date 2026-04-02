package com.memodiary.ui.screen.footprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * City-level summary for display in the footprint list.
 */
data class CityInfo(
    val country: String,
    val province: String,
    val city: String,
    val noteCount: Int,
    val latestTime: Long   // most recent createdAt
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FootprintViewModel : ViewModel() {

    private val repository = AppModule.repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Three-level structure: Country → Province → list of CityInfo.
     * Sorted by most recently visited country/province/city.
     */
    val groupedFootprints: StateFlow<Map<String, Map<String, List<CityInfo>>>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getMemosWithLocation()
            else repository.searchMemosWithLocation(query)
        }
        .map { memos -> groupByLocation(memos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadGroupedNotesByLocation(): StateFlow<Map<String, Map<String, List<CityInfo>>>> =
        groupedFootprints

    fun getNotesByCity(city: String): Flow<List<Memo>> =
        repository.getMemosByCity(city)

    private fun groupByLocation(memos: List<Memo>): Map<String, Map<String, List<CityInfo>>> {
        // Group by structured location first, then fall back to parsing free-form address text.
        val cityGroups = memos.groupBy {
            val parsed = parseAddressFallback(it.address)
            Triple(
                it.country ?: parsed.first,
                it.province ?: parsed.second,
                it.city ?: parsed.third
            )
        }

        val cityInfos = cityGroups.map { (key, notes) ->
            CityInfo(
                country = key.first,
                province = key.second,
                city = key.third,
                noteCount = notes.size,
                latestTime = notes.maxOf { it.createdAt }
            )
        }.sortedByDescending { it.latestTime }

        // Build Country → Province → CityInfos, preserving order by latest time
        val result = linkedMapOf<String, MutableMap<String, MutableList<CityInfo>>>()
        for (ci in cityInfos) {
            val provinces = result.getOrPut(ci.country) { linkedMapOf() }
            val cities = provinces.getOrPut(ci.province) { mutableListOf() }
            cities.add(ci)
        }

        @Suppress("UNCHECKED_CAST")
        return result as Map<String, Map<String, List<CityInfo>>>
    }

    private fun parseAddressFallback(address: String?): Triple<String, String, String> {
        if (address.isNullOrBlank()) return Triple("未分类", "未分类", "未分类")

        val text = address.replace(" ", "")

        fun extractBySuffix(vararg suffixes: String): String? {
            for (suffix in suffixes) {
                val idx = text.indexOf(suffix)
                if (idx > 0) {
                    val start = (idx - 8).coerceAtLeast(0)
                    return text.substring(start, idx + 1)
                }
            }
            return null
        }

        val country = when {
            text.contains("中国") -> "中国"
            text.contains("日本") -> "日本"
            text.contains("美国") -> "美国"
            else -> "未分类"
        }

        val province = extractBySuffix("省", "市", "自治区") ?: "未分类"
        val city = extractBySuffix("市", "区", "县") ?: "未分类"
        return Triple(country, province, city)
    }
}
