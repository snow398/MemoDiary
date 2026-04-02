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

    private fun groupByLocation(memos: List<Memo>): Map<String, Map<String, List<CityInfo>>> {
        // Group memos by (country, province, city), build CityInfo for each group
        val cityGroups = memos.groupBy { Triple(
            it.country ?: "未知国家",
            it.province ?: "未知省份",
            it.city ?: "未知城市"
        ) }

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
}
