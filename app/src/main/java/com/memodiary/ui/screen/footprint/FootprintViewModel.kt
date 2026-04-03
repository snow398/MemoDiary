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

    /**
     * Returns memos that belong to the given (country, province, city) triplet.
     * Uses the same in-memory parsing logic as [groupByLocation], so manual-address
     * memos (where the DB city column is NULL) are correctly included.
     */
    fun getMemosForCityKey(country: String, province: String, city: String): Flow<List<Memo>> =
        repository.getMemosWithLocation().map { memos ->
            memos.filter { memo ->
                val fallback = parseAddressFallback(memo.address)
                val memoCountry  = memo.country?.ifBlank { null }  ?: fallback.first
                val memoProvince = memo.province?.ifBlank { null } ?: fallback.second
                val memoCity     = memo.city?.ifBlank { null }     ?: fallback.third
                memoCountry == country && memoProvince == province && memoCity == city
            }
        }

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
                it.country?.ifBlank { null }  ?: parsed.first,
                it.province?.ifBlank { null } ?: parsed.second,
                it.city?.ifBlank { null }     ?: parsed.third
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
        val parsed = parseAddress(address)
        val cityOrDistrict = when {
            parsed.city != "未分类" -> parsed.city
            parsed.district != "未分类" -> parsed.district
            else -> "未分类"
        }
        return Triple(parsed.country, parsed.province, cityOrDistrict)
    }

    /** Universal address parser: tries CJK patterns first, then generic international patterns. */
    private fun parseAddress(address: String?): ParsedAddress {
        if (address.isNullOrBlank()) {
            return ParsedAddress("未分类", "未分类", "未分类", "未分类")
        }

        val trimmed = address.trim()

        // ── Detect language type ─────────────────────────────────────────
        val hasChinese = trimmed.any { it.code in 0x4E00..0x9FFF }
        val hasJapanese = trimmed.any { it.code in 0x3040..0x30FF }

        return if (hasChinese) parseChineseAddress(trimmed)
        else if (hasJapanese) parseJapaneseAddress(trimmed)
        else parseLatinAddress(trimmed)
    }

    private fun parseChineseAddress(address: String): ParsedAddress {
        val text = normalizeAddressText(address)
        val country = detectChineseCountry(text)
        val province = extractProvince(text)
        val afterProvince = if (province == "未分类") text else text.substringAfter(province, text)
        val city = extractCity(afterProvince).ifBlank {
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

    /** Japanese address: e.g. "日本東京都新宿区西新宿" */
    private fun parseJapaneseAddress(text: String): ParsedAddress {
        val province = Regex("([\\p{IsHan}\\p{InHiragana}\\p{InKatakana}]+[都道府県])").find(text)?.groupValues?.get(1) ?: "未分类"
        val city = Regex("([\\p{IsHan}\\p{InHiragana}\\p{InKatakana}]+[市区町村郡])").find(text)?.groupValues?.get(1) ?: "未分类"
        return ParsedAddress(country = "日本", province = province, city = city, district = "未分类")
    }

    /** Latin-script address (English, French, Spanish, etc.)
     *  Attempt to extract Country, State/Province, City from comma-delimited tokens (last = country). */
    private fun parseLatinAddress(text: String): ParsedAddress {
        val parts = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> ParsedAddress(
                country  = parts.last().normalizeCountryName(),
                province = parts[parts.size - 2],
                city     = parts[parts.size - 3],
                district = "未分类"
            )
            parts.size == 2 -> ParsedAddress(
                country  = parts.last().normalizeCountryName(),
                province = "未分类",
                city     = parts.first(),
                district = "未分类"
            )
            parts.size == 1 -> ParsedAddress(
                country  = parts.first().normalizeCountryName().let {
                    if (it == parts.first()) "未分类" else it  // if unchanged, not a country
                },
                province = "未分类",
                city     = parts.first(),
                district = "未分类"
            )
            else -> ParsedAddress("未分类", "未分类", "未分类", "未分类")
        }
    }

    private fun String.normalizeCountryName(): String {
        val lower = this.lowercase(Locale.getDefault()).trim()
        return COUNTRY_NAME_MAP[lower] ?: this
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

    private fun detectChineseCountry(text: String): String {
        return when {
            text.contains("中国") || text.contains("中华人民共和国") -> "中国"
            text.contains("日本") -> "日本"
            text.contains("美国") || text.contains("usa") || text.contains("unitedstates") -> "美国"
            text.contains("韩国") || text.contains("korea") -> "韩国"
            text.contains("英国") || text.contains("unitedkingdom") -> "英国"
            text.contains("法国") || text.contains("france") -> "法国"
            text.contains("德国") || text.contains("germany") -> "德国"
            text.contains("澳大利亚") || text.contains("australia") -> "澳大利亚"
            text.contains("加拿大") || text.contains("canada") -> "加拿大"
            text.contains("泰国") || text.contains("thailand") -> "泰国"
            text.contains("新加坡") || text.contains("singapore") -> "新加坡"
            text.contains("香港") -> "中国"
            text.contains("澳门") -> "中国"
            text.contains("台湾") -> "中国"
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

        /** Maps lowercase English country name variants to display name. */
        private val COUNTRY_NAME_MAP = mapOf(
            "china" to "中国", "prc" to "中国", "people's republic of china" to "中国",
            "japan" to "日本",
            "usa" to "美国", "united states" to "美国", "united states of america" to "美国", "us" to "美国",
            "south korea" to "韩国", "korea" to "韩国",
            "uk" to "英国", "united kingdom" to "英国", "great britain" to "英国",
            "france" to "法国",
            "germany" to "德国",
            "australia" to "澳大利亚",
            "canada" to "加拿大",
            "thailand" to "泰国",
            "singapore" to "新加坡",
            "malaysia" to "马来西亚",
            "indonesia" to "印度尼西亚",
            "vietnam" to "越南",
            "india" to "印度",
            "russia" to "俄罗斯",
            "italy" to "意大利",
            "spain" to "西班牙",
            "portugal" to "葡萄牙",
            "netherlands" to "荷兰",
            "new zealand" to "新西兰",
            "brazil" to "巴西",
            "mexico" to "墨西哥",
            "hong kong" to "中国",
            "macau" to "中国", "macao" to "中国",
            "taiwan" to "中国台湾"
        )
    }
}
