#!/usr/bin/env bash
# One-command setup, build and installation of TUMBA AI (macOS / Linux).
#
#   ./install.sh                # build the debug APK and install it over adb
#   ./install.sh --local-ai     # build the native llama.cpp engine first
#   ./install.sh --skip-local-ai  # cloud-only APK
#   ./install.sh --release      # build an unsigned release APK
#   ./install.sh --skip-build   # install the APK that already exists
#   ./install.sh --no-launch    # do not start the app after installing
#
# Requires: a JDK 17+, curl, unzip. The Android SDK is installed automatically when
# missing (cmdline-tools + platform 35 + build-tools + platform-tools).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLING_ROOT="${XDG_DATA_HOME:-$HOME/.local/share}/tumba-ai"
NDK_VERSION="27.0.12077973"
CMAKE_VERSION="3.22.1"

BUILD_RELEASE=0
BUILD_LOCAL_AI=0
SKIP_LOCAL_AI=0
SKIP_BUILD=0
NO_LAUNCH=0
DEVICE=""

for arg in "$@"; do
  case "$arg" in
    --release) BUILD_RELEASE=1 ;;
    --local-ai) BUILD_LOCAL_AI=1 ;;
    --skip-local-ai) SKIP_LOCAL_AI=1 ;;
    --skip-build) SKIP_BUILD=1 ;;
    --no-launch) NO_LAUNCH=1 ;;
    --device=*) DEVICE="${arg#*=}" ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

step() { printf '\n==> %s\n' "$1"; }
ok() { printf '    %s\n' "$1"; }
die() { printf 'error: %s\n' "$1" >&2; exit 1; }

# --- JDK ------------------------------------------------------------------------------
resolve_jdk() {
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    echo "$JAVA_HOME"; return
  fi
  local candidates=(
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    "/usr/lib/jvm/default-java"
    "$TOOLING_ROOT/jdk"
  )
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/bin/java" ]]; then echo "$candidate"; return; fi
  done
  if command -v java >/dev/null 2>&1; then
    local home
    home="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
    [[ -x "$home/bin/java" ]] && { echo "$home"; return; }
  fi
  return 1
}

step "Checking Java 17+"
if ! JDK_HOME="$(resolve_jdk)"; then
  die "No JDK 17+ found. Install one (e.g. 'sudo apt install openjdk-21-jdk' or 'brew install openjdk@21') and re-run."
fi
JAVA_VERSION="$("$JDK_HOME/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n1)"
[[ "${JAVA_VERSION:-0}" -ge 17 ]] || die "Java $JAVA_VERSION found at $JDK_HOME, but 17+ is required."
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
ok "JDK: $JDK_HOME"

# --- Android SDK ----------------------------------------------------------------------
resolve_sdk() {
  local candidates=()
  [[ -n "${ANDROID_HOME:-}" ]] && candidates+=("$ANDROID_HOME")
  [[ -n "${ANDROID_SDK_ROOT:-}" ]] && candidates+=("$ANDROID_SDK_ROOT")
  if [[ -f "$REPO_ROOT/local.properties" ]]; then
    local from_file
    from_file="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$REPO_ROOT/local.properties" | head -n1)"
    [[ -n "$from_file" ]] && candidates+=("$from_file")
  fi
  candidates+=(
    "$TOOLING_ROOT/android-sdk"
    "$HOME/Android/Sdk"
    "$HOME/Library/Android/sdk"
    "/usr/local/share/android-sdk"
  )
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/platform-tools/adb" || -d "$candidate/platforms" ]]; then
      echo "$candidate"; return 0
    fi
  done
  return 1
}

install_sdk() {
  local sdk="$TOOLING_ROOT/android-sdk"
  local os
  case "$(uname -s)" in
    Darwin) os="mac" ;;
    *) os="linux" ;;
  esac
  mkdir -p "$sdk" "$TOOLING_ROOT"
  local url="https://dl.google.com/android/repository/commandlinetools-${os}-11076708_latest.zip"
  local zip="$TOOLING_ROOT/cmdline-tools.zip"
  step "Downloading Android command-line tools"
  curl -fL --retry 3 -o "$zip" "$url"
  rm -rf "$TOOLING_ROOT/cmdline-tools"
  unzip -q "$zip" -d "$TOOLING_ROOT/cmdline-tools"
  mkdir -p "$sdk/cmdline-tools/latest"
  cp -R "$TOOLING_ROOT/cmdline-tools/cmdline-tools/." "$sdk/cmdline-tools/latest/"
  rm -rf "$TOOLING_ROOT/cmdline-tools" "$zip"

  local sdkmanager="$sdk/cmdline-tools/latest/bin/sdkmanager"
  chmod +x "$sdkmanager"
  step "Accepting SDK licenses"
  yes | "$sdkmanager" --sdk_root="$sdk" --licenses >/dev/null || true
  step "Installing platform-tools, platform 35 and build-tools"
  "$sdkmanager" --sdk_root="$sdk" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
  echo "$sdk"
}

step "Checking the Android SDK"
if ! SDK_ROOT="$(resolve_sdk)"; then
  SDK_ROOT="$(install_sdk | tail -n1)"
fi
ok "SDK: $SDK_ROOT"
printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$REPO_ROOT/local.properties"

SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [[ -x "$SDKMANAGER" ]] && { [[ ! -d "$SDK_ROOT/platforms/android-35" ]] || [[ ! -d "$SDK_ROOT/build-tools/35.0.0" ]]; }; then
  step "Installing missing SDK packages"
  yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
  "$SDKMANAGER" --sdk_root="$SDK_ROOT" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
fi

# --- Native engine ---------------------------------------------------------------------
# Built automatically when the NDK is already installed and no library is present.
# --local-ai installs the NDK on demand; --skip-local-ai stays cloud-only.
PREBUILT="$REPO_ROOT/app/src/main/jniLibs/arm64-v8a/libtumba_llama.so"
if [[ "$SKIP_LOCAL_AI" -eq 0 ]]; then
  if [[ "$BUILD_LOCAL_AI" -eq 1 ]] || { [[ ! -f "$PREBUILT" ]] && [[ -d "$SDK_ROOT/ndk/$NDK_VERSION" ]]; }; then
    if [[ ! -d "$SDK_ROOT/ndk/$NDK_VERSION" ]]; then
      [[ -x "$SDKMANAGER" ]] || die "The NDK is required for --local-ai. Install Android cmdline-tools first."
      step "Installing NDK $NDK_VERSION and CMake $CMAKE_VERSION"
      "$SDKMANAGER" --sdk_root="$SDK_ROOT" "ndk;$NDK_VERSION" "cmake;$CMAKE_VERSION"
    fi
    step "Building the native llama.cpp engine"
    if ! "$REPO_ROOT/tools/build-native.sh" --sdk-root "$SDK_ROOT" --ndk-version "$NDK_VERSION" --cmake-version "$CMAKE_VERSION"; then
      if [[ "$BUILD_LOCAL_AI" -eq 1 ]]; then
        die "Native engine build failed."
      fi
      echo "Native engine build failed; installing the cloud-only APK instead."
      echo "Run tools/build-native.sh to retry later."
    fi
  elif [[ ! -f "$PREBUILT" ]]; then
    echo "No native engine library yet. Local GGUF inference will be unavailable."
    echo "Re-run with --local-ai to install the NDK and build it automatically."
  fi
fi

# --- Build ----------------------------------------------------------------------------
VARIANTS_DIR="$REPO_ROOT/app/build/outputs/apk"
if [[ "$BUILD_RELEASE" -eq 1 ]]; then
  TASK="assembleRelease"; APK="$VARIANTS_DIR/release/app-release-unsigned.apk"
else
  TASK="assembleDebug"; APK="$VARIANTS_DIR/debug/app-debug.apk"
fi

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  step "Building the APK"
  (cd "$REPO_ROOT" && ./gradlew "$TASK" --console=plain)
fi
[[ -f "$APK" ]] || die "APK not found at $APK"
ok "APK: $APK"

# --- Install --------------------------------------------------------------------------
ADB="$SDK_ROOT/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$(command -v adb || true)"
if [[ -z "$ADB" ]]; then
  printf '\nADB not found. The APK is ready at %s - install it manually.\n' "$APK"
  exit 0
fi

step "Looking for a connected device"
"$ADB" start-server >/dev/null
DEVICES=()
while IFS= read -r line; do
  [[ -n "$line" ]] && DEVICES+=("$line")
done < <("$ADB" devices | awk 'NR>1 && $2 == "device" { print $1 }')
if [[ "${#DEVICES[@]}" -eq 0 ]]; then
  printf '\nNo device detected.\n  1. Enable Developer options and USB debugging.\n  2. Connect and accept the RSA prompt.\n  3. Re-run this script, or install manually: %s\n' "$APK"
  exit 0
fi
SERIAL="${DEVICE:-${DEVICES[0]}}"

step "Installing on $SERIAL"
"$ADB" -s "$SERIAL" install -r -d "$APK"
if [[ "$NO_LAUNCH" -eq 0 ]]; then
  "$ADB" -s "$SERIAL" shell am start -n com.tumba.ai/.MainActivity >/dev/null || true
fi
step "Done"
ok "TUMBA AI is installed on $SERIAL"