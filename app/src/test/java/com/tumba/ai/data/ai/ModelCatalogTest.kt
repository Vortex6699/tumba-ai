package com.tumba.ai.data.ai

import com.tumba.ai.domain.model.ProviderDefaults
import com.tumba.ai.domain.model.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun everyModelHasUniqueIdentity() {
        val identities = ModelCatalog.allCloud.map { it.providerId to it.id }
        assertEquals(identities.size, identities.distinct().size)
    }

    @Test
    fun defaultModelUsesTheStableLatestAlias() {
        val default = ModelCatalog.gemini.first { it.isDefault }
        assertEquals("gemini", default.providerId)
        assertEquals("gemini-flash-latest", default.id)
        assertEquals(ProviderDefaults.GEMINI_MODEL, default.id)
    }

    @Test
    fun retiredGeminiModelsNormalizeToTheLatestAlias() {
        assertEquals(
            ProviderDefaults.GEMINI_MODEL,
            ProviderDefaults.normalizeGeminiModel("gemini-2.0-flash"),
        )
        assertEquals(
            ProviderDefaults.GEMINI_MODEL,
            ProviderDefaults.normalizeGeminiModel("gemini-2.0-flash-lite"),
        )
        assertEquals(
            "gemini-2.5-flash",
            ProviderDefaults.normalizeGeminiModel("gemini-2.5-flash"),
        )
    }

    @Test
    fun cloudCatalogHasPositiveContextWindows() {
        assertTrue(ModelCatalog.allCloud.all { (it.contextWindow ?: 0) > 0 })
    }

    @Test
    fun localModelsAreMarkedLocalAndTextOnly() {
        val local = ModelCatalog.localModel("abc", "My GGUF")
        assertEquals(ProviderKind.LOCAL, local.kind)
        assertTrue(!local.capabilities.supportsImages)
    }
}
