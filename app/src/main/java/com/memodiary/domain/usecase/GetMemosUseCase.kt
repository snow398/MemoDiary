package com.memodiary.domain.usecase

import com.memodiary.data.repository.MemoRepository
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.flow.Flow

class GetMemosUseCase(private val memoRepository: MemoRepository) {
    fun execute(): Flow<List<Memo>> = memoRepository.getAllMemos()
}