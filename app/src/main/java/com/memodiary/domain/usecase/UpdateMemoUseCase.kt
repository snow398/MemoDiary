package com.memodiary.domain.usecase

import com.memodiary.data.repository.MemoRepository
import com.memodiary.domain.model.Memo

class UpdateMemoUseCase(private val memoRepository: MemoRepository) {
    suspend operator fun invoke(memo: Memo) = memoRepository.updateMemo(memo)
}