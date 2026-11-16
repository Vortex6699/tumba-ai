package com.tumba.ai.data.ai

import com.tumba.ai.domain.model.LocalModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The download catalogue is compiled into the app, so a typo here becomes a broken download
 * button in production. These checks keep the entries internally consistent; the URLs and
 * sizes were verified with HEAD requests against their hosts.
 */
class LocalModelCatalogTest {

    @Test
    fun `catalogue is not empty`() {
        assertTrue(LocalModelCatalog.downloads.isNotEmpty())
    }

    @Test
    fun `ids and urls are unique`() {
        val models = LocalModelCatalog.downloads
        assertEquals(models.size, models.map { it.id }.distinct().size)
        assertEquals(models.size, models.map { it.url }.distinct().size)
        assertEquals(models.size, models.map { it.fileName }.distinct().size)
    }

    @Test
    fun `entries are https and point at a gguf file`() {
        LocalModelCatalog.downloads.forEach { model ->
            assertTrue(model.id, model.url.startsWith("https://"))
            assertTrue(model.id, model.url.endsWith(".gguf"))
            assertTrue(model.id, model.fileName.endsWith(".gguf"))
        }
    }

    @Test
    fun `download size is plausible`() {
        LocalModelCatalog.downloads.forEach { model ->
            assertTrue(model.id, model.sizeBytes > 100L * 1024 * 1024)
            assertTrue(model.id, model.recommendedRamBytes > model.sizeBytes)
            assertTrue(model.id, model.contextWindow >= 2_048)
            assertTrue(model.id, model.quantization.isNotBlank())
            assertTrue(model.id, model.parameters.isNotBlank())
        }
    }

    @Test
    fun `lookup by id`() {
        val first = LocalModelCatalog.downloads.first()
        assertEquals(first, LocalModelCatalog.find(first.id))
        assertEquals(null, LocalModelCatalog.find("missing"))
    }
}