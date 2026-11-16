# Third-party notices

TUMBA AI includes or links against the following third-party components.

## llama.cpp / ggml (on-device inference)

- Project: https://github.com/ggml-org/llama.cpp
- Pinned release: `v0.9.0` (fetched by `tools/build-native.ps1` / `tools/build-native.sh`)
- License: MIT

`app/src/main/cpp/tumba_llama.cpp` is TUMBA's JNI bridge; llama.cpp and ggml are compiled
into `libtumba_llama.so`. The full MIT text ships with the fetched sources in
`app/src/main/cpp/llama.cpp/LICENSE`.

## Model weights

Model weights are **never** bundled in the APK. The in-app download catalogue points at
these officially hosted files; each model keeps its own license, which you accept by
downloading it:

| Model | Host | License |
| --- | --- | --- |
| Qwen2.5 0.5B / 1.5B Instruct (GGUF) | huggingface.co/Qwen | Apache-2.0 |
| Qwen2.5 3B Instruct (GGUF, bartowski) | huggingface.co/bartowski | Apache-2.0 |
| Llama 3.2 1B Instruct (GGUF, bartowski) | huggingface.co/bartowski | Llama 3.2 Community License |

## Android and Kotlin libraries

Jetpack Compose, AndroidX, Hilt (Apache-2.0), Room (Apache-2.0), OkHttp/Retrofit
(Apache-2.0), kotlinx.coroutines and kotlinx.serialization (Apache-2.0), Coil (Apache-2.0).
Their notices are available from their respective repositories.