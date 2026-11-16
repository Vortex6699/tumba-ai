#!/usr/bin/env bash
# Builds libtumba_llama.so for Android (macOS / Linux).
#
# Fetches a pinned llama.cpp release into app/src/main/cpp/llama.cpp (when missing),
# cross-compiles it with the Android NDK and writes the library into
# app/src/main/jniLibs/<abi>/. Gradle then packages it; the app build itself never needs
# the NDK.
#
#   ./tools/build-native.sh
#   ./tools/build-native.sh --abi arm64-v8a --clean
#   ./tools/build-native.sh --sdk-root "$ANDROID_HOME"

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP_DIR="$REPO_ROOT/app/src/main/cpp"
LLAMA_DIR="$CPP_DIR/llama.cpp"
JNI_ROOT="$REPO_ROOT/app/src/main/jniLibs"
BUILD_ROOT="$REPO_ROOT/build/native"

LLAMA_TAG="v0.9.0"
NDK_VERSION="27.0.12077973"
CMAKE_VERSION="3.22.1"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
CLEAN=0
ABIS=("arm64-v8a" "x86_64")

while [[ $# -gt 0 ]]; do
  case "$1" in
    --abi) ABIS=("$2"); shift 2 ;;
    --abi=*) ABIS=("${1#*=}"); shift ;;
    --llama-tag) LLAMA_TAG="$2"; shift 2 ;;
    --sdk-root) SDK_ROOT="$2"; shift 2 ;;
    --ndk-version) NDK_VERSION="$2"; shift 2 ;;
    --cmake-version) CMAKE_VERSION="$2"; shift 2 ;;
    --clean) CLEAN=1; shift ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

step() { printf '\n==> %s\n' "$1"; }
die() { printf 'error: %s\n' "$1" >&2; exit 1; }

if [[ -z "$SDK_ROOT" && -f "$REPO_ROOT/local.properties" ]]; then
  SDK_ROOT="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$REPO_ROOT/local.properties" | head -n1)"
fi
[[ -n "$SDK_ROOT" ]] || die "Android SDK not found. Pass --sdk-root or set ANDROID_HOME."
NDK="$SDK_ROOT/ndk/$NDK_VERSION"
CMAKE="$SDK_ROOT/cmake/$CMAKE_VERSION/bin/cmake"
[[ -d "$NDK" ]] || die "NDK $NDK_VERSION not found at $NDK"
[[ -x "$CMAKE" ]] || die "CMake $CMAKE_VERSION not found at $CMAKE"

STRIP="$NDK/toolchains/llvm/prebuilt/$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64/bin/llvm-strip"
# The SDK's CMake ships Ninja next to cmake; add it to PATH so `-G Ninja` resolves.
export PATH="$(dirname "$CMAKE"):$PATH"

if [[ "$CLEAN" -eq 1 ]]; then
  step "Cleaning previous native build output"
  rm -rf "$LLAMA_DIR" "$BUILD_ROOT" "$JNI_ROOT"
fi

if [[ ! -f "$LLAMA_DIR/include/llama.h" ]]; then
  step "Fetching llama.cpp $LLAMA_TAG"
  tmp="$(mktemp -d)"
  curl -fL --retry 3 -o "$tmp/llama.zip" \
    "https://codeload.github.com/ggml-org/llama.cpp/zip/refs/tags/$LLAMA_TAG"
  unzip -q "$tmp/llama.zip" -d "$tmp"
  mkdir -p "$LLAMA_DIR"
  cp -R "$tmp"/llama.cpp-*/. "$LLAMA_DIR/"
  rm -rf "$tmp"
fi

for abi in "${ABIS[@]}"; do
  step "Building tumba_llama for $abi"
  abi_build="$BUILD_ROOT/$abi"
  abi_out="$JNI_ROOT/$abi"
  mkdir -p "$abi_build" "$abi_out"
  "$CMAKE" -S "$CPP_DIR" -B "$abi_build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release \
    -DLLAMA_CPP_DIR="$LLAMA_DIR" \
    -DCMAKE_LIBRARY_OUTPUT_DIRECTORY="$abi_out" \
    -DCMAKE_ARCHIVE_OUTPUT_DIRECTORY="$abi_build/lib"
  "$CMAKE" --build "$abi_build" --target tumba_llama --parallel
  [[ -f "$abi_out/libtumba_llama.so" ]] || die "libtumba_llama.so was not produced for $abi"
  [[ -x "$STRIP" ]] && "$STRIP" --strip-unneeded "$abi_out/libtumba_llama.so"
  printf '    %s\n' "$abi_out/libtumba_llama.so"
done

step "Done"
echo "Now run: ./gradlew assembleDebug (or ./install.sh)"