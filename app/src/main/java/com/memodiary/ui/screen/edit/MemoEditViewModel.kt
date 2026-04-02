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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _mood = MutableStateFlow(MoodType.NONE)
    val mood: StateFlow<MoodType> = _mood

    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = repository.getMemoById(memoId)
            _existingMemo.value = memo
            _title.value = memo?.title ?: ""
            _content.value = memo?.content ?: ""
            _mood.value = memo?.mood ?: MoodType.NONE
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

    fun onTitleChange(value: String) { _title.value = value }
    fun onContentChange(value: String) { _content.value = value }
    fun onManualAddressChange(value: String) { _manualAddress.value = value }
    fun onMoodChange(mood: MoodType) { _mood.value = mood }

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
            if (info != null) _locationInfo.value = info
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
            val base = Memo(
                id = existing?.id ?: 0,
                title = _title.value.trim(),
                content = _content.value,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                latitude = loc?.latitude,
                longitude = loc?.longitude,
                country = loc?.country,
                province = loc?.province,
                city = loc?.city,
                address = loc?.address,
                mood = _mood.value,
                imagePaths = _imagePaths.value
            )
            if (existing == null) repository.insertMemo(base)
            else repository.updateMemo(base)
            _isSaving.value = false
            onSaved()
        }
    }
}

