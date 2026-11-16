package com.tumba.ai.ui.markdown

/**
 * A deliberately small Markdown subset for chat answers: headings, paragraphs, fenced code,
 * lists, quotes and dividers. It is pure Kotlin so it can be unit tested without Android.
 */
sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Code(val language: String?, val code: String) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class NumberedList(val items: List<String>) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

data class InlineToken(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val link: String? = null,
)

private val HEADING = Regex("^ {0,3}(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^\\s*[-*+]\\s+(.*)$")
private val NUMBERED = Regex("^\\s*\\d+[.)]\\s+(.*)$")
private val QUOTE = Regex("^\\s*>\\s?(.*)$")

fun parseMarkdown(source: String): List<MarkdownBlock> {
    val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isEmpty()) return
        val text = paragraph.joinToString("\n").trim()
        if (text.isNotEmpty()) blocks += MarkdownBlock.Paragraph(text)
        paragraph.clear()
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val language = trimmed.removePrefix("```").trim().ifBlank { null }
                val code = mutableListOf<String>()
                index += 1
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code += lines[index]
                    index += 1
                }
                if (index < lines.size) index += 1
                blocks += MarkdownBlock.Code(language, code.joinToString("\n"))
                continue
            }

            trimmed.isEmpty() -> flushParagraph()

            HEADING.matches(line) -> {
                flushParagraph()
                val match = HEADING.find(line)!!
                blocks += MarkdownBlock.Heading(
                    level = match.groupValues[1].length,
                    text = match.groupValues[2].trim(),
                )
            }

            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                flushParagraph()
                blocks += MarkdownBlock.Divider
            }

            BULLET.matches(line) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (index < lines.size && BULLET.matches(lines[index])) {
                    items += BULLET.find(lines[index])!!.groupValues[1]
                    index += 1
                }
                blocks += MarkdownBlock.BulletList(items)
                continue
            }

            NUMBERED.matches(line) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (index < lines.size && NUMBERED.matches(lines[index])) {
                    items += NUMBERED.find(lines[index])!!.groupValues[1]
                    index += 1
                }
                blocks += MarkdownBlock.NumberedList(items)
                continue
            }

            QUOTE.matches(line) -> {
                flushParagraph()
                val quoted = mutableListOf<String>()
                while (index < lines.size && QUOTE.matches(lines[index])) {
                    quoted += QUOTE.find(lines[index])!!.groupValues[1]
                    index += 1
                }
                blocks += MarkdownBlock.Quote(quoted.joinToString("\n"))
                continue
            }

            else -> paragraph += line
        }
        index += 1
    }
    flushParagraph()
    return blocks
}

/**
 * Inline formatting: `code`, **bold**, *italic*, ~~strike~~ and [links](url).
 * Nested combinations intentionally degrade to plain styles instead of failing.
 */
fun parseInline(text: String): List<InlineToken> {
    val tokens = mutableListOf<InlineToken>()
    val plain = StringBuilder()

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            tokens += InlineToken(plain.toString())
            plain.clear()
        }
    }

    var i = 0
    while (i < text.length) {
        when {
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    flushPlain()
                    tokens += InlineToken(text.substring(i + 1, end), code = true)
                    i = end + 1
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 1) {
                    flushPlain()
                    tokens += InlineToken(text.substring(i + 2, end), bold = true)
                    i = end + 2
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            text.startsWith("__", i) -> {
                val end = text.indexOf("__", i + 2)
                if (end > i + 1) {
                    flushPlain()
                    tokens += InlineToken(text.substring(i + 2, end), bold = true)
                    i = end + 2
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i + 1) {
                    flushPlain()
                    tokens += InlineToken(text.substring(i + 2, end), strike = true)
                    i = end + 2
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            text.startsWith("[", i) -> {
                val labelEnd = text.indexOf("](", i + 1)
                val urlEnd = if (labelEnd > 0) text.indexOf(')', labelEnd + 2) else -1
                if (labelEnd > 0 && urlEnd > labelEnd) {
                    flushPlain()
                    tokens += InlineToken(
                        text = text.substring(i + 1, labelEnd),
                        link = text.substring(labelEnd + 2, urlEnd),
                    )
                    i = urlEnd + 1
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            text[i] == '*' || text[i] == '_' -> {
                val marker = text[i]
                val canOpen = i + 1 < text.length && !text[i + 1].isWhitespace()
                var end = -1
                if (canOpen) {
                    var cursor = i + 1
                    while (cursor < text.length) {
                        if (text[cursor] == marker &&
                            cursor > i + 1 &&
                            !text[cursor - 1].isWhitespace()
                        ) {
                            end = cursor
                            break
                        }
                        cursor += 1
                    }
                }
                if (end > i + 1) {
                    flushPlain()
                    tokens += InlineToken(text.substring(i + 1, end), italic = true)
                    i = end + 1
                } else {
                    plain.append(text[i])
                    i += 1
                }
            }

            else -> {
                plain.append(text[i])
                i += 1
            }
        }
    }
    flushPlain()
    return tokens
}
