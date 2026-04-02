package com.memodiary.ui.screen.footprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

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

    private data class ParsedAddress(
        val country: String,
        val province: String,
        val city: String,
        val district: String
    )

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

        return result.mapValues { (_, provinces) ->
            provinces.mapValues { (_, cities) -> cities.toList() }
        }
    }

    private fun parseAddressFallback(address: String?): Triple<String, String, String> {
        val parsed = parseChineseAddress(address)
        // In grouped list, if city is missing, use district as the lowest visible granularity.
        val cityOrDistrict = when {
            parsed.city != "未分类" -> parsed.city
            parsed.district != "未分类" -> parsed.district
            else -> "未分类"
        }
        return Triple(parsed.country, parsed.province, cityOrDistrict)
    }

    private fun parseChineseAddress(address: String?): ParsedAddress {
        if (address.isNullOrBlank()) {
            return ParsedAddress("未分类", "未分类", "未分类", "未分类")
        }

        val text = normalizeAddressText(address)
        val country = detectCountry(text)

        val province = extractProvince(text)
        val afterProvince = if (province == "未分类") text else text.substringAfter(province, text)

        val city = extractCity(afterProvince).ifBlank {
            // Municipality often has province == city-level name
            if (province in MUNICIPALITIES) province else "未分类"
        }

        val district = extractDistrict(afterProvince.substringAfter(city, afterProvince)).ifBlank {
            extractDistrict(afterProvince)
        }

        val normalizedProvince = if (province == "未分类") inferProvinceByCity(city) else province

        return ParsedAddress(
            country = country,
            province = normalizedProvince,
            city = if (city.isBlank()) "未分类" else city,
            district = if (district.isBlank()) "未分类" else district
        )
    }

    private fun normalizeAddressText(raw: String): String {
        return raw.trim()
            .replace("，", "")
            .replace(",", "")
            .replace(";", "")
            .replace("；", "")
            .replace("·", "")
            .replace(" ", "")
            .lowercase(Locale.getDefault())
    }

    private fun detectCountry(text: String): String {
        return when {
            text.contains("中国") || text.contains("中华人民共和国") -> "中国"
            text.contains("日本") -> "日本"
            text.contains("美国") || text.contains("usa") || text.contains("unitedstates") -> "美国"
            text.contains("香港") -> "中国"
            text.contains("澳门") -> "中国"
            else -> "未分类"
        }
    }

    private fun extractProvince(text: String): String {
        SPECIAL_PROVINCES.forEach { p ->
            if (text.contains(p)) return p
        }

        val provinceMatch = Regex("([\\p{IsHan}]{2,9}省)").find(text)?.groupValues?.get(1)
        if (!provinceMatch.isNullOrBlank()) return provinceMatch

        val cityAsProvince = Regex("(北京市|上海市|天津市|重庆市)").find(text)?.groupValues?.get(1)
        return cityAsProvince ?: "未分类"
    }

    private fun extractCity(text: String): String {
        val cityLike = Regex("([\\p{IsHan}]{2,12}(自治州|地区|盟|市))").find(text)?.groupValues?.get(1)
        return cityLike ?: ""
    }

    private fun extractDistrict(text: String): String {
        val district = Regex("([\\p{IsHan}]{1,12}(区|县|旗|市))").find(text)?.groupValues?.get(1)
        return district ?: ""
    }

    private fun inferProvinceByCity(city: String): String {
        if (city.isBlank() || city == "未分类") return "未分类"
        return CITY_TO_PROVINCE[city] ?: "未分类"
    }

    companion object {
        private val MUNICIPALITIES = setOf("北京市", "上海市", "天津市", "重庆市")

        private val SPECIAL_PROVINCES = listOf(
            "北京市", "上海市", "天津市", "重庆市",
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "香港特别行政区", "澳门特别行政区", "台湾省"
        )

        private val CITY_TO_PROVINCE = mapOf(
            "深圳市" to "广东省",
            "广州市" to "广东省",
            "佛山市" to "广东省",
            "东莞市" to "广东省",
            "杭州市" to "浙江省",
            "宁波市" to "浙江省",
            "南京市" to "江苏省",
            "苏州市" to "江苏省",
            "成都市" to "四川省",
            "武汉市" to "湖北省",
            "西安市" to "陕西省",
            "青岛市" to "山东省",
            "厦门市" to "福建省",
            "长沙市" to "湖南省"
        )
    }
}
