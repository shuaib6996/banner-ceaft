# Contributing to BannerCraft

Thank you for contributing to BannerCraft! Here are the guidelines and instructions to get started.

---

## 1. Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or Ladybug (2024.2.1+) / Meerkat or newer.
- **JDK**: Java 11 or Java 17+.
- **Android SDK**: Android 14+ / SDK 36 platform installed.
- **Gemini API Key**: Obtainable from [Google AI Studio](https://aistudio.google.com/).

---

## 2. Local Environment Setup

1. **Clone or Open the Repository:**
   Open Android Studio and select **Open**, navigating to the project directory.

2. **Configure API Key:**
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add your Gemini API key in `.env`:
   ```properties
   GEMINI_API_KEY=AIzaSyYourActualApiKeyHere
   ```

3. **Configure Keystore for Debug Builds:**
   If you do not have the custom `debug.keystore` referenced in `app/build.gradle.kts`, comment out line 48 in `app/build.gradle.kts`:
   ```kotlin
   // debug { signingConfig = signingConfigs.getByName("debugConfig") }
   ```
   This will allow Android Studio to use your machine's default Android debug key.

4. **Sync & Run:**
   - Click **Sync Project with Gradle Files**.
   - Select an emulator (e.g. Pixel 8 running Android 14/15) or physical device.
   - Run the `app` target.

---

## 3. Running Tests

- **Unit Tests:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Robolectric & ViewModel Tests:**
  Located in `app/src/test/java/com/example/`.
- **Android Instrumentation Tests:**
  ```bash
  ./gradlew connectedAndroidTest
  ```
