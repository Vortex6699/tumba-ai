# TUMBA AI

**Your AI. Your device. Your control.**

<<<<<<< Updated upstream
TUMBA AI — персональний AI-асистент для Android, який об'єднує хмарні нейромережі та локальні AI-моделі в одному мінімалістичному застосунку.
=======
- Package: `com.tumba.ai`
- Version: 1.2.1 (versionCode 6)
- Min SDK: 26 · Target/Compile SDK: 35
- Kotlin 2.0.21 · Jetpack Compose (Material 3) · Hilt · Room · DataStore · OkHttp/Retrofit
- On-device inference: llama.cpp via JNI (`app/src/main/cpp`)
- Product name: **TUMBA AI**
>>>>>>> Stashed changes

Використовуй Gemini, DeepSeek або власні OpenAI-compatible API. Або запускай GGUF-моделі безпосередньо на своєму пристрої.

## ✨ Можливості

- 💬 **AI-чат** — потокові відповіді, Markdown, копіювання та повторна генерація.
- 🧠 **Кілька AI-провайдерів** — Gemini, DeepSeek та власні API.
- 📱 **Локальний AI** — запуск GGUF-моделей безпосередньо на Android через llama.cpp.
- 🖼️ **Розуміння зображень** — надсилання фотографій моделям із підтримкою Vision.
- 📚 **Історія чатів** — пошук, перейменування, закріплення та видалення розмов.
- ⚙️ **Керування моделями** — вибір, додавання власних API, імпорт і завантаження GGUF.
- 🎛️ **Налаштування AI** — системний промпт, температура, контекст розмови та потокова генерація.
- 🌍 **Мультимовність** — українська та англійська мови.
- 🎨 **Персоналізація** — світла, темна та системна теми.

## 🔒 Приватність і безпека

TUMBA AI створений із пріоритетом на приватність користувача.

- API-ключі шифруються за допомогою AES-256/GCM та Android Keystore.
- Історія розмов, налаштування та вкладення зберігаються локально.
- Немає реклами, аналітики та телеметрії.
- Локальні моделі працюють без надсилання запитів у хмару.
- Хмарні запити надсилаються лише обраному провайдеру з використанням твого API-ключа.

**Твої дані залишаються під твоїм контролем.**

## 📥 Встановлення APK

Хочеш спробувати TUMBA AI на своєму Android-пристрої?

1. Перейди до розділу **Releases** цього репозиторію.
2. Завантаж останню версію APK.
3. Відкрий завантажений файл на Android.
4. Підтвердь встановлення, якщо система попросить дозвіл.

[⬇️ Завантажити останню версію](../../releases/latest)

**Поточна версія:** 1.2.0  
**Package:** `com.tumba.ai`  
**Мінімальна версія Android:** 8.0 (API 26)

## 🤖 Підтримувані AI-моделі

### Хмарні провайдери

- Google Gemini
- DeepSeek
- Власні OpenAI-compatible API
- OpenRouter та сумісні API-шлюзи

### Локальні моделі

Підтримка GGUF через llama.cpp.

У каталозі доступні:
- Qwen 2.5 0.5B
- Qwen 2.5 1.5B
- Qwen 2.5 3B
- Llama 3.2 1B

Також можна імпортувати власні GGUF-файли.

## 🛠️ Технології

- Kotlin 2.0.21
- Jetpack Compose
- Material 3
- MVVM Architecture
- Hilt
- Room Database
- DataStore
- OkHttp / Retrofit
- llama.cpp
- JNI / C++

## 🚀 Запуск проєкту

**Необхідно:**
- Android Studio Ladybug або новіша.
- JDK 17 або 21.
- Android SDK 35.

**Інструкція:**

1. Клонуй репозиторій.
2. Відкрий папку `tumba-ai` в Android Studio.
3. Дочекайся завершення Gradle Sync.
4. Запусти застосунок на Android-пристрої або емуляторі.

Для компіляції API-ключі не потрібні.

### Збірка APK

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK буде створений у:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Для локального AI у збірці має бути присутня відповідна нативна бібліотека llama.cpp.

## 📂 Архітектура

<<<<<<< Updated upstream
```text
=======
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
`dist/tumba-ai-1.2.1.apk` is a debug-signed, installable build of this exact source tree
(package `com.tumba.ai`, version `1.2.1`, versionCode 6, min SDK 26, target SDK 35). It
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
>>>>>>> Stashed changes
tumba-ai/
├── app/
│   ├── schemas/
│   └── src/main/
│       ├── java/com/tumba/ai/
│       │   ├── core/
│       │   ├── data/
│       │   ├── di/
│       │   ├── domain/
│       │   └── ui/
│       ├── cpp/
│       ├── jniLibs/
│       └── res/
├── tools/
├── dist/
├── gradle/
├── install.ps1
├── install.bat
├── install.sh
├── BUILD_REPORT.md
└── README.md
```

## ⚠️ Важливо

- Для хмарних моделей потрібен інтернет та власний API-ключ.
- Для локальних моделей потрібне завантаження або імпорт GGUF-файлу.
- Локальний рушій зібраний для `arm64-v8a` та `x86_64`.
- APK містить нативний рушій, але не містить ваг AI-моделей.
- Релізні збірки потребують власного підпису.

## 🖤 Про TUMBA

TUMBA — незалежний проєкт, мета якого створювати корисні цифрові інструменти без зайвих обмежень.

Ми віримо, що хороші програми мають бути зручними, приватними та доступними.

**TUMBA — створюємо те, чого нам не вистачало.**

*Private. Powerful. Yours.*
