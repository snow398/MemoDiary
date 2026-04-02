package com.memodiary.ui.screen.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.data.location.LocationInfo
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MemoEditViewModel : ViewModel() {

    private val repository = AppModule.repository
    private val locationRepository = AppModule.locationRepository

    private val _existingMemo = MutableStateFlow<Memo?>(null)

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

    fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = repository.getMemoById(memoId)
            _existingMemo.value = memo
            _title.value = memo?.title ?: ""
            _content.value = memo?.content ?: ""
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

    fun onTitleChange(value: String) { _title.value = value }
    fun onContentChange(value: String) { _content.value = value }
    fun onManualAddressChange(value: String) { _manualAddress.value = value }

    fun fetchLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            val loc = locationRepository.getCurrentLocation()
            if (loc != null) {
                val geocoded = locationRepository.geocode(loc)
                _locationInfo.value = geocoded
            }
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
            }
            _isLocating.value = false
        }
    }

    fun clearLocation() {
        _locationInfo.value = null
    }

    fun saveMemo(onSaved: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val now = System.currentTimeMillis()
            val existing = _existingMemo.value
            val loc = _locationInfo.value
            if (existing == null) {
                repository.insertMemo(
                    Memo(
                        id = 0,
                        title = _title.value.trim(),
                        content = _content.value,
                        createdAt = now,
                        updatedAt = now,
                        latitude = loc?.latitude,
                        longitude = loc?.longitude,
                        country = loc?.country,
                        province = loc?.province,
                        city = loc?.city,
                        address = loc?.address
                    )
                )
            } else {
                repository.updateMemo(
                    existing.copy(
                        title = _title.value.trim(),
                        content = _content.value,
                        updatedAt = now,
                        latitude = loc?.latitude,
                        longitude = loc?.longitude,
                        country = loc?.country,
                        province = loc?.province,
                        city = loc?.city,
                        address = loc?.address
                    )
                )
            }
            _isSaving.value = false
            onSaved()
        }
    }
}