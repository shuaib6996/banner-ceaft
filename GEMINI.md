# Agent Guidelines & Project Instructions: BannerCraft

This document provides behavioral constraints, styling rules, and coding conventions for AI assistants (Antigravity, Gemini, Claude, etc.) and human developers contributing to **BannerCraft**.

---

## 1. Project Core Principles

1. **YouTube Safe-Area First**:
   - Any banner composition or visual element rendering must respect the official YouTube 2025/2026 safe zone guidelines defined in [`TargetDimensions.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/TargetDimensions.kt).
   - Core text (Channel Name, Subtext) and logos/avatars must never be placed outside the `1546 x 423 px` centered area on the `2560 x 1440 px` canvas.

2. **Neumorphic Aesthetic Consistency**:
   - Do not replace custom neumorphic modifiers with standard flat Material 3 cards unless explicitly requested.
   - Use `Modifier.neumorphicConvex()` for elevated interactive surfaces.
   - Use `Modifier.neumorphicInset()` for debossed text fields, sliders, and sunken containers.
   - Use `Modifier.tactilePressable()` for tactile micro-interactions on buttons.

3. **Privacy & Security**:
   - **Never hardcode Gemini API keys** into source code or commit them into git.
   - All user API keys must be handled through [`ApiKeyViewModel`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/state/ApiKeyViewModel.kt) and stored via [`SecureStorageManager`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/SecureStorageManager.kt) (`EncryptedSharedPreferences`).
   - Keep `.env` in `.gitignore`.

---

## 2. Kotlin & Compose Standards

- **State Management**:
  - Expose state from ViewModels using `StateFlow<T>`.
  - Collect state in Composables using `collectAsState()` or `collectAsStateWithLifecycle()`.
  - Keep Composables stateless where possible by hoisting events up to the screen level.
- **Coroutines & Threading**:
  - Perform network operations (OkHttp calls) and heavy bitmap manipulation strictly on `Dispatchers.IO` or `Dispatchers.Default`.
  - Never block the Main (UI) thread with synchronous I/O or image decoding.
- **Dependencies & Version Catalog**:
  - Always check [`gradle/libs.versions.toml`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/gradle/libs.versions.toml) before adding any dependencies to maintain consistent versioning.

---

## 3. Gemini Image Generation Conventions

- Primary model endpoint: `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image:generateContent`
- Fallback model endpoint: `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent`
- In request body:
  - `generationConfig.responseModalities`: Must include `"IMAGE"`.
  - `generationConfig.imageConfig.aspectRatio`: Must map to valid aspect ratios (`"16:9"`, `"4:3"`, `"1:1"`, `"9:16"`).
- Always handle HTTP 401/403 (Invalid Key), HTTP 429 (Rate Limit), and network disconnection gracefully with user-friendly error messages.

---

## 4. Multi-Provider & Model Switching Architecture

1. **Multi-Key & Multi-Provider Profiles**:
   - Users can save multiple API keys and provider profiles (Google Gemini, OpenAI Compatible, OpenRouter, Groq, DeepSeek, Together AI, local Ollama).
   - All profiles are serialized and securely encrypted via [`SecureStorageManager.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/SecureStorageManager.kt) (`saveAllProviderProfiles` / `loadAllProviderProfiles`).
   - Profile management (active profile switching, adding new keys, deleting profiles) is orchestrated by [`ApiKeyViewModel.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/state/ApiKeyViewModel.kt).

2. **In-Chat Real-Time Model Switching**:
   - Users can switch chat models directly from [`PromptChatScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/PromptChatScreen.kt) without visiting Settings.
   - Tapping the active model pill opens `ChatModelSwitcherDialog` with instant 1-tap switching across Gemini models (`gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`, etc.) and custom OpenAI-compatible models.

3. **In-Studio Real-Time Image Model Switching**:
   - Users can switch image generation engines directly from [`GenerationSettingsScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/GenerationSettingsScreen.kt) before generating banners.
   - Tapping the Image Model card opens `ImageModelSwitcherDialog` to switch between `imagen-3.0-generate-002`, `imagen-3`, `gemini-3.1-flash-image`, `qwen-image`, `flux.2-klein-4b`, `stable-diffusion-3.5-large`, etc.

4. **Separated Chat & Image Provider Configurations**:
   - AI Chat (`chatConfig`) and Banner Image Generation (`imageConfig`) are completely decoupled.
   - Users can use one provider (e.g. NVIDIA NIM `meta/llama-3.2-11b-vision-instruct`) for Chat brainstorming and a different provider (e.g. Google Gemini `imagen-3.0-generate-002` or Flux) for Banner rendering.
   - [`ApiKeySetupScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt) provides dedicated tabs: `[ 💬 AI Chat Setup ]` and `[ 🎨 Image Setup ]`.
   - Automatic API key sanitization (`sanitizeApiKey`) strips surrounding quotes, prefixes (like `api key:`, `bearer `), and leading/trailing punctuation to prevent HTTP 401 JWT header errors.

