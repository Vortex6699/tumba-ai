# TUMBA AI вЂ” Build & Verification Report

**Date:** 2026-11-13 (Europe/Kiev)
**Project:** `tumba-ai/`
**Package:** `com.tumba.ai` В· **Version:** 1.2.0 (versionCode 5)

## Environment

| Item | Value |
| --- | --- |
| OS | Windows 11 amd64 (sandboxed workspace) |
| JDK | Microsoft OpenJDK 17.0.21.0.2 LTS (build) В· Temurin 21 used by the portable installer |
| Gradle | 8.11.1 (wrapper included and verified) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 (Compose compiler plugin, kotlinx.serialization) |
| KSP / Hilt / Room | 2.0.21-1.0.28 / 2.52 / 2.6.1 |
| Android SDK | Platform 35, Build Tools 35.0.0, platform-tools |
| NDK / CMake | 27.0.12077973 / 3.22.1 (installed for the native engine) |
| SDK levels | min 26 В· target 35 В· compile 35 |
| ABIs | `arm64-v8a`, `x86_64` (`abiFilters`) |

The environment had no preinstalled JDK or Android SDK; both were bootstrapped inside the
workspace. The delivered project uses standard `google()` / `mavenCentral()` repositories and
the checked-in Gradle wrapper.

## Commands run

```bash
gradlew.bat testDebugUnitTest assembleDebug assembleRelease lintDebug --console=plain
powershell -ExecutionPolicy Bypass -File install.ps1 -SkipBuild
powershell -ExecutionPolicy Bypass -File install.ps1        # full path: JDK/SDK detect + build + adb
```

Two sandbox-specific settings remain in `gradle.properties` (`org.gradle.native=false`,
HTTP timeouts) because the agent sandbox blocks Gradle's native file watcher and some long
connections. They do not change app behaviour.

## Results

| Check | Result |
| --- | --- |
| `assembleDebug` | **SUCCESS** |
| `assembleRelease` | **SUCCESS** (unsigned, R8/minify disabled) |
| `testDebugUnitTest` | **35/35 passed**, 0 failures (7 suites) |
| `lintDebug` | **SUCCESS**, 0 errors (72 warnings, see below) |
| Android resources / manifest | Linked successfully; EN + UK parity kept |
| Room schema export | `schemas/.../1.json` and `2.json` present |
| One-command installers | `install.ps1` verified end-to-end up to the device step |
| APK signature | Debug certificate, installable with `adb install -r -d` |
| Native engine in APK | `libtumba_llama.so` present for `arm64-v8a` and `x86_64` |

### Unit tests (35)

| Suite | Tests |
| --- | --- |
| `MarkdownParserTest` | 6 |
| `SseAccumulatorTest` | 5 |
| `ProviderParsingTest` (incl. 404 в†’ model-not-found) | 6 |
| `OpenAICompatibleProviderTest` (MockWebServer, real OkHttp + SSE) | 4 |
| `ModelCatalogTest` (incl. retired-model normalisation) | 5 |
| `GeminiModelParserTest` (models.list filtering/metadata) | 4 |
| `LocalModelCatalogTest` (download catalogue invariants) | 5 |

The provider test exercises: request path (`/v1/chat/completions`), `Bearer` auth header,
streamed deltas, usage parsing, image data-URL payload, vision-capability rejection and 401 в†’
invalid-key mapping. Gemini discovery parsing is covered separately with synthetic
`models.list` payloads. `LocalModelCatalogTest` guards the curated download list (unique IDs,
HTTPS `.gguf` URLs, plausible sizes, lookup by ID).

## What changed in 1.2.0

Design-only release. No changes to the AI engine, providers, storage, crypto or networking вЂ”
only Compose UI, navigation and strings.

1. **Chat is now the whole screen.** The bottom navigation bar is gone. The chat header holds
   only: menu button (opens history), centred conversation title with the active model name
   underneath (tap opens Settings), new-chat button, and an overflow menu with rename/delete.
   The wordmark chip and the model chip were removed from the header.
2. **History moved into a left drawer.** `ConversationDrawer` replaces the old History screen
   (`HistoryScreen.kt` deleted, `HistoryViewModel` reused unchanged): new-chat button, search
   field, pinned/today/yesterday/previous-7-days/older groups, per-row pin, rename and delete,
   selected-row highlight, and a Settings link in the footer. Swipe-to-open works on the chat
   route; system back closes the drawer.
3. **Everything model-related moved into Settings.** Settings now starts with two link cards вЂ”
   **Models** (cloud providers, API keys, custom endpoints, local models) and **Local models** вЂ”
   and both screens use a back arrow instead of a Settings gear. The chat empty state's setup
   cards route to Settings.
4. **Cleaner messages.** Removed the per-message avatars, the "TUMBA" label, timestamps and the
   user-message copy row; assistant copy/regenerate actions stay.
5. **Empty state** is centred: wordmark, headline, subtitle and suggestion chips in a FlowRow.
6. **Version bumped to 1.2.0 / versionCode 5** (debug signing certificate unchanged from 1.1.0,
   so `adb install -r` upgrades in place).

EN/UK string parity is 276/276; `lintDebug` reports 0 errors (72 warnings, all pre-existing
style/dependency suggestions).

## What changed in 1.1.1

### Reported problem

> "I downloaded the local engine, selected it, and the answer is the error *local engine
> unavailable*."

Cause: `dist/tumba-ai-1.1.0.apk` was built **without** `lib/arm64-v8a/libtumba_llama.so`.
`System.loadLibrary("tumba_llama")` therefore threw `UnsatisfiedLinkError`, the engine
reported `LocalEngineStatus.Unavailable`, and the UI showed the generic message with no way
to tell "library missing" from "device ABI unsupported".

### Fix

1. **The library is now actually built and packaged.** `tools/build-native` was run for both
   supported ABIs and the stripped output was placed in `app/src/main/jniLibs/`:
   - `arm64-v8a/libtumba_llama.so` вЂ” 6,600,224 bytes, ELF64 AArch64, `DYN`
   - `x86_64/libtumba_llama.so` вЂ” 7,247,624 bytes, ELF64 x86-64, `DYN`

   `llvm-readelf` shows only platform dependencies (`liblog`, `libandroid`, `libm`, `libdl`,
   `libc`) and `llvm-nm -D` lists `JNI_OnLoad` plus all
   `Java_com_tumba_ai_data_ai_local_LlamaCppEngine_*` entry points, so the library is
   loadable by this exact Kotlin package/class.
2. **Gradle keeps the prebuilt untouched.** `packaging.jniLibs.keepDebugSymbols` includes
   `**/libtumba_llama.so`, so AGP's `stripDebugDebugSymbols` does not rewrite (or damage) the
   already-stripped library and assembling never needs an NDK.
3. **The error is now self-diagnosing.** `AiError.LocalEngineUnavailable` became
   `data class LocalEngineUnavailable(detail: String?)`; `LlamaCppEngine.detect()` reports
   - the device ABIs (`Build.SUPPORTED_ABIS`),
   - which ABI folders inside the installed APK actually carry `libtumba_llama.so` (read
    from the APK zip, because the manifest has `extractNativeLibs=false` and
    `nativeLibraryDir` stays empty),
   - the `UnsatisfiedLinkError` message.

   The new string `error_local_engine_detail` ("On-device engine is unavailable: %1$s" /
   "Р›РѕРєР°Р»СЊРЅРёР№ СЂСѓС€С–Р№ РЅРµРґРѕСЃС‚СѓРїРЅРёР№: %1$s") is used by the chat banner, the connection test and
   the models screen, in both locales.
4. **Version bumped to 1.1.1 / versionCode 4** so the fix installs over 1.1.0 with
   `adb install -r`.

### Verifying the packaged APK

```text
lib/arm64-v8a/libtumba_llama.so   6,600,224
lib/x86_64/libtumba_llama.so      7,247,624
aapt2 dump badging: versionCode='4' versionName='1.1.1'
```

## What changed in 1.1.0

### Local AI (real llama.cpp engine)

- New JNI bridge `app/src/main/cpp/tumba_llama.cpp`:
  - loads the GGUF model once and keeps it mapped (`LLAMA_LOAD_MODE_MMAP`);
  - renders the prompt with the model's own chat template (`llama_chat_apply_template`),
    falling back to ChatML only when the model ships none;
  - samples with a top-k / top-p / temperature / repeat-penalty chain, or greedy when
    `temperature <= 0`;
  - streams UTF-8-safe token pieces through a `TokenCallback` and polls `isCancelled()`
    between tokens, so Stop returns within one token;
  - truncates over-long prompts from the oldest tokens and logs it;
  - releases everything through RAII guards on every exit path.
- `LlamaCppEngine` (Kotlin) keeps the model handle across requests, serializes generation,
  exposes `unload()` and reports `LocalEngineStatus.Unavailable` when the shared library is
  missing вЂ” no fabricated output in any path.
- `TumbaApplication` unloads the model on `onTrimMemory(RUNNING_LOW)` / `onLowMemory()`.
- `LocalProvider` now forwards the structured conversation and rejects image attachments with
  a localized error.
- `CMakeLists.txt` builds `libtumba_llama.so` from a pinned llama.cpp v0.9.0 checkout with
  tests/examples/server/tools/common disabled and OpenMP/Vulkan/CUDA/Metal off.

### Model downloads

- Curated catalogue (`LocalModelCatalog`): Qwen2.5 0.5B / 1.5B / 3B Instruct and
  Llama 3.2 1B Instruct, Q4_K_M, with verified sizes.
- Resumable downloader (`LocalModelRepositoryImpl.downloadModel`): HTTP `Range` resume from a
  `.part` file, automatic restart when the server ignores the range (416), free-space check,
  GGUF magic validation, atomic rename, progress throttled to 5 Hz, cancellation discards the
  partial file only after the writer has stopped.
- Compose `DownloadsCard`: per-model size/quantization, RAM-fit warning, progress bar and
  cancel; the first downloaded model becomes active automatically.
- All new strings exist in English and Ukrainian.

### Automatic installation

- `install.ps1` (+ `install.bat`) and `install.sh`:
  - locate or download a JDK 17+ (portable Temurin 21 in `%LOCALAPPDATA%\TumbaAI\jdk`),
  - locate or install the Android SDK (cmdline-tools, platform 35, build-tools 35.0.0,
    platform-tools) and accept licences non-interactively,
  - write `local.properties`,
  - build the APK with the wrapper,
  - build the native engine automatically when the NDK is present and no `libtumba_llama.so`
    exists (`-LocalAi` / `--local-ai` installs the NDK on demand),
  - install with `adb install -r -d` and launch the app.
  A failed native build is **non-fatal** unless local AI was explicitly requested, so
  one-command install still produces a working cloud-capable APK.
- `tools/build-native.ps1` / `.sh` fetch llama.cpp, add the SDK's Ninja to `PATH` and pass
  `CMAKE_MAKE_PROGRAM`, then strip the library into `app/src/main/jniLibs/<abi>/`.

## Native engine: how it was built and verified

The C++ bridge was first syntax-checked against the real llama.cpp headers, then **fully
compiled and linked** for both ABIs with NDK 27.0.12077973 / CMake 3.22.1, Release, no
OpenMP/Vulkan/CUDA/Metal:

```powershell
cmake -S app/src/main/cpp -B build/native/arm64-v8a -G Ninja `
  -DCMAKE_TOOLCHAIN_FILE=<ndk>/build/cmake/android.toolchain.cmake `
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release `
  -DLLAMA_CPP_DIR=<repo>/app/src/main/cpp/llama.cpp `
  -DCMAKE_LIBRARY_OUTPUT_DIRECTORY=<repo>/app/src/main/jniLibs/arm64-v8a
cmake --build build/native/arm64-v8a --target tumba_llama --parallel 8   # 77/77 steps, exit 0
llvm-strip --strip-unneeded app/src/main/jniLibs/arm64-v8a/libtumba_llama.so
```

The same steps produced the `x86_64` library. Verification of the output:

| Check | `arm64-v8a` | `x86_64` |
| --- | --- | --- |
| Size after strip | 6,600,224 bytes | 7,247,624 bytes |
| ELF class/type | ELF64 `DYN` | ELF64 `DYN` |
| Machine | AArch64 | Advanced Micro Devices X86-64 |
| `NEEDED` | `liblog`, `libandroid`, `libm`, `libdl`, `libc` | same |
| Exports | `JNI_OnLoad`, `Java_com_tumba_ai_data_ai_local_LlamaCppEngine_native{Version,LoadModel,Generate,FreeModel,ContextSize}` | same |
| Inside APK | `lib/arm64-v8a/libtumba_llama.so` | `lib/x86_64/libtumba_llama.so` |

Note on this sandbox: executables started from the agent workspace cannot create files
(`clang.exe: unable to open output file ...: Permission denied`). Copying the NDK/CMake to
`%LOCALAPPDATA%\TumbaAI\` resolved it; this is a sandbox restriction, not a project issue.
AGP's own `stripDebugDebugSymbols` still hits it for third-party `.so` files in this
environment, which is harmless (those libraries are packaged as-is) and does not affect
`libtumba_llama.so` thanks to `keepDebugSymbols`.

## Installer verification

`install.ps1 -SkipBuild` completed successfully in this environment:

1. detected `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`;
2. reused the SDK from `local.properties` and rewrote the file;
3. found the committed native library for both ABIs and skipped the native build;
4. located the debug APK and started ADB.

The portable JDK path was also exercised: the installer downloaded and extracted Temurin 21
to `%LOCALAPPDATA%\TumbaAI\jdk` and later accepted it via `-JdkHome`.

The only failure is environmental: `adb` cannot create its log in `%TEMP%`
(`Permission denied` in the sandbox), so no device was ever listed. The script detects this,
prints the "No device detected" instructions and exits with code 0 instead of failing.

## Artifacts

| Artifact | Path | Size | SHA-256 |
| --- | --- | --- | --- |
| Installable debug APK | `dist/tumba-ai-1.2.0.apk` | 33,496,455 bytes | `F50ADB993F130ADED9D918C815D3EF2C0EF9E71852D6CB781AE909DA593B5BA8` |
| Unsigned release APK | `app/build/outputs/apk/release/app-release-unsigned.apk` | 26,795,964 bytes | `2DE57ABAC580B97FB689C53E10F24591786C1EEE1B0BB04DBCA292F0C38605E5` |

`aapt2 dump badging` confirms: package `com.tumba.ai`, versionName `1.2.0`, versionCode 5,
minSdk 26, targetSdk 35. The APK contains two 64-bit ABIs with `libtumba_llama.so` in each
and no model weights. `zipalign -c -P 16` reports both libraries 16 KB-page aligned
(required on Android 15 devices with 16 KB pages), and `apksigner verify` passes with the
same debug certificate as 1.1.0/1.1.1, so `adb install -r` upgrades in place.

## What is verified

- Full Kotlin/Java compilation, KSP processing, Hilt graph, Room DAO queries, resource linking
  for both locales.
- 35 JVM tests, including a real OkHttp/SSE pipeline against MockWebServer.
- Native engine compiled, linked and stripped for `arm64-v8a` and `x86_64`; ELF type,
  dependencies and JNI exports inspected; both libraries present inside the APK.
- Download catalogue invariants and the URL/size values (verified with HTTP HEAD earlier).
- APK packaging and debug signing; installer flow up to the device step.

## What is **not** verified (honest limits)

1. **No device or emulator run.** UI behaviour, navigation, IME insets, SAF pickers, Keystore
   encryption, process-death restoration and actual token streaming on a phone were not
   exercised at runtime.
2. **The native library was never executed here.** It links and exports the expected JNI
   symbols, but no device/emulator loaded it, so actual model loading and token generation
   still need a runtime check on a phone (`adb install -r dist/tumba-ai-1.2.0.apk`).
3. **No live provider calls.** No API keys were available; Gemini/DeepSeek/custom HTTP+SSE is
   mock-tested, not exercised against the real services.
4. **No full model download was executed here.** The four catalogue URLs return HTTP 200 with
   the expected `Content-Length`, but a 0.5вЂ“2 GB transfer and its resume path were not run.
5. **Instrumented tests** are not included (no emulator).
6. **The v1 в†’ v2 migration** was not executed against a real v1 database; it is covered by
   Room schema export and code review.
7. **Release APK is unsigned.** Sign it with a production keystore before distribution.

## Lint

`lintDebug`: **0 errors and 72 warnings**. The dominant categories are unused strings kept for
future screens, `PluralsCandidate`, `MonochromeLauncherIcon` (lint does not see the `-v33`
variants) and a `UsableSpace` suggestion. No dependency-version or API-level errors remain.
`lintVitalRelease` runs as part of `assembleRelease`.