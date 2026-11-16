# On-device engine (llama.cpp)

TUMBA runs GGUF models locally through llama.cpp. The JNI bridge in this folder is a real
engine, not a stub: it loads the model, applies the model's own chat template, samples with a
standard top-k / top-p / temperature chain, streams UTF-8 tokens back to Kotlin and polls a
cancellation callback between tokens.

```
app/src/main/cpp/
├── CMakeLists.txt      builds libtumba_llama.so from llama.cpp (static) + the JNI bridge
├── tumba_llama.cpp     JNI surface used by LlamaCppEngine
└── llama.cpp/          fetched by tools/build-native.* (git-ignored, pinned tag)
```

`libtumba_llama.so` is committed under `app/src/main/jniLibs/<abi>/`, so the normal Gradle
build never needs the NDK. Rebuild it only when the bridge or llama.cpp version changes.

## Rebuilding

```powershell
# Windows (installs NDK/CMake on demand through Android Studio's sdkmanager first)
powershell -ExecutionPolicy Bypass -File tools\build-native.ps1
# one ABI, fresh checkout
powershell -ExecutionPolicy Bypass -File tools\build-native.ps1 -Abi arm64-v8a -Clean
```

```bash
# macOS / Linux
./tools/build-native.sh
./tools/build-native.sh --abi arm64-v8a --clean
```

The script fetches the pinned llama.cpp release (`v0.9.0`), configures CMake with the NDK
toolchain for `android-26`, builds the `tumba_llama` target and strips the result into
`app/src/main/jniLibs/<abi>/`.

## JNI contract

`LlamaCppEngine` declares these private native methods; names must stay in sync:

| Method | Purpose |
| --- | --- |
| `nativeVersion()` | llama.cpp version, used as a load smoke test |
| `nativeLoadModel(path)` | loads + mmaps a GGUF file, returns an opaque handle |
| `nativeFreeModel(handle)` | releases the model (also called from `onTrimMemory`) |
| `nativeGenerate(handle, roles, contents, maxTokens, temperature, contextSize, threads, callback)` | formats, tokenizes, decodes and streams tokens |
| `nativeContextSize(handle)` | the model's trained context length |

`TokenCallback` (`onToken`, `isCancelled`) is resolved by name in `JNI_OnLoad`, so it must
stay public and unmangled.

## Behaviour and limits

- The model stays loaded between messages; the llama context and sampler are per request.
- Prompts longer than the context are truncated from the oldest tokens, with a log line.
- Cancellation is checked between tokens, so "Stop" returns within one token.
- `temperature <= 0` switches to greedy sampling (deterministic).
- Only text is supported. Vision and tool calling are out of scope for the local path.
- The library is built for `arm64-v8a` and `x86_64`. Other ABIs report the engine as
  unavailable instead of crashing.

## Licences

llama.cpp and ggml are MIT licensed. Model weights are never bundled — see
`THIRD_PARTY_NOTICES.md` and the licence of each model you download.