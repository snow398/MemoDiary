package com.memodiary.ui.screen.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.data.image.ImageStorage
import com.memodiary.data.location.LocationInfo
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import com.memodiary.domain.model.MoodType
import com.memodiary.domain.model.NoteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoEditViewModel : ViewModel() {

    private val repository = AppModule.repository
    private val locationRepository = AppModule.locationRepository

    private val _existingMemo = MutableStateFlow<Memo?>(null)

    /** Optional pre-set creation timestamp (from calendar date selection). */
    private var initialDateMs: Long? = null

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _locationInfo = MutableStateFlow<LocationInfo?>(null)
    val locationInfo: StateFlow<LocationInfo?> = _locationInfo

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating

    private val _manualAddress = MutableStateFlow("")
    val manualAddress: StateFlow<String> = _manualAddress

    private val _mood = MutableStateFlow(MoodType.NONE)
    val mood: StateFlow<MoodType> = _mood

    private val _noteColor = MutableStateFlow(NoteColor.NONE)
    val noteColor: StateFlow<NoteColor> = _noteColor

    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = repository.getMemoById(memoId)
            _existingMemo.value = memo
            _title.value = memo?.title ?: ""
            _content.value = memo?.content ?: ""
            _mood.value = memo?.mood ?: MoodType.NONE
            _noteColor.value = memo?.noteColor ?: NoteColor.NONE
            _imagePaths.value = memo?.imagePaths ?: emptyList()
            if (memo?.latitude != null && memo.longitude != null) {
                _locationInfo.value = LocationInfo(
                    latitude = memo.latitude,
                    longitude = memo.longitude,
                    country = memo.country,
                    province = memo.province,
                    city = memo.city,
                    address = memo.address
                )
            }
        }
    }

    /** Called when the user opens a new memo from the Calendar for a specific date. */
    fun setInitialDate(dateMs: Long) {
        initialDateMs = dateMs
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onContentChange(value: String) { _content.value = value }
    fun onManualAddressChange(value: String) { _manualAddress.value = value }
    fun onMoodChange(mood: MoodType) { _mood.value = mood }
    fun onNoteColorChange(color: NoteColor) { _noteColor.value = color }

    /** Copy a gallery/camera URI into app storage and append to image list. */
    fun addImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { ImageStorage.saveImageFromUri(context, uri) }
            if (path != null) _imagePaths.value = _imagePaths.value + path
        }
    }

    /** Called after TakePicture succeeds with the already-created cache file path. */
    fun addCameraImage(context: Context, cachePath: String) {
        viewModelScope.launch {
            val cacheFile = java.io.File(cachePath)
            val path = withContext(Dispatchers.IO) { ImageStorage.saveCameraFile(context, cacheFile) }
            if (path != null) _imagePaths.value = _imagePaths.value + path
        }
    }

    fun removeImage(path: String) {
        _imagePaths.value = _imagePaths.value.filter { it != path }
        ImageStorage.deleteImage(path)
    }

    fun fetchLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            val loc = locationRepository.getCurrentLocation()
            if (loc != null) _locationInfo.value = locationRepository.geocode(loc)
            _isLocating.value = false
        }
    }

    fun resolveManualAddress() {
        val addr = _manualAddress.value.trim()
        if (addr.isBlank()) return
        viewModelScope.launch {
            _isLocating.value = true
            val info = locationRepository.geocodeAddress(addr)
            if (info != null) {
                _locationInfo.value = info
            } else {
                // Geocoder unavailable or failed — parse locally so the address is still saved
                _locationInfo.value = parseAddressLocally(addr)
            }
            _isLocating.value = false
        }
    }

    fun clearLocation() { _locationInfo.value = null }

    fun saveMemo(onSaved: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val now = System.currentTimeMillis()
            val existing = _existingMemo.value
            val loc = _locationInfo.value
            val manual = _manualAddress.value.trim()

            // createdAt: existing memo keeps its original time; new memos use the calendar-selected
            // date if provided (preserving the time-of-day as current time within that day), else now.
            val createdAt = existing?.createdAt
                ?: initialDateMs?.let { dayStart ->
                    // dayStart is midnight of the selected day; add current time-of-day offset
                    val todayStart = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    dayStart + (now - todayStart).coerceAtLeast(0L)
                }
                ?: now

            // If geocoding succeeded use it; if user typed an address but never hit "解析" (or it
            // failed), fall back to local parsing so the address is never silently discarded.
            val resolvedLoc: com.memodiary.data.location.LocationInfo? = when {
                loc != null -> loc
                manual.isNotBlank() -> parseAddressLocally(manual)
                else -> null
            }

            val base = Memo(
                id = existing?.id ?: 0,
                title = _title.value.trim(),
                content = _content.value,
                createdAt = createdAt,
                updatedAt = now,
                // Only save lat/lon when there is a real GPS fix (non-zero coords)
                latitude  = resolvedLoc?.latitude?.takeIf { it != 0.0 },
                longitude = resolvedLoc?.longitude?.takeIf { it != 0.0 },
                country = resolvedLoc?.country,
                province = resolvedLoc?.province,
                city = resolvedLoc?.city,
                address = resolvedLoc?.address,
                mood = _mood.value,
                imagePaths = _imagePaths.value,
                noteColor = _noteColor.value
            )
            if (existing == null) repository.insertMemo(base)
            else repository.updateMemo(base)
            _isSaving.value = false
            onSaved()
        }
    }

    /**
     * Parse a free-form address string locally (no network / Geocoder needed).
     * Returns a LocationInfo with null lat/lon but populated country/province/city/address.
     *
     * Supports:
     *  - Chinese: 省/市/区 suffix detection
     *  - Japanese: 都道府県 / 市区町村
     *  - Latin (comma-delimited): last token = country, second-last = state, third-last = city
     */
    private fun parseAddressLocally(text: String): com.memodiary.data.location.LocationInfo {
        val hasChinese = text.any { it.code in 0x4E00..0x9FFF }
        val hasJapanese = text.any { it.code in 0x3040..0x30FF }

        val (country, province, city) = when {
            hasChinese -> parseChinese(text)
            hasJapanese -> parseJapanese(text)
            else -> parseLatin(text)
        }
        return com.memodiary.data.location.LocationInfo(
            latitude  = 0.0,   // placeholder — no actual GPS fix; DB stores null when lat==0 && lon==0
            longitude = 0.0,
            country   = country.ifBlank { null },
            province  = province.ifBlank { null },
            city      = city.ifBlank { null },
            address   = text
        )
    }

    private fun parseChinese(text: String): Triple<String, String, String> {
        val t = text.replace(Regex("[，,；; ·]"), "")
        val country = when {
            t.contains("中国") || t.contains("中华人民共和国") -> "中国"
            t.contains("日本") -> "日本"
            t.contains("美国") -> "美国"
            t.contains("韩国") -> "韩国"
            t.contains("英国") -> "英国"
            t.contains("香港") || t.contains("澳门") || t.contains("台湾") -> "中国"
            else -> "中国"   // assume China for Chinese-script addresses
        }
        val province = Regex("([\\p{IsHan}]{2,9}省|北京市|上海市|天津市|重庆市|内蒙古自治区|广西壮族自治区|西藏自治区|宁夏回族自治区|新疆维吾尔自治区|香港特别行政区|澳门特别行政区)")
            .find(t)?.groupValues?.get(1) ?: ""
        val afterProv = if (province.isNotBlank()) t.substringAfter(province, t) else t
        val city = Regex("([\\p{IsHan}]{2,12}(?:自治州|地区|盟|市))")
            .find(afterProv)?.groupValues?.get(1) ?: ""
        return Triple(country, province, city)
    }

    private fun parseJapanese(text: String): Triple<String, String, String> {
        val province = Regex("([\\p{IsHan}\\p{InHiragana}\\p{InKatakana}]+[都道府県])").find(text)?.groupValues?.get(1) ?: ""
        val city = Regex("([\\p{IsHan}\\p{InHiragana}\\p{InKatakana}]+[市区町村郡])").find(text)?.groupValues?.get(1) ?: ""
        return Triple("日本", province, city)
    }

    private fun parseLatin(text: String): Triple<String, String, String> {
        val parts = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> Triple(parts.last(), parts[parts.size - 2], parts[parts.size - 3])
            parts.size == 2 -> Triple(parts.last(), "", parts.first())
            else -> Triple("", "", text)
        }
    }
}

