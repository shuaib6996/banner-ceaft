# Project Memory: BannerCraft

**App Name:** BannerCraft  
**Package:** `com.example` (`com.aistudio.bannercraft.x8q2k`)  
**Tagline:** AI-Powered YouTube Channel Studio and Banner Creation app with soft neumorphic UI.  
**Platform:** Android (Min SDK 24, Target SDK 36, Compile SDK 36.1)  
**Primary Language:** Kotlin 2.2.10 | Jetpack Compose (BOM 2024.09.00)  

---

## 1. Project Overview & Intent

BannerCraft is a specialized Android application designed for YouTube creators to conceptualize, generate, customize, and export professional YouTube channel banners and graphics using Google's **Gemini 3.1 Flash Image** AI model.

The interface is built with an aesthetic **soft neumorphic design system** supporting both dynamic Light and Dark modes with tactile spring animations, inset cavities for inputs, and convex elevations for cards and controls.

---

## 2. Key Specifications & Specs

### YouTube Banner & Safe Zone Dimensions
Defined in [`TargetDimensions.kt`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/TargetDimensions.kt):
- **Full Canvas (TV Display):** `2560 x 1440 px` (Aspect Ratio: `16:9`)
- **Desktop Max Display:** `2560 x 423 px`
- **Tablet Display:** `1855 x 423 px`
- **Mobile Safe Area (All Devices):** `1546 x 423 px` (Centered at `x = 507, y = 508`)
- **Other Supported Aspect Ratios:**
  - `4:3` (2048 x 1536)
  - `1:1` (1080 x 1080)
  - `9:16` (1080 x 1920)

---

## 3. Technology Stack & Dependencies

| Layer | Technology / Library |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 |
| **UI Framework** | Jetpack Compose + Material 3 (BOM 2024.09.00) |
| **Architecture** | MVVM (Model - View - ViewModel) + StateFlow UDF |
| **Navigation** | AndroidX Navigation Compose (`2.8.9`) |
| **AI Integration** | OkHttp 4.10 direct REST to Google Gemini 3.8/3.7, Imagen 3, and NVIDIA NIM (FLUX.1 Dev, LLaMA 3.2 Vision) |
| **Authentication** | Firebase Authentication (Google Sign-In, Email/Password, Guest Mode) + Cloud Firestore |
| **Local Database** | Room (`2.7.0`) with KSP (`2.3.5`) |
| **Security** | `androidx.security:security-crypto:1.0.0` (`EncryptedSharedPreferences` with AES256-GCM / AES256-SIV) |
| **Image Loading** | Coil Compose (`2.7.0`) |
| **Build Tooling** | Android Gradle Plugin `8.x/9.x`, Secrets Gradle Plugin |

---

## 4. Architecture & Directory Structure

```
bannerflow-navigator/
├── .env.example                               # Template for API keys
├── .gitignore                                 # Git ignore including .env, build artifacts
├── build.gradle.kts                           # Root Gradle build config
├── gradle/
│   ├── libs.versions.toml                     # Version catalog
│   └── wrapper/
├── README.md                                  # Setup & run documentation
├── ARCHITECTURE.md                            # Comprehensive architectural specs
├── CONTRIBUTING.md                            # Development guidelines
├── GEMINI.md                                  # Agent & developer guidelines
├── MEMORY.md                                  # Project memory (this document)
└── app/
    ├── build.gradle.kts                       # App module Gradle configuration
    ├── google-services.json                   # Firebase client configuration
    ├── proguard-rules.pro                     # ProGuard / R8 rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/
        │   │   ├── MainActivity.kt            # Single Activity host with edge-to-edge
        │   │   ├── data/                      # Data models, DB, and service layer
        │   │   │   ├── AiModelConfig.kt       # Model endpoints & defaults
        │   │   │   ├── ApiProviderConfig.kt   # Multi-provider presets & model configs
        │   │   │   ├── BannerDatabase.kt      # Room DB instance
        │   │   │   ├── BannerHistoryDao.kt    # DAO for banner history
        │   │   │   ├── BannerHistoryItem.kt   # Room Entity for saved banners
        │   │   │   ├── BannerHistoryRepository.kt
        │   │   │   ├── DefaultAvatarHelper.kt # Sample avatars / placeholders
        │   │   │   ├── GeminiChatService.kt   # Gemini 3.8 Flash multi-turn chat client
        │   │   │   ├── GeminiImageService.kt  # Gemini / Imagen 3 image REST Client
        │   │   │   ├── ImageProcessingService.kt # Safe area scaling & bitmap composition
        │   │   │   ├── LogoOverlayService.kt  # Avatar/logo overlay placement
        │   │   │   ├── OpenAiCompatibleService.kt # NVIDIA NIM (FLUX.1 Dev & LLaMA) client
        │   │   │   ├── SaveService.kt         # MediaStore bitmap export
        │   │   │   ├── SecureStorageManager.kt# EncryptedSharedPreferences for API keys
        │   │   │   ├── TargetDimensions.kt    # Canvas dimensions & Safe area bounds
        │   │   │   └── TextOverlayService.kt  # Typography drawing onto banners
        │   │   ├── navigation/
        │   │   │   └── NavGraph.kt            # Compose Navigation routes & transitions
        │   │   ├── state/                     # ViewModels & UI States
        │   │   │   ├── ApiKeyViewModel.kt     # Multi-provider profile & decoupled key state
        │   │   │   ├── AppThemeViewModel.kt   # Dark/Light mode state management
        │   │   │   ├── AuthViewModel.kt       # Firebase authentication state
        │   │   │   ├── BannerCreationViewModel.kt # Generation wizard pipeline
        │   │   │   ├── HistoryViewModel.kt    # Saved banner collection
        │   │   │   ├── ImageEditorViewModel.kt# AI Studio image editing state
        │   │   │   └── PromptChatViewModel.kt # AI Prompt Chat brainstorming state
        │   │   └── ui/
        │   │       ├── components/
        │   │       │   └── UiComponents.kt    # Neumorphic buttons, cards, headers
        │   │       ├── screens/
        │   │       │   ├── ApiKeySetupScreen.kt # Decoupled Chat & Image key configuration
        │   │       │   ├── GenerationSettingsScreen.kt # Aspect ratio, model picker, overlays
        │   │       │   ├── HistoryScreen.kt   # Saved banners gallery & viewer
        │   │       │   ├── HomeScreen.kt      # Dashboard & navigation hub
        │   │       │   ├── ImageEditorScreen.kt # Image Studio prompt-based editor
        │   │       │   ├── LoginScreen.kt     # Firebase Auth login/signup screen
        │   │       │   ├── PromptBuilderScreen.kt # Mood, topic, and style wizard
        │   │       │   ├── PromptChatScreen.kt# AI prompt brainstorm chat with model switcher
        │   │       │   ├── ResultScreen.kt    # High-res banner view & export
        │   │       │   ├── SettingsScreen.kt  # App settings & profile manager
        │   │       │   └── SplashScreen.kt    # Splash launcher & auth routing
        │   │       └── theme/
        │   │           ├── Color.kt
        │   │           ├── Neumorphic.kt      # Neumorphic convex/inset modifiers
        │   │           ├── Theme.kt           # BannerCraftTheme setup
        │   │           └── Type.kt            # Typography styles
        └── test/                              # Unit & Robolectric test suites
```

---

## 5. Core User Flows & Workflows

1. **Onboarding & Splash:**
   - App checks for saved Gemini API key via [`SecureStorageManager`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/SecureStorageManager.kt).
   - If not set or invalid, redirects to `ApiKeySetupScreen`; otherwise lands on `HomeScreen`.

2. **Banner Creation Pipeline:**
   - **Step 1: Prompt Builder (`PromptBuilderScreen`):** Select channel topic/niche (Gaming, Tech, Vlog, Education, etc.), mood (Energetic, Cyberpunk, Minimalist, etc.), curated palette, and custom description.
   - **Step 2: Generation Settings (`GenerationSettingsScreen`):** Configure aspect ratio, style (3D Render, Anime, Vector, Realistic), overlay text (channel name, tagline), text positioning, and avatar/logo overlay.
   - **Step 3: AI Generation (`ResultScreen`):**
     - Requests `GeminiImageService.generateImage()` using `gemini-3.1-flash-image` with `responseModalities: ["IMAGE"]`.
     - Raw AI image is passed through [`ImageProcessingService`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/data/ImageProcessingService.kt) to scale into `2560x1440` and composite text/logo overlays strictly within YouTube safe zones.
     - Saves finished output into Room DB (`banner_history`) and provides instant MediaStore gallery export.

3. **Image Studio (`ImageEditorScreen`):**
   - Allows users to input any existing image and apply AI edit prompts (via `GeminiImageService.editImage()`) to refine styles, adjust lighting, or modify elements.

4. **History & Gallery (`HistoryScreen`):**
   - Displays previously generated banners saved locally in Room, allowing instant inspection, re-editing, or re-exporting.

---

## 6. Important Implementation Details & Gotchas

1. **Gemini API Key Format:**
   - Validated via [`ApiKeyViewModel.validateApiKey()`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/src/main/java/com/example/state/ApiKeyViewModel.kt): must start with `AIza` and have minimum 15 characters.
   - Stored securely in `EncryptedSharedPreferences`. Fallback occurs automatically to normal private prefs if Android Keystore fails.

2. **Gradle & Secrets:**
   - Uses `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` to read from `.env` and `.env.example`.
   - For local builds, remove or configure `signingConfig = signingConfigs.getByName("debugConfig")` in [`app/build.gradle.kts`](file:///c:/Users/shuai/Downloads/bannerflow-navigator%20%281%29/app/build.gradle.kts) if running without the bundled debug keystore.

3. **Neumorphic System Conventions:**
   - Use `Modifier.neumorphicConvex()` for elevated surfaces (cards, buttons, action tiles).
   - Use `Modifier.neumorphicInset()` for sunken surfaces (search inputs, text boxes, active slots).
   - Use `Modifier.tactilePressable()` for interactive touch feedback with spring scale animations.
