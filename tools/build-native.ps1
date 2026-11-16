#requires -Version 5.1
<#
.SYNOPSIS
    Builds the native llama.cpp engine (libtumba_llama.so) for TUMBA AI.

.DESCRIPTION
    Fetches a pinned llama.cpp release into app/src/main/cpp/llama.cpp (unless it is already
    there), cross-compiles it with the Android NDK and copies the resulting shared library
    into app/src/main/jniLibs/<abi>/. The normal Gradle build then packages it automatically;
    Gradle itself never needs the NDK.

.PARAMETER Abi
    Android ABIs to build. Default: arm64-v8a (phones) and x86_64 (emulators).

.PARAMETER LlamaTag
    llama.cpp release tag. Keep it pinned so builds stay reproducible.

.PARAMETER Clean
    Remove previous CMake build directories and the fetched llama.cpp checkout first.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-native.ps1
    powershell -ExecutionPolicy Bypass -File tools\build-native.ps1 -Abi arm64-v8a -Clean
#>
[CmdletBinding()]
param(
    [string[]]$Abi = @('arm64-v8a', 'x86_64'),
    [string]$LlamaTag = 'v0.9.0',
    [string]$SdkRoot = $env:ANDROID_HOME,
    [string]$NdkVersion = '27.0.12077973',
    [string]$CMakeVersion = '3.22.1',
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$cppDir = Join-Path $repoRoot 'app/src/main/cpp'
$llamaDir = Join-Path $cppDir 'llama.cpp'
$jniRoot = Join-Path $repoRoot 'app/src/main/jniLibs'
$buildRoot = Join-Path $repoRoot 'build/native'

function Write-Step([string]$text) {
    Write-Host ""
    Write-Host "==> $text" -ForegroundColor Cyan
}

function Resolve-SdkRoot {
    $candidates = @()
    if ($SdkRoot) { $candidates += $SdkRoot }
    if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }
    $localProperties = Join-Path $repoRoot 'local.properties'
    if (Test-Path $localProperties) {
        $line = Get-Content $localProperties |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
            Select-Object -First 1
        if ($line) {
            $candidates += ($line -replace '^\s*sdk\.dir\s*=\s*', '').Trim().Replace('\\', '\')
        }
    }
    $candidates += @(
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
        (Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk'),
        'C:\Android\Sdk'
    )
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate 'ndk'))) { return $candidate }
    }
    throw "Android SDK with an NDK was not found. Install the NDK and/or pass -SdkRoot <path>. " +
          "See the README for the sdkmanager command."
}

$sdk = Resolve-SdkRoot
$ndk = Join-Path $sdk "ndk/$NdkVersion"
$cmake = Join-Path $sdk "cmake/$CMakeVersion/bin/cmake.exe"

if (-not (Test-Path $ndk)) {
    throw "NDK $NdkVersion is not installed at $ndk. Run: sdkmanager `"ndk;$NdkVersion`" `"cmake;$CMakeVersion`""
}
if (-not (Test-Path $cmake)) {
    throw "CMake $CMakeVersion is not installed at $cmake. Run: sdkmanager `"cmake;$CMakeVersion`""
}

$hostTag = if ($IsWindows -or $env:OS -eq 'Windows_NT') { 'windows-x86_64' } else { 'linux-x86_64' }
$strip = Join-Path $ndk "toolchains/llvm/prebuilt/$hostTag/bin/llvm-strip.exe"
if (-not (Test-Path $strip)) { $strip = Join-Path $ndk "toolchains/llvm/prebuilt/$hostTag/bin/llvm-strip" }

# The SDK's CMake ships Ninja next to cmake.exe, but that folder is not on PATH.
# Put it first so `-G Ninja` resolves without extra setup.
$cmakeBin = Split-Path $cmake -Parent
$ninja = Join-Path $cmakeBin 'ninja.exe'
if (-not (Test-Path $ninja)) { $ninja = Join-Path $cmakeBin 'ninja' }
$env:PATH = "$cmakeBin;$env:PATH"

if ($Clean) {
    Write-Step "Cleaning previous native build output"
    if (Test-Path $llamaDir) { Remove-Item -Recurse -Force $llamaDir }
    if (Test-Path $buildRoot) { Remove-Item -Recurse -Force $buildRoot }
    if (Test-Path $jniRoot) { Remove-Item -Recurse -Force $jniRoot }
}

if (-not (Test-Path (Join-Path $llamaDir 'include/llama.h'))) {
    Write-Step "Fetching llama.cpp $LlamaTag"
    $archive = Join-Path ([System.IO.Path]::GetTempPath()) "llama-$LlamaTag.zip"
    $extract = Join-Path ([System.IO.Path]::GetTempPath()) "llama-$LlamaTag"
    $url = "https://codeload.github.com/ggml-org/llama.cpp/zip/refs/tags/$LlamaTag"
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing
    if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -Path $archive -DestinationPath $extract -Force
    $inner = Get-ChildItem $extract -Directory | Select-Object -First 1
    if (-not $inner) { throw "The llama.cpp archive did not contain a source directory." }
    New-Item -ItemType Directory -Force -Path $llamaDir | Out-Null
    Copy-Item -Path (Join-Path $inner.FullName '*') -Destination $llamaDir -Recurse -Force
    Remove-Item -Force $archive
    Remove-Item -Recurse -Force $extract
} else {
    Write-Host "llama.cpp sources already present in app/src/main/cpp/llama.cpp"
}

foreach ($architecture in $Abi) {
    Write-Step "Building tumba_llama for $architecture"
    $abiBuild = Join-Path $buildRoot $architecture
    $abiOut = Join-Path $jniRoot $architecture
    New-Item -ItemType Directory -Force -Path $abiBuild, $abiOut | Out-Null

    & $cmake -S $cppDir -B $abiBuild -G Ninja `
        "-DCMAKE_MAKE_PROGRAM=$ninja" `
        "-DCMAKE_TOOLCHAIN_FILE=$ndk/build/cmake/android.toolchain.cmake" `
        "-DANDROID_ABI=$architecture" `
        "-DANDROID_PLATFORM=android-26" `
        "-DCMAKE_BUILD_TYPE=Release" `
        "-DLLAMA_CPP_DIR=$llamaDir" `
        "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=$abiOut" `
        "-DCMAKE_ARCHIVE_OUTPUT_DIRECTORY=$abiBuild/lib"
    if ($LASTEXITCODE -ne 0) { throw "CMake configure failed for $architecture" }

    & $cmake --build $abiBuild --target tumba_llama --parallel
    if ($LASTEXITCODE -ne 0) { throw "Build failed for $architecture" }

    $artifact = Join-Path $abiOut 'libtumba_llama.so'
    if (-not (Test-Path $artifact)) { throw "libtumba_llama.so was not produced for $architecture" }
    if (Test-Path $strip) { & $strip --strip-unneeded $artifact }
    $size = [Math]::Round((Get-Item $artifact).Length / 1MB, 2)
    Write-Host "    $artifact ($size MB)" -ForegroundColor Green
}

Write-Step "Done"
Write-Host "Native engine built. Now run: .\gradlew.bat assembleDebug (or install.ps1)"