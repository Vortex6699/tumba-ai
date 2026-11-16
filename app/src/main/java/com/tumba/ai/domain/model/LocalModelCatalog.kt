package com.tumba.ai.domain.model

/**
 * A curated, officially hosted GGUF file that TUMBA can fetch on request.
 *
 * Downloading is always user-initiated: nothing is fetched in the background and the model
 * list itself carries no model weights. Sizes mirror the `Content-Length` of the pinned
 * files and are used for progress, free-space checks and resumable downloads.
 */
data class DownloadableModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val quantization: String,
    val parameters: String,
    val contextWindow: Int,
    val recommendedRamBytes: Long,
)

/**
 * Small instruction-tuned models that fit mid-range phones. Keep this list short and
 * verified: every entry must resolve to a real GGUF file at the pinned URL.
 */
object LocalModelCatalog {

    val downloads: List<DownloadableModel> = listOf(
        DownloadableModel(
            id = "qwen2.5-0.5b-instruct-q4",
            displayName = "Qwen2.5 0.5B Instruct",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/" +
                "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeBytes = 491_400_032L,
            quantization = "Q4_K_M",
            parameters = "0.5B",
            contextWindow = 8_192,
            recommendedRamBytes = 1_500_000_000L,
        ),
        DownloadableModel(
            id = "llama-3.2-1b-instruct-q4",
            displayName = "Llama 3.2 1B Instruct",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/" +
                "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            sizeBytes = 807_694_464L,
            quantization = "Q4_K_M",
            parameters = "1B",
            contextWindow = 8_192,
            recommendedRamBytes = 2_000_000_000L,
        ),
        DownloadableModel(
            id = "qwen2.5-1.5b-instruct-q4",
            displayName = "Qwen2.5 1.5B Instruct",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/" +
                "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeBytes = 1_117_320_736L,
            quantization = "Q4_K_M",
            parameters = "1.5B",
            contextWindow = 16_384,
            recommendedRamBytes = 2_800_000_000L,
        ),
        DownloadableModel(
            id = "qwen2.5-3b-instruct-q4",
            displayName = "Qwen2.5 3B Instruct",
            fileName = "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            url = "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/" +
                "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            sizeBytes = 1_929_903_264L,
            quantization = "Q4_K_M",
            parameters = "3B",
            contextWindow = 16_384,
            recommendedRamBytes = 4_500_000_000L,
        ),
    )

    fun find(id: String): DownloadableModel? = downloads.firstOrNull { it.id == id }
}