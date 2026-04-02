package com.memodiary.ui.screen.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MemoEditViewModel : ViewModel() {

    private val repository = AppModule.repository

    // The memo being edited (null means we are creating a new one)
    private val _existingMemo = MutableStateFlow<Memo?>(null)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            val memo = repository.getMemoById(memoId)
            _existingMemo.value = memo
            _title.value = memo?.title ?: ""
            _content.value = memo?.content ?: ""
        }
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onContentChange(value: String) { _content.value = value }

    fun saveMemo(onSaved: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val now = System.currentTimeMillis()
            val existing = _existingMemo.value
            if (existing == null) {
                repository.insertMemo(
                    Memo(
                        id = 0,
                        title = _title.value.trim(),
                        content = _content.value,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                repository.updateMemo(
                    existing.copy(
                        title = _title.value.trim(),
                        content = _content.value,
                        updatedAt = now
                    )
                )
            }
            _isSaving.value = false
            onSaved()
        }
    }
}