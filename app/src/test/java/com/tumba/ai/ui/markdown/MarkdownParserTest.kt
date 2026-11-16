package com.tumba.ai.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parsesHeadingsParagraphsAndCodeFences() {
        val blocks = parseMarkdown(
            """
            # Title

            A short paragraph with **bold** text.

            ```kotlin
            val answer = 42
            ```

            ## Section
            """.trimIndent(),
        )

        assertEquals(4, blocks.size)
        assertEquals(MarkdownBlock.Heading(1, "Title"), blocks[0])
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
        assertEquals(
            MarkdownBlock.Code("kotlin", "val answer = 42"),
            blocks[2],
        )
        assertEquals(MarkdownBlock.Heading(2, "Section"), blocks[3])
    }

    @Test
    fun parsesListsQuotesAndDividers() {
        val blocks = parseMarkdown(
            """
            - one
            - two

            1. first
            2. second

            > quoted line

            ---
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                MarkdownBlock.BulletList(listOf("one", "two")),
                MarkdownBlock.NumberedList(listOf("first", "second")),
                MarkdownBlock.Quote("quoted line"),
                MarkdownBlock.Divider,
            ),
            blocks,
        )
    }

    @Test
    fun keepsIncompleteCodeFenceAsCode() {
        val blocks = parseMarkdown("```python\nprint('hi')")
        assertEquals(MarkdownBlock.Code("python", "print('hi')"), blocks.single())
    }

    @Test
    fun parsesInlineFormatting() {
        val tokens = parseInline("Use `code`, **bold**, *italic*, ~~gone~~ and [a link](https://example.com).")

        assertTrue(tokens.any { it.code && it.text == "code" })
        assertTrue(tokens.any { it.bold && it.text == "bold" })
        assertTrue(tokens.any { it.italic && it.text == "italic" })
        assertTrue(tokens.any { it.strike && it.text == "gone" })
        assertTrue(tokens.any { it.link == "https://example.com" && it.text == "a link" })
    }

    @Test
    fun leavesUnclosedMarkersAsPlainText() {
        val tokens = parseInline("2 * 3 = 6 and **not closed")
        val text = tokens.joinToString("") { it.text }
        assertEquals("2 * 3 = 6 and **not closed", text)
        assertTrue(tokens.none { it.bold })
    }

    @Test
    fun rendersEmptyInputAsNoBlocks() {
        assertTrue(parseMarkdown("").isEmpty())
        assertTrue(parseMarkdown("   \n\n").isEmpty())
    }
}
