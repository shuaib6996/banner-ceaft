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
- **Content Moderation & Black-Image Detection**:
  - Both Gemini and NVIDIA/FLUX models may reject prompts or return pitch-black images due to built-in safety filters.
  - Handled via [`ImageSafetyValidator.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/ImageSafetyValidator.kt) and `ContentModeratedException`.
  - When detected, the app catches the condition immediately and presents a clean, user-friendly notice: *"⚠️ Content Moderated: This prompt was flagged by AI safety guidelines. Please modify your prompt and try again."* with a quick 1-tap action to tweak the prompt.

---

## 4. Multi-Provider & Model Switching Architecture

1. **Multi-Key & Multi-Provider Profiles**:
   - Users can save multiple API keys and provider profiles (Google Gemini, OpenAI Compatible, OpenRouter, Groq, DeepSeek, Together AI, local Ollama).
   - All profiles are serialized and securely encrypted via [`SecureStorageManager.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/SecureStorageManager.kt) (`saveAllProviderProfiles` / `loadAllProviderProfiles`).
   - Profile management (active profile switching, adding new keys, deleting profiles) is orchestrated by [`ApiKeyViewModel.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/state/ApiKeyViewModel.kt).

2. **In-Chat Real-Time Model Switching & Side Panel**:
   - Users can switch chat models directly from [`PromptChatScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/PromptChatScreen.kt) via [`ChatSideDrawer.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/components/ChatSideDrawer.kt).
   - Tapping the active model pill or the `≡ Panel` button slides out the drawer with instant 1-tap switching across Gemini models (`gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`, etc.) and NVIDIA NIM / OpenAI models (`meta/llama-3.2-11b-vision-instruct`, etc.).
   - Side panel displays past prompt ideas & creative banner history with 1-tap reuse, plus quick action to start a new chat session.

3. **In-Studio Real-Time Image Model Switching & Side Panel**:
   - In [`ImageEditorScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/ImageEditorScreen.kt) (AI Image Studio), users can switch image generation engines via [`StudioSideDrawer.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/components/StudioSideDrawer.kt).
   - Filtered **strictly to image models only**: Google Imagen 3 (`imagen-3.0-generate-002`, `gemini-3.1-flash-image`), NVIDIA FLUX (`black-forest-labs/flux.1-dev`), Stable Diffusion 3.5 Large, and any user-saved image profiles.
   - Side panel includes thumbnail visual previews of past generated banners with a "Load" button that reloads the image and prompt directly into the studio workspace.
   - Also supported in [`GenerationSettingsScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/GenerationSettingsScreen.kt) via `ImageModelSwitcherDialog`.

4. **Separated Chat & Image Provider Configurations**:
   - AI Chat (`chatConfig`) and Banner Image Generation (`imageConfig`) are completely decoupled.
   - Users can use one provider (e.g. NVIDIA NIM `meta/llama-3.2-11b-vision-instruct`) for Chat brainstorming and a different provider (e.g. Google Gemini `imagen-3.0-generate-002` or Flux) for Banner rendering.
   - [`ApiKeySetupScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt) provides dedicated tabs: `[ 💬 AI Chat Setup ]` and `[ 🎨 Image Setup ]`.
   - Automatic API key sanitization (`sanitizeApiKey`) strips surrounding quotes, prefixes (like `api key:`, `bearer `), and leading/trailing punctuation to prevent HTTP 401 JWT header errors.
5. **History & Prompt Architecture**:
   - [`HistoryScreen.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/ui/screens/HistoryScreen.kt) is structured into dual tabs: `[ 🎨 Images ]` and `[ 💬 Prompts ]`.
   - Both sections support multi-selection with "Select All / Deselect All", selection counter badge, and batch "Delete Selected" via [`BannerHistoryDao.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/BannerHistoryDao.kt).
   - Image models priority auto-selects NVIDIA NIM (`black-forest-labs/flux.1-dev`) first across all screens, followed by Google Imagen 3 (`imagen-3.0-generate-002`).

6. **Hugging Face Inference Providers Architecture**:
   - Integrated via [`HuggingFaceImageService.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/HuggingFaceImageService.kt) for serverless text-to-image synthesis (`black-forest-labs/FLUX.1-schnell`, `black-forest-labs/FLUX.1-dev`, `stabilityai/stable-diffusion-3.5-large`, `SDXL 1.0`).
   - Supports serverless multi-provider routing (`router.huggingface.co`) with automatic provider resolution (`expand[]=inferenceProviderMapping`) and explicit partner routing (`fal-ai`, `together`, `replicate`, `wavespeed`).
   - Conforms strictly to official Hugging Face error status code handling (401, 403, 404, 402/429, and 503 retry loop with 20s delay).
   - Reuses existing provider fields: `providerName`, `apiKey`, `baseUrl`, `imageModel`.
