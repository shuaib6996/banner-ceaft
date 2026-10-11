package com.example.data

data class AiModelItem(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String,
    val isRecommended: Boolean = false
)

object AiModelConfig {
    const val DEFAULT_MODEL = "gemini-3.8-flash"

    val ALL_MODELS = listOf(
        // --- Flash Models (Fast & High Quota - Recommended) ---
        AiModelItem(
            id = "gemini-3.8-flash",
            displayName = "gemini-3.8-flash",
            category = "Flash Models (Fast & High Quota)",
            description = "Google's latest flagship model, high rate limits",
            isRecommended = true
        ),
        AiModelItem(
            id = "gemini-flash-latest",
            displayName = "gemini-flash-latest",
            category = "Flash Models (Fast & High Quota)",
            description = "Always points to latest active production flash model"
        ),
        AiModelItem(
            id = "gemini-3.7-flash",
            displayName = "gemini-3.7-flash",
            category = "Flash Models (Fast & High Quota)",
            description = "Fast reasoning & creative director chat model"
        ),
        AiModelItem(
            id = "gemini-3.5-flash",
            displayName = "gemini-3.5-flash",
            category = "Flash Models (Fast & High Quota)",
            description = "High efficiency balanced speed & intelligence"
        ),

        // --- Pro Models (Heavy Reasoning) ---
        AiModelItem(
            id = "gemini-3.1-pro-preview",
            displayName = "gemini-3.1-pro-preview",
            category = "Pro Models (Heavy Reasoning)",
            description = "Deep reasoning & complex prompt architecture"
        ),
        AiModelItem(
            id = "gemini-pro-latest",
            displayName = "gemini-pro-latest",
            category = "Pro Models (Heavy Reasoning)",
            description = "Always points to latest production pro model"
        ),

        // --- Image Generation Models ---
        AiModelItem(
            id = "imagen-3.0-generate-002",
            displayName = "imagen-3.0-generate-002",
            category = "Image Generation Models",
            description = "Dedicated Google Imagen visual synthesis (Recommended)",
            isRecommended = true
        ),
        AiModelItem(
            id = "imagen-3",
            displayName = "imagen-3",
            category = "Image Generation Models",
            description = "Advanced Google Imagen 3 diffusion model"
        ),
        AiModelItem(
            id = "gemini-3.1-flash-image",
            displayName = "gemini-3.1-flash-image",
            category = "Image Generation Models",
            description = "Direct multimodal image generation"
        ),
        AiModelItem(
            id = "gemini-3.1-flash-image-preview",
            displayName = "gemini-3.1-flash-image-preview",
            category = "Image Generation Models",
            description = "Preview multimodal image generation"
        ),

        // --- Embedding Models ---
        AiModelItem(
            id = "text-embedding-004",
            displayName = "text-embedding-004",
            category = "Embedding Models",
            description = "State-of-the-art semantic text embeddings"
        ),
        AiModelItem(
            id = "gemini-embedding-001",
            displayName = "gemini-embedding-001",
            category = "Embedding Models",
            description = "General multimodal embedding vectors"
        )
    )

    fun getModelById(id: String?): AiModelItem {
        return ALL_MODELS.find { it.id.equals(id, ignoreCase = true) } ?: ALL_MODELS.first()
    }
}
