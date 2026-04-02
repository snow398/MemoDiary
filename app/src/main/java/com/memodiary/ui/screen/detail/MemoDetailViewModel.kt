package com.memodiary.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memodiary.di.AppModule
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MemoDetailViewModel : ViewModel() {

    private val repository = AppModule.repository

    private val _memo = MutableStateFlow<Memo?>(null)
    val memo: StateFlow<Memo?> = _memo

    fun loadMemo(memoId: Long) {
        viewModelScope.launch {
            _memo.value = repository.getMemoById(memoId)
        }
    }
}