package com.memodiary.domain.usecase

import com.memodiary.data.repository.MemoRepository

class DeleteMemoUseCase(private val memoRepository: MemoRepository) {
    suspend operator fun invoke(memoId: Long) = memoRepository.deleteMemo(memoId)
}