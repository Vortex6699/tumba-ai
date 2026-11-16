package com.tumba.ai.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseAccumulatorTest {

    @Test
    fun emitsPayloadOnBlankLine() {
        val accumulator = SseAccumulator()
        assertNull(accumulator.consume("data: {\"a\":1}"))
        assertEquals("{\"a\":1}", accumulator.consume(""))
    }

    @Test
    fun joinsMultipleDataLinesWithNewline() {
        val accumulator = SseAccumulator()
        accumulator.consume("data: line one")
        accumulator.consume("data: line two")
        assertEquals("line one\nline two", accumulator.consume(""))
    }

    @Test
    fun ignoresCommentsAndUnknownFields() {
        val accumulator = SseAccumulator()
        assertNull(accumulator.consume(": keep-alive"))
        assertNull(accumulator.consume("event: message"))
        accumulator.consume("data: [DONE]")
        assertEquals("[DONE]", accumulator.consume(""))
    }

    @Test
    fun flushesTrailingPayloadWithoutBlankLine() {
        val accumulator = SseAccumulator()
        accumulator.consume("data: tail")
        assertEquals("tail", accumulator.finish())
        assertNull(accumulator.finish())
    }

    @Test
    fun ignoresEmptyEvents() {
        val accumulator = SseAccumulator()
        assertNull(accumulator.consume(""))
    }
}
