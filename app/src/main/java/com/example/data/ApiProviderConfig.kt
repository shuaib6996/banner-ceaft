package com.example.data

import org.json.JSONObject
import java.util.UUID

/**
 * Data classes and presets for OpenAI-compatible and Gemini API providers,
 * modeled after developer tools like Roo Code and Continue.
 */
data class ProviderPreset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultChatModel: String,
    val defaultImageModel: String,
    val popularChatModels: List<String>,
    val popularImageModels: List<String>,
    val description: String,
    val keyHint: String,
    val isOpenAiCompatible: Boolean = true
)

data class ApiProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val providerId: String = "gemini",
    val providerName: String = "Google Gemini (Direct)",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val apiKey: String = "",
    val chatModel: String = "gemini-2.0-flash",
    val imageModel: String = "imagen-3.0-generate-002",
    val isOpenAiCompatible: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("providerId", providerId)
        json.put("providerName", providerName)
        json.put("baseUrl", baseUrl)
        json.put("apiKey", apiKey)
        json.put("chatModel", chatModel)
        json.put("imageModel", imageModel)
        json.put("isOpenAiCompatible", isOpenAiCompatible)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ApiProviderConfig {
            return ApiProviderConfig(
                id = json.optString("id", UUID.randomUUID().toString()),
                providerId = json.optString("providerId", "gemini"),
                providerName = json.optString("providerName", "Google Gemini"),
                baseUrl = json.optString("baseUrl", "https://generativelanguage.googleapis.com"),
                apiKey = json.optString("apiKey", ""),
                chatModel = json.optString("chatModel", "gemini-2.0-flash"),
                imageModel = json.optString("imageModel", "imagen-3.0-generate-002"),
                isOpenAiCompatible = json.optBoolean("isOpenAiCompatible", false)
            )
        }
    }
}

data class ModelOption(
    val id: String,
    val displayName: String,
    val description: String
)

object ApiProviderPresets {

    val GEMINI_CHAT_MODELS = listOf(
        ModelOption("gemini-4-argon", "Gemini 4 Argon", "Google's flagship frontier reasoning model (Pro Plan / 1M context)"),
        ModelOption("gemini-3.8-flash", "Gemini 3.8 Flash", "Google's latest flagship high-speed model (Recommended & Active)"),
        ModelOption("gemini-flash-latest", "Gemini Flash (Latest)", "Always routes to the latest active production flash model"),
        ModelOption("gemini-3.7-flash", "Gemini 3.7 Flash", "Fast reasoning & creative director chat model"),
        ModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", "High-efficiency balanced speed & quality"),
        ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep reasoning & complex prompt architecture"),
        ModelOption("gemini-pro-latest", "Gemini Pro (Latest)", "Always routes to latest production pro model")
    )

    val GEMINI_IMAGE_MODELS = listOf(
        ModelOption("imagen-3.0-generate-002", "Imagen 3.0 (generate-002)", "Official Google AI Studio image model (Recommended)"),
        ModelOption("imagen-3", "Imagen 3", "Standard Imagen 3 identifier"),
        ModelOption("gemini-3.1-flash-image", "Gemini 3.1 Flash Image", "Multimodal direct image generation"),
        ModelOption("gemini-3.1-flash-image-preview", "Gemini 3.1 Flash Image Preview", "Preview multimodal generation")
    )

    val NVIDIA_CHAT_MODELS = listOf(
        ModelOption("meta/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision", "Ultra-fast vision & chat intelligence (Recommended & Tested)"),
        ModelOption("nvidia/nemotron-3-ultra-550b-a55b", "Nemotron 3 Ultra 550B", "Flagship NVIDIA 550B model with deep creative reasoning (Tested)"),
        ModelOption("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "Nemotron 3 Nano Omni 30B", "Fast reasoning & brainstorming engine (Tested)")
    )

    val NVIDIA_IMAGE_MODELS = listOf(
        ModelOption("black-forest-labs/flux.1-dev", "FLUX.1 Dev", "High-definition flagship visual synthesis (Recommended & Tested)"),
        ModelOption("black-forest-labs/flux.1-schnell", "FLUX.1 Schnell", "Fast distilled high-speed generation (Tested)"),
        ModelOption("black-forest-labs/flux.1-kontext-dev", "FLUX.1 Kontext Dev", "Multimodal in-context image generation"),
        ModelOption("black-forest-labs/flux.2-klein-4b", "FLUX.2 Klein 4B", "Next-gen compact visual diffusion")
    )

    val PRESET_GEMINI = ProviderPreset(
        id = "gemini",
        name = "Google Gemini (Direct)",
        baseUrl = "https://generativelanguage.googleapis.com",
        defaultChatModel = "gemini-3.8-flash",
        defaultImageModel = "imagen-3.0-generate-002",
        popularChatModels = GEMINI_CHAT_MODELS.map { it.id },
        popularImageModels = GEMINI_IMAGE_MODELS.map { it.id },
        description = "Direct Google AI Studio API for Gemini and Imagen 3 models.",
        keyHint = "AIzaSy...",
        isOpenAiCompatible = false
    )

    val PRESET_OPENAI_COMPATIBLE = ProviderPreset(
        id = "openai_compatible",
        name = "OpenAI Compatible",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        defaultChatModel = "meta/llama-3.2-11b-vision-instruct",
        defaultImageModel = "black-forest-labs/flux.1-dev",
        popularChatModels = listOf(
            "meta/llama-3.2-11b-vision-instruct",
            "nvidia/nemotron-3-ultra-550b-a55b",
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning"
        ),
        popularImageModels = NVIDIA_IMAGE_MODELS.map { it.id },
        description = "NVIDIA NIM / OpenAI-compatible endpoint. Preconfigured with your endpoint & models.",
        keyHint = "nvapi-..."
    )

    val PRESET_OPENROUTER = ProviderPreset(
        id = "openrouter",
        name = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultChatModel = "meta-llama/llama-3.2-3b-instruct:free",
        defaultImageModel = "black-forest-labs/flux-1-schnell",
        popularChatModels = listOf("meta-llama/llama-3.2-3b-instruct:free", "google/gemini-2.0-flash-exp:free"),
        popularImageModels = listOf("black-forest-labs/flux-1-schnell"),
        description = "Unified gateway to models. Enter your Base URL and model IDs.",
        keyHint = "sk-or-v1-..."
    )

    val PRESET_OPENAI = ProviderPreset(
        id = "openai",
        name = "OpenAI",
        baseUrl = "",
        defaultChatModel = "",
        defaultImageModel = "",
        popularChatModels = emptyList(),
        popularImageModels = emptyList(),
        description = "OpenAI endpoints. Enter your Base URL and model IDs.",
        keyHint = "sk-proj-..."
    )

    val PRESET_GROQ = ProviderPreset(
        id = "groq",
        name = "Groq",
        baseUrl = "",
        defaultChatModel = "",
        defaultImageModel = "",
        popularChatModels = emptyList(),
        popularImageModels = emptyList(),
        description = "Fast LPU inference. Enter your Base URL and model IDs.",
        keyHint = "gsk_..."
    )

    val PRESET_DEEPSEEK = ProviderPreset(
        id = "deepseek",
        name = "DeepSeek",
        baseUrl = "",
        defaultChatModel = "",
        defaultImageModel = "",
        popularChatModels = emptyList(),
        popularImageModels = emptyList(),
        description = "DeepSeek reasoning models. Enter your Base URL and model IDs.",
        keyHint = "sk-..."
    )

    val PRESET_TOGETHER = ProviderPreset(
        id = "together",
        name = "Together AI",
        baseUrl = "",
        defaultChatModel = "",
        defaultImageModel = "",
        popularChatModels = emptyList(),
        popularImageModels = emptyList(),
        description = "Together cloud endpoints. Enter your Base URL and model IDs.",
        keyHint = "..."
    )

    val PRESET_OLLAMA = ProviderPreset(
        id = "ollama",
        name = "Ollama (Local)",
        baseUrl = "",
        defaultChatModel = "",
        defaultImageModel = "",
        popularChatModels = emptyList(),
        popularImageModels = emptyList(),
        description = "Local machine endpoint. Enter your Base URL and model IDs.",
        keyHint = "Optional (can be blank)"
    )

    val HUGGINGFACE_IMAGE_MODELS = listOf(
        ModelOption("black-forest-labs/FLUX.1-schnell", "FLUX.1 Schnell", "Fast distilled high-speed generation (Recommended)"),
        ModelOption("black-forest-labs/FLUX.1-dev", "FLUX.1 Dev", "High-definition flagship visual synthesis"),
        ModelOption("stabilityai/stable-diffusion-3.5-large", "SD 3.5 Large", "Stability AI next-generation flagship"),
        ModelOption("stabilityai/stable-diffusion-xl-base-1.0", "SDXL 1.0", "Standard high-resolution diffusion")
    )

    val PRESET_HUGGINGFACE = ProviderPreset(
        id = "huggingface",
        name = "Hugging Face",
        baseUrl = "auto",
        defaultChatModel = "",
        defaultImageModel = "black-forest-labs/FLUX.1-schnell",
        popularChatModels = emptyList(),
        popularImageModels = HUGGINGFACE_IMAGE_MODELS.map { it.id },
        description = "Hugging Face Inference Providers. Access FLUX, SDXL & open models via serverless routing.",
        keyHint = "hf_...",
        isOpenAiCompatible = false
    )

    val CLOUDFLARE_IMAGE_MODELS = listOf(
        ModelOption(
            id = "@cf/black-forest-labs/flux-1-schnell",
            displayName = "FLUX.1 Schnell [Free Daily]",
            description = "12B rectified flow, fast 4-step generation. Eligible for Cloudflare's 10,000 free Neurons/day. (Recommended)"
        ),
        ModelOption(
            id = "@cf/bytedance/stable-diffusion-xl-lightning",
            displayName = "SDXL Lightning [Free Daily]",
            description = "Distilled high-speed SDXL in 4-8 steps. Runs within free daily 10k Neurons allocation."
        ),
        ModelOption(
            id = "@cf/lykon/dreamshaper-8-lcm",
            displayName = "DreamShaper 8 LCM [Free Daily]",
            description = "Fast Latent Consistency Model (photorealism & anime). Free daily allocation eligible."
        ),
        ModelOption(
            id = "@cf/black-forest-labs/flux-2-klein-4b",
            displayName = "FLUX.2 Klein 4B [Free Daily]",
            description = "Next-gen ultra-fast compact diffusion model. Efficient generation within daily quota."
        ),
        ModelOption(
            id = "@cf/stabilityai/stable-diffusion-xl-base-1.0",
            displayName = "SDXL Base 1.0 [Metered / High Neurons]",
            description = "Foundational 1024px diffusion (20-50 steps). Higher Neurons; free up to 10k/day, then metered at $0.011/1k Neurons."
        ),
        ModelOption(
            id = "@cf/black-forest-labs/flux-2-dev",
            displayName = "FLUX.2 Dev [Metered / Paid Plan]",
            description = "Flagship multi-reference visual synthesis. Compute-intensive, requires paid plan or metered Neurons."
        )
    )

    val PRESET_CLOUDFLARE = ProviderPreset(
        id = "cloudflare",
        name = "Cloudflare Workers AI",
        baseUrl = "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/",
        defaultChatModel = "",
        defaultImageModel = "@cf/black-forest-labs/flux-1-schnell",
        popularChatModels = emptyList(),
        popularImageModels = CLOUDFLARE_IMAGE_MODELS.map { it.id },
        description = "Cloudflare Workers AI serverless GPU inference. Free 10,000 Neurons/day across eligible models.",
        keyHint = "Cloudflare API Token (Workers AI Read/Edit permission)",
        isOpenAiCompatible = false
    )

    val ALL_PRESETS = listOf(
        PRESET_GEMINI,
        PRESET_OPENAI_COMPATIBLE,
        PRESET_HUGGINGFACE,
        PRESET_CLOUDFLARE,
        PRESET_OPENROUTER,
        PRESET_OPENAI,
        PRESET_GROQ,
        PRESET_DEEPSEEK,
        PRESET_TOGETHER,
        PRESET_OLLAMA
    )

    fun getPresetById(id: String): ProviderPreset {
        if (id.equals("custom", ignoreCase = true) || id.equals("openai_compatible", ignoreCase = true)) return PRESET_OPENAI_COMPATIBLE
        if (id.equals("huggingface", ignoreCase = true)) return PRESET_HUGGINGFACE
        if (id.equals("cloudflare", ignoreCase = true)) return PRESET_CLOUDFLARE
        return ALL_PRESETS.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PRESET_GEMINI
    }
}
