package com.example

import com.example.state.BannerCreationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BannerCreationViewModelTest {

    private lateinit var viewModel: BannerCreationViewModel

    @Before
    fun setUp() {
        viewModel = BannerCreationViewModel()
    }

    @Test
    fun buildBasePrompt_combinesInputsCorrectly() {
        viewModel.updateChannelTopic("Cozy Minecraft gaming")
        viewModel.updateSelectedMood("Calm & Cozy")
        viewModel.updateCustomPromptDetails("Warm fireplace and glowing lanterns")

        val prompt = viewModel.buildBasePrompt()

        assertTrue(prompt.contains("Calm & Cozy"))
        assertTrue(prompt.contains("Cozy Minecraft gaming"))
        assertTrue(prompt.contains("Warm fireplace and glowing lanterns"))
    }

    @Test
    fun buildBasePrompt_omitsEmptyCustomDetailsAndColors() {
        viewModel.updateChannelTopic("Tech Reviews")
        viewModel.updateSelectedMood("Minimal")
        viewModel.updateCustomPromptDetails("   ") // Whitespace only

        val state = viewModel.state.value
        val prompt = state.basePromptFragment

        assertTrue(prompt.contains("Minimal"))
        assertTrue(prompt.contains("Tech Reviews"))
        assertFalse(prompt.contains("Additional creator notes:"))
    }

    @Test
    fun buildFinalPrompt_assemblesFullPromptWithStyleAndSpaceInstructions() {
        viewModel.updateChannelTopic("Retro Gaming")
        viewModel.updateSelectedMood("Energetic")
        viewModel.updateSelectedStyle("3D Render")
        viewModel.updateWantsText(true)
        viewModel.updateUseAiTextInImage(false) // Path B
        viewModel.updateTextPosition("Center")
        viewModel.updateWantsLogo(true)
        viewModel.updateLogoPosition("Left of Title")

        val finalPrompt = viewModel.buildFinalPrompt()

        assertTrue(finalPrompt.contains("3D render, soft studio lighting"))
        assertTrue(finalPrompt.contains("the center area subtle enough"))
        assertTrue(finalPrompt.contains("the left side of the safe zone simple for an avatar logo"))
        assertTrue(finalPrompt.contains("YouTube banner background design"))
    }

    @Test
    fun buildFinalPrompt_handlesPathAIInImageTextRendering() {
        viewModel.updateChannelTopic("Alex Stream")
        viewModel.updateSelectedMood("Bold & Intense")
        viewModel.updateWantsText(true)
        viewModel.updateUseAiTextInImage(true) // Path A
        viewModel.updateTextContent("Alex Gaming HD", "Tagline")

        val finalPrompt = viewModel.buildFinalPrompt()

        assertTrue(finalPrompt.contains("Include legible overlay title text reading 'Alex Gaming HD'"))
    }

    @Test
    fun buildFinalPrompt_removesClausesWhenTogglesTurnedOff() {
        viewModel.updateChannelTopic("Gaming Vlogs")
        viewModel.updateTextPosition("Top")
        viewModel.updateWantsText(true)
        viewModel.updateWantsLogo(true)

        var finalPrompt = viewModel.buildFinalPrompt()
        assertTrue(finalPrompt.contains("simple and uncluttered"))

        // Turn off text & logo
        viewModel.updateWantsText(false)
        viewModel.updateWantsLogo(false)

        finalPrompt = viewModel.buildFinalPrompt()
        assertFalse(finalPrompt.contains("simple and uncluttered"))
        assertFalse(finalPrompt.contains("avatar logo"))
    }

    @Test
    fun targetDeviceMode_injectsBoundaryConstraintsForPhoneSafe() {
        viewModel.updateTargetDeviceMode("Phone Safe")
        val state = viewModel.state.value
        assertEquals("Phone Safe", state.targetDeviceMode)

        val prompt = viewModel.buildFinalPrompt()
        assertTrue(prompt.contains("middle 1546x423 area"))
        assertTrue(prompt.contains("CRITICAL BOUNDARY CONSTRAINT"))
    }

    @Test
    fun targetDeviceMode_injectsBoundaryConstraintsForDesktop() {
        viewModel.updateTargetDeviceMode("Desktop")
        val state = viewModel.state.value
        assertEquals("Desktop", state.targetDeviceMode)

        val prompt = viewModel.buildFinalPrompt()
        assertTrue(prompt.contains("middle 2560x423 area"))
        assertTrue(prompt.contains("desktop banner band"))
    }

    @Test
    fun targetDimensions_resolvesTargetSizesCorrectly() {
        val mobile = com.example.data.YouTubeBannerTarget.fromLabel("Phone Safe")
        assertEquals(1546, mobile.width)
        assertEquals(423, mobile.height)

        val desktop = com.example.data.YouTubeBannerTarget.fromLabel("Desktop")
        assertEquals(2560, desktop.width)
        assertEquals(423, desktop.height)

        val full = com.example.data.YouTubeBannerTarget.fromLabel("Full TV")
        assertEquals(2560, full.width)
        assertEquals(1440, full.height)
    }
}

