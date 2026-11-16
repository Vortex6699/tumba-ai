# TUMBA AI

A private, local-first AI assistant for Android. TUMBA brings cloud models (Gemini,
DeepSeek and any OpenAI-compatible endpoint) and on-device GGUF model management into one
quiet, monochrome interface. Conversations, attachments and API keys never leave the
device except in the request you explicitly send to the provider you selected.

- Package: `com.tumba.ai`
- Version: 1.2.0 (versionCode 5)
- Min SDK: 26 · Target/Compile SDK: 35
- Kotlin 2.0.21 · Jetpack Compose (Material 3) · Hilt · Room · DataStore · OkHttp/Retrofit
- On-device inference: llama.cpp via JNI (`app/src/main/cpp`)
- Product name: **TUMBA AI**

---

## 1. What is implemented

### Chat
- Streaming responses (SSE) for Gemini, DeepSeek and OpenAI-compatible endpoints.
- Markdown rendering with headings, lists, quotes, inline formatting and fenced code
  blocks with a copy button.
- Copy any answer, regenerate the last answer, stop generation immediately.
- Animated thinking indicator and token-level persistence while streaming.
- Per-conversation title, model and provider.
- Image attachments for vision-capable models, with full-screen preview.
- AI errors are mapped to localizable states (missing/invalid key, rate limit, timeout,
  network, context length, content filter, server error) instead of raw JSON.

### History
- Left-side drawer opened from the menu button in the chat header (swipe also works).
- Local Room-backed conversation list with search across titles and message bodies.
- Rename, delete, pin/unpin and date grouping (Pinned / Today / Yesterday / Previous 7 days / Older).
- History survives restarts and process death.

### Models
- Reached from Settings → Models; nothing model-related sits on the chat screen.
- Cloud and on-device sections.
- Built-in model catalogue with context window and vision metadata.
- One active model used by Chat; the active model name is shown under the conversation title.
- API key management (write-only in the UI — keys are never displayed back).
- Connection tests with latency, per model/provider.
- Live model discovery for Gemini (`models.list`). The model list is refreshed from the key,
  so retired generations never leave the app stuck on a 404.
- Custom OpenAI-compatible endpoints: name, base URL, model id, optional key, vision
  capability and context window.
- Local GGUF import (SAF), size/quantization display, selection and deletion.

### Settings
- Entry point for everything model-related: Models and Local models link cards.
- Theme: System / Light / Dark (dark palette is the product identity).
- Interface language: System / English / Українська (AppCompat per-app locales).
- System prompt, conversation context length, creativity (temperature), streaming toggle.
- API keys for Gemini and DeepSeek.
- Data controls: JSON export, delete all conversations, delete all API keys.
- App information (version, package, build type) and privacy notes.

### Security
- API keys are encrypted with AES-256/GCM using a non-exportable Android Keystore key
  (`KeystoreSecretStore`). Only ciphertext + per-write IV are stored in private prefs.
- Key material is never logged. The debug HTTP interceptor logs only method, host and path.
- Keys are attached only to requests sent to the provider they belong to; the key is not
  included in URLs where a header is supported.
- Backup rules exclude the secret store, imported models and attachments.
- Deleting conversations also deletes their attachment files.

### Local AI
- Curated download catalogue (Qwen2.5 0.5B/1.5B/3B, Llama 3.2 1B) with resumable,
  progress-reporting downloads, free-space checks and GGUF validation.
- SAF import for any GGUF file you already have, with quantization detection.
- Real on-device inference through a bundled llama.cpp engine (`libtumba_llama.so`):
  the model's own chat template is applied, sampling uses top-k/top-p/temperature,
  tokens stream into the same UI and Stop cancels within one token.
- The model is loaded once and released on `onTrimMemory`/`onLowMemory`.
- The native library is built from `app/src/main/cpp` by `tools/build-native.*`.
  If it is missing for the device ABI, the app says so explicitly instead of faking output.
- No weights are ever bundled in the APK.

---

## 2. Architecture

Clean-ish, single-module architecture that keeps Android/framework types out of the core:

```
ui/            Compose screens, ViewModels, theme, markdown renderer
domain/        Pure Kotlin models, repository interfaces, AIProvider + AIRepository
data/
  ai/          AirHttpClient (SSE), providers (Gemini/DeepSeek/OpenAI-compatible/Local),
               ProviderRegistry, AIRepositoryImpl, ModelCatalog, error mapping
  local/       Room database, entities, DAOs, converters, migrations
  repository/  Repository implementations (Room, DataStore, encrypted key store)
  security/    KeystoreSecretStore
  files/       AttachmentStore
  network/     Retrofit discovery API
di/            Hilt modules and bindings
```

Data flow:

```
Compose Screen
   ↕ UiState / events
ViewModel  (Chat / History / Models / Settings)
   ↕ suspend + Flow
Repository interfaces
   ├── Room (conversations, messages, attachments, custom endpoints, local models)
   ├── DataStore (settings + active model)
   └── AIRepository
          └── AIProvider
                ├── GeminiProvider
                ├── DeepSeekProvider  ── delegates to ── OpenAICompatibleProvider
                ├── OpenAICompatibleProvider (any /chat/completions)
                └── LocalProvider ── LlamaCppEngine (llama.cpp via JNI)
```

`ChatViewModel` depends only on `AIRepository`, never on Gemini/DeepSeek directly.

### Streaming
`AiHttpClient` executes OkHttp calls on `Dispatchers.IO`, parses SSE with the unit-tested
`SseAccumulator`, cancels the HTTP call when the coroutine is cancelled, and retries only if
the stream failed before emitting anything. This makes “Stop” immediate without duplicating
text.

### Context
The repository/ViewModel sends the last N messages (configurable) with a system prompt,
drops a leading orphaned assistant message, merges adjacent same-role messages, and attaches
images only from the most recent user message. Attachments are rejected when the active
model has no vision capability.

---

## 3. Build

### One-command install
Windows (double-click `install.bat`, or from a terminal):

```powershell
powershell -ExecutionPolicy Bypass -File install.ps1            # build + install over USB
powershell -ExecutionPolicy Bypass -File install.ps1 -LocalAi   # also build the native engine
```

macOS / Linux:

```bash
chmod +x install.sh tools/build-native.sh   # once, if the archive lost the executable bit
./install.sh                # build + install over USB
./install.sh --local-ai     # also build the native engine
./install.sh --skip-local-ai  # cloud-only APK
```

The scripts find or install a JDK 17+, the Android SDK (platform 35, build-tools 35.0.0,
platform-tools), write `local.properties`, run the Gradle wrapper, wait for a USB device,
install the APK with `adb install -r -d` and launch the app. Re-running is safe; existing
tooling is reused. The native llama.cpp engine ships prebuilt in
`app/src/main/jniLibs/`, so a normal build packages it without an NDK. The installer only
rebuilds it when the libraries are missing and an NDK is already installed; pass
`-LocalAi`/`--local-ai` to install the NDK and make that rebuild mandatory.

### Requirements
- Android Studio Ladybug or newer (JDK 17+; the project builds with JDK 17 or 21).
- Android SDK Platform 35, Build Tools 35.0.0.
- NDK 27.0.12077973 + CMake 3.22.1 only for rebuilding the native engine.
- No API keys are required to compile.

### Android Studio
1. Open the `tumba-ai` folder.
2. Let Gradle sync (AGP 8.7.3, Gradle 8.11.1).
3. Run `app` on an API 26+ device or emulator.

### Command line
```bash
export JAVA_HOME=/path/to/jdk21
./gradlew assembleDebug
# macOS/Linux output: app/build/outputs/apk/debug/app-debug.apk
# Windows: .\gradlew.bat assembleDebug
```

### Native engine (local inference)
The engine is already built and committed for `arm64-v8a` and `x86_64`; rebuild it only after
changing `app/src/main/cpp` or updating llama.cpp:

```powershell
# Windows: installs nothing, uses the NDK already present in the SDK
powershell -ExecutionPolicy Bypass -File tools\build-native.ps1
# install NDK/CMake first if needed:
sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"
```

```bash
./tools/build-native.sh
```

The script fetches the pinned llama.cpp release (`v0.9.0`) into
`app/src/main/cpp/llama.cpp` (git-ignored), builds `libtumba_llama.so` for `arm64-v8a` and
`x86_64`, strips it and copies it into `app/src/main/jniLibs/`. Gradle packages the library
on the next build; Gradle itself never needs the NDK. Details and the JNI contract are in
[`app/src/main/cpp/README.md`](app/src/main/cpp/README.md).

Release builds are intentionally unsigned:
```bash
./gradlew assembleRelease
```
Add your own signing config in `app/build.gradle.kts` or use Android Studio's
“Generate Signed Bundle / APK”.

### Tests
```bash
./gradlew testDebugUnitTest
```
35 JVM tests cover the Markdown parser, SSE accumulator, provider JSON/error parsing,
message merging, model catalogue invariants, the download catalogue, Gemini model-list
parsing and retired-model normalisation, plus an end-to-end OpenAI-compatible streaming test
against MockWebServer (auth header, SSE deltas, usage, image payload, vision rejection and
401 mapping).

### Included verified artifact
`dist/tumba-ai-1.2.0.apk` is a debug-signed, installable build of this exact source tree
(package `com.tumba.ai`, version `1.2.0`, versionCode 5, min SDK 26, target SDK 35). It
**contains the native llama.cpp engine** for `arm64-v8a` (`libtumba_llama.so`, 6.6 MB) and
`x86_64` (7.2 MB), so local models work without running `tools/build-native.*` first. The
shipped library is already stripped; Gradle packages it unchanged. See
[BUILD_REPORT.md](BUILD_REPORT.md) for the environment, commands and SHA-256.

If you rebuild from a clean checkout where `app/src/main/jniLibs/*/libtumba_llama.so` is
absent, run `tools/build-native.*` (or `install.ps1 -LocalAi`) once and rebuild; the APK then
contains the engine again.

---

## 4. Provider setup

### Gemini (recommended default)
1. Create an API key in Google AI Studio.
2. Settings → Models → Gemini → **Set key** (or Settings → API keys).
3. Pick a model, optionally tap **Test connection**.
4. Chat.

The default model is the stable alias **`gemini-flash-latest`**. Gemini retires dated
generations (for example, Gemini 2.0 Flash was shut down on 2026-06-01), so TUMBA also
offers a **sync** button in the Gemini card that loads the models currently available to your
key. The selection is stored by ID; a previously selected retired model is transparently
mapped to `gemini-flash-latest` on upgrade.

Requests use `x-goog-api-key` (not a query parameter) and the native
`v1beta/models/{model}:streamGenerateContent?alt=sse` endpoint.

**If you see `404`:** this means the model ID is unavailable for your key, not that the key
is invalid. Open **Settings → Models → Gemini**, tap **Sync models**, and choose a model from the
refreshed list (or select **Gemini 2.5 Flash** on older builds). `401/403` would indicate a
key/permission problem instead.

### DeepSeek
1. Create a key at platform.deepseek.com.
2. Settings → Models → DeepSeek → **Set key**.
3. Models: `deepseek-chat`, `deepseek-reasoner` (text-only).

### Custom / OpenAI-compatible
1. Settings → Models → **Add custom API**.
2. Base URL (for example `https://openrouter.ai/api/v1`), model id, optional key,
   whether the endpoint accepts images, optional context window.
3. Save, test, select the model.

The key is optional for custom endpoints to support local gateways that do not require
authentication.

---

## 5. Database

Room schema (version 2):

- `conversations(id, title, createdAt, updatedAt, providerId, modelId, pinned)`
- `messages(id, conversationId → conversations.id CASCADE, content, role, timestamp, status, error)`
  - index on `(conversationId, timestamp)`
- `attachments(id, messageId → messages.id CASCADE, path, mimeType, name, sizeBytes)`
  - index on `messageId`
- `custom_providers(id, name, baseUrl, modelId, supportsImages, contextWindow, createdAt)`
- `local_models(id, displayName, fileName, path, sizeBytes, importedAt, quantization)`

Version 1 → 2 adds `conversations.pinned` with an additive `ALTER TABLE` migration in
`DatabaseMigrations`. Schemas are exported to `app/schemas/`; destructive migration is not
enabled.

---

## 6. Privacy model

- Conversations, settings and attachments live in app-private storage.
- Network traffic goes only to the selected provider (Gemini/DeepSeek/custom endpoint), using
  the user's own key.
- No analytics, ads, crash reporters or third-party SDKs beyond the model HTTP calls.
- The system prompt and recent conversation context are sent to the model — that is the
  explicit purpose of a chat request.
- Export is user-initiated and writes a JSON file to a location chosen via SAF.

---

## 7. Known limitations

| Area | Status |
| --- | --- |
| Local llama.cpp inference | Implemented with a real JNI bridge; `libtumba_llama.so` for `arm64-v8a` and `x86_64` is packaged in the APK. If a build is made without it (clean checkout), the engine reports the exact reason instead of fabricating output. |
| Local engine on 32-bit devices | Not built (`abiFilters` = 64-bit only). The app installs and cloud models work; local inference reports "not in this APK (device ABI: armeabi-v7a, ...)". |
| Device runtime | Compiled and linted, but not launched on a device/emulator in this environment. UI runtime is therefore unverified. |
| Live provider calls | No API keys were available; Gemini/DeepSeek/custom HTTP+SSE is mock-tested, not exercised against the real services. |
| Live model downloads | The four catalogue URLs and sizes were verified with HTTP HEAD; a full 0.5–2 GB transfer was not executed here. |
| Instrumented UI tests | Not included/run in this environment (no emulator). |
| Migration runtime | Room schemas v1/v2 exported and the additive migration is implemented, but it was not executed against a real v1 database here. |
| Release signing | Unsigned by default; configure your own keystore. |
| Streaming token counts | Captured when the provider returns usage; not displayed yet. |
| Markdown | Deliberate subset; no tables, images, nested emphasis or clickable links. |
| Search | SQLite `LIKE`; Cyrillic case-insensitive matching depends on SQLite's ASCII-only folding. |

---

## 8. Project layout

```
tumba-ai/
├── app/
│   ├── schemas/                  Room exported schemas (v1, v2)
│   ├── src/main/java/com/tumba/ai/
│   │   ├── core/                 ids, formatting, dispatchers, errors
│   │   ├── data/                 ai, local, repository, security, files, network
│   │   ├── di/                   Hilt modules
│   │   ├── domain/               models, repositories, AI contracts
│   │   └── ui/                   chat, history, models, settings, theme, markdown
│   ├── src/main/cpp/             JNI bridge + llama.cpp build notes
│   ├── src/main/jniLibs/         libtumba_llama.so per ABI (built by tools/)
│   ├── src/main/res/             English + Ukrainian strings, icons, themes
│   └── src/test/                 JVM unit tests
├── tools/                        build-native.ps1 / build-native.sh
├── dist/                         Verified installable debug APK
├── gradle/                       Wrapper (Gradle 8.11.1)
├── install.ps1 / install.bat     One-command Windows setup + install
├── install.sh                    One-command macOS/Linux setup + install
├── BUILD_REPORT.md               Build, test and limitation report
├── README.md
└── build.gradle.kts
```
