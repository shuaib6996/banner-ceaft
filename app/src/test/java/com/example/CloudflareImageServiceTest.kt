package com.example

import com.example.data.ApiProviderPresets
import com.example.data.CloudflareImageService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudflareImageServiceTest {

    private lateinit var service: CloudflareImageService

    @Before
    fun setUp() {
        service = CloudflareImageService()
    }

    @Test
    fun extractAccountIdAndEndpoint_fullUrl_resolvesCorrectly() {
        val baseUrl = "https://api.cloudflare.com/client/v4/accounts/023e105f4ecef8ad9ca31a8372d0c353/ai/run/"
        val model = "@cf/black-forest-labs/flux-1-schnell"

        val (accountId, endpoint) = service.extractAccountIdAndEndpoint(baseUrl, model)

        assertEquals("023e105f4ecef8ad9ca31a8372d0c353", accountId)
        assertEquals(
            "https://api.cloudflare.com/client/v4/accounts/023e105f4ecef8ad9ca31a8372d0c353/ai/run/@cf/black-forest-labs/flux-1-schnell",
            endpoint
        )
    }

    @Test
    fun extractAccountIdAndEndpoint_rawAccountId_resolvesCorrectly() {
        val rawId = "023e105f4ecef8ad9ca31a8372d0c353"
        val model = "@cf/bytedance/stable-diffusion-xl-lightning"

        val (accountId, endpoint) = service.extractAccountIdAndEndpoint(rawId, model)

        assertEquals("023e105f4ecef8ad9ca31a8372d0c353", accountId)
        assertEquals(
            "https://api.cloudflare.com/client/v4/accounts/023e105f4ecef8ad9ca31a8372d0c353/ai/run/@cf/bytedance/stable-diffusion-xl-lightning",
            endpoint
        )
    }

    @Test
    fun extractAccountIdAndEndpoint_autoPrependsCfPrefixWhenOmitted() {
        val rawId = "myaccount123"
        val model = "black-forest-labs/flux-1-schnell" // user didn't write @cf/

        val (accountId, endpoint) = service.extractAccountIdAndEndpoint(rawId, model)

        assertEquals("myaccount123", accountId)
        assertTrue(endpoint.endsWith("/@cf/black-forest-labs/flux-1-schnell"))
    }

    @Test
    fun buildPayload_fluxModel_setsStepsParameter() {
        val payload = service.buildPayload("@cf/black-forest-labs/flux-1-schnell", "A neon cyberpunk banner", steps = 4)

        assertEquals("A neon cyberpunk banner", payload.getString("prompt"))
        assertEquals(4, payload.getInt("steps"))
        assertTrue(!payload.has("num_steps"))
    }

    @Test
    fun buildPayload_sdxlLightning_setsNumSteps() {
        val payload = service.buildPayload("@cf/bytedance/stable-diffusion-xl-lightning", "Fantasy mountain landscape", steps = 8)

        assertEquals("Fantasy mountain landscape", payload.getString("prompt"))
        assertEquals(8, payload.getInt("num_steps"))
    }

    @Test
    fun buildPayload_sdxlBase_setsNumStepsAndGuidance() {
        val payload = service.buildPayload("@cf/stabilityai/stable-diffusion-xl-base-1.0", "Minimalist aesthetic banner")

        assertEquals("Minimalist aesthetic banner", payload.getString("prompt"))
        assertEquals(20, payload.getInt("num_steps"))
        assertEquals(7.5, payload.getDouble("guidance"), 0.01)
    }

    @Test
    fun presetCloudflare_isRegisteredInAllPresets() {
        val preset = ApiProviderPresets.getPresetById("cloudflare")
        assertNotNull(preset)
        assertEquals("Cloudflare Workers AI", preset.name)
        assertEquals("@cf/black-forest-labs/flux-1-schnell", preset.defaultImageModel)

        val inList = ApiProviderPresets.ALL_PRESETS.any { it.id == "cloudflare" }
        assertTrue(inList)
    }

    @Test
    fun cloudflareImageModels_containsFreeDailyAndMeteredModels() {
        val models = ApiProviderPresets.CLOUDFLARE_IMAGE_MODELS
        assertTrue(models.isNotEmpty())

        val schnell = models.find { it.id == "@cf/black-forest-labs/flux-1-schnell" }
        assertNotNull(schnell)
        assertTrue(schnell!!.displayName.contains("Free Daily"))

        val lightning = models.find { it.id == "@cf/bytedance/stable-diffusion-xl-lightning" }
        assertNotNull(lightning)
        assertTrue(lightning!!.displayName.contains("Free Daily"))

        val sdxlBase = models.find { it.id == "@cf/stabilityai/stable-diffusion-xl-base-1.0" }
        assertNotNull(sdxlBase)
        assertTrue(sdxlBase!!.displayName.contains("Metered"))
    }

    @Test
    fun sanitizePromptLength_shortPrompt_remainsUnchanged() {
        val short = "A vibrant gaming banner"
        assertEquals(short, service.sanitizePromptLength(short))
    }

    @Test
    fun sanitizePromptLength_ultraLongPrompt_condensedUnderLimit() {
        val ultraLong = "A futuristic cyberpunk city with flying vehicles and glowing neon signs. ".repeat(40)
        assertTrue(ultraLong.length > 2500)

        val sanitized = service.sanitizePromptLength(ultraLong, 1900)
        assertTrue("Expected length <= 1900 but was ${sanitized.length}", sanitized.length <= 1900)
        assertTrue(sanitized.endsWith("."))
    }

    @Test
    fun buildPayload_ultraLongPrompt_payloadPromptClamped() {
        val ultraLong = "Epic space battle with starships, planets, nebulae, cosmic dust. ".repeat(40)
        val payload = service.buildPayload("@cf/black-forest-labs/flux-1-schnell", ultraLong)

        val promptInPayload = payload.getString("prompt")
        assertTrue("Prompt in payload must be <= 1900 chars", promptInPayload.length <= 1900)
    }
}
