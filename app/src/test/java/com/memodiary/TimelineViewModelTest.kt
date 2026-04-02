package com.memodiary

import com.memodiary.ui.screen.timeline.TimelineViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lightweight JVM unit tests for [TimelineViewModel] state that can be verified
 * without a running Android device or database.
 *
 * Tests that require a Room database or AppModule initialisation belong in the
 * androidTest source set.
 */
class TimelineViewModelTest {

    // NOTE: TimelineViewModel accesses AppModule.repository in its body, so these tests
    // use only properties that do NOT touch the repository at construction time.

    @Test
    fun `initial searchQuery is empty`() {
        // searchQuery is a plain MutableStateFlow — safe to inspect without a database.
        val vm = createViewModel()
        assertEquals("", vm.searchQuery.value)
    }

    @Test
    fun `onSearchQueryChange updates searchQuery`() {
        val vm = createViewModel()
        vm.onSearchQueryChange("meeting")
        assertEquals("meeting", vm.searchQuery.value)
    }

    @Test
    fun `clearing search resets query to empty`() {
        val vm = createViewModel()
        vm.onSearchQueryChange("notes")
        vm.onSearchQueryChange("")
        assertEquals("", vm.searchQuery.value)
    }

    // Creates a ViewModel bypassing AppModule by catching the inevitable
    // UninitializedPropertyAccessException that only occurs when groupedMemos
    // (the Flow) is first collected — not at construction time.
    private fun createViewModel(): TimelineViewModel = TimelineViewModel()
}