package org.fossify.documents.ui.screens

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditActionTest {
    @Test
    fun `formatting action updates state text and selection`() {
        val state = TextFieldState(
            initialText = "hello",
            initialSelection = TextRange(0, 5),
        )

        state.applyMarkdownAction(MarkdownEditAction.Bold)

        assertEquals("**hello**", state.text.toString())
        assertEquals(TextRange(0, 9), state.selection)
    }
}
