#requires -Version 5.1
<#
.SYNOPSIS
    One-command setup, build and installation of TUMBA AI on a connected Android device.

.DESCRIPTION
    Finds (or installs) a JDK, the Android SDK and platform tools, builds the APK with the
    bundled Gradle wrapper, and installs it over ADB. Safe to re-run: existing tooling is
    reused, an already connected device is picked automatically.

.PARAMETER Release
    Build an unsigned release APK instead of the debug APK. Android refuses to update an
    app signed by a different key, so uninstall the debug build first.

.PARAMETER LocalAi
    Build the native llama.cpp engine before the APK (requires the NDK; installed on demand).

.PARAMETER SkipLocalAi
    Do not build the native engine even if the NDK is available (cloud-only APK).

.PARAMETER SkipBuild
    Only install the APK that already exists in app/build/outputs.

.PARAMETER NoLaunch
    Install but do not start the app.

.PARAMETER Device
    ADB serial to target when more than one device is connected.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File install.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File install.ps1 -LocalAi
#>
[CmdletBinding()]
param(
    [switch]$Release,
    [switch]$LocalAi,
    [switch]$SkipLocalAi,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [string]$Device,
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$SdkRoot = $env:ANDROID_HOME
)

$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
$toolingRoot = Join-Path $env:LOCALAPPDATA 'TumbaAI'
$ndkVersion = '27.0.12077973'
$cmakeVersion = '3.22.1'

function Write-Step([string]$text) {
    Write-Host ""
    Write-Host "==> $text" -ForegroundColor Cyan
}

function Write-Ok([string]$text) {
    Write-Host "    $text" -ForegroundColor Green
}

function Test-Jdk([string]$homePath) {
    if (-not $homePath) { return $false }
    $java = Join-Path $homePath 'bin/java.exe'
    if (-not (Test-Path $java)) { return $false }
    # A JDK always ships a `release` file with JAVA_VERSION; reading it is more reliable
    # than parsing `java -version`, whose banner goes to stderr and can be swallowed by
    # non-interactive hosts.
    $release = Join-Path $homePath 'release'
    if (Test-Path $release) {
        $line = Get-Content $release -ErrorAction SilentlyContinue |
            Where-Object { $_ -match '^\s*JAVA_VERSION\s*=\s*"?(\d+)' } |
            Select-Object -First 1
        if ($line -and $line -match '^\s*JAVA_VERSION\s*=\s*"?(\d+)') {
            return ([int]$matches[1] -ge 17)
        }
    }
    # Fallback for stripped layouts: derive the version from the folder name.
    $leaf = Split-Path $homePath -Leaf
    if ($leaf -match '(?i)(?:jdk|jre)[-_]?(?:1\.)?(\d+)') {
        return ([int]$matches[1] -ge 17)
    }
    return $false
}

# Looks for a `bin\java.exe` at $root, in a direct child, or in a grandchild, so that
# both layouts ("...\jdk-21" and "...\Microsoft\jdk-17.0.x") are covered.
function Find-JdkUnder([string]$root) {
    if (-not $root -or -not (Test-Path $root)) { return $null }
    if (Test-Jdk $root) { return $root }
    foreach ($child in (Get-ChildItem $root -Directory -ErrorAction SilentlyContinue)) {
        if (Test-Jdk $child.FullName) { return $child.FullName }
        foreach ($grand in (Get-ChildItem $child.FullName -Directory -ErrorAction SilentlyContinue)) {
            if (Test-Jdk $grand.FullName) { return $grand.FullName }
        }
    }
    return $null
}

function Resolve-Jdk {
    # 1. Explicit argument / JAVA_HOME of this process.
    if (Test-Jdk $JdkHome) { return $JdkHome }

    # 2. Well-known install roots (covers Android Studio's bundled JBR, Adoptium,
    #    Microsoft OpenJDK, IntelliJ downloads and a previously installed portable JDK).
    $roots = @(
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Microsoft'),
        (Join-Path $env:ProgramFiles 'Zulu'),
        (Join-Path $env:ProgramFiles 'BellSoft'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'),
        (Join-Path $toolingRoot 'jdk'),
        (Join-Path $env:USERPROFILE '.jdks')
    )
    if ($env:ProgramW6432) { $roots += (Join-Path $env:ProgramW6432 'Java') }
    foreach ($root in $roots) {
        $found = Find-JdkUnder $root
        if ($found) { return $found }
    }

    # 3. Whatever `java` the PATH resolves to (where.exe works even in -NoProfile shells).
    $whereJava = & where.exe java 2>$null | Select-Object -First 1
    if ($whereJava) {
        $javaHome = Split-Path (Split-Path $whereJava -Parent) -Parent
        if (Test-Jdk $javaHome) { return $javaHome }
    }
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaHome = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
        if (Test-Jdk $javaHome) { return $javaHome }
    }
    return $null
}

function Install-Jdk {
    Write-Step "Installing a portable JDK 21 (Temurin) into $toolingRoot\jdk"
    New-Item -ItemType Directory -Force -Path $toolingRoot | Out-Null
    $zip = Join-Path $toolingRoot 'jdk21.zip'
    $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    $extract = Join-Path $toolingRoot 'jdk-extract'
    if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -Path $zip -DestinationPath $extract -Force
    $inner = Get-ChildItem $extract -Directory | Where-Object {
        Test-Path (Join-Path $_.FullName 'bin/java.exe')
    } | Select-Object -First 1
    if (-not $inner) { throw "The JDK archive did not contain a usable JDK." }
    $target = Join-Path $toolingRoot 'jdk'
    if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    Move-Item $inner.FullName $target
    Remove-Item -Recurse -Force $extract
    Remove-Item -Force $zip
    if (-not (Test-Jdk $target)) {
        throw "The downloaded JDK at $target could not be validated. Install a JDK 17+ manually and re-run."
    }
    return $target
}

function Resolve-Sdk {
    $candidates = @()
    if ($SdkRoot) { $candidates += $SdkRoot }
    if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }
    $localProperties = Join-Path $repoRoot 'local.properties'
    if (Test-Path $localProperties) {
        $line = Get-Content $localProperties |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
            Select-Object -First 1
        if ($line) { $candidates += ($line -replace '^\s*sdk\.dir\s*=\s*', '').Trim().Replace('\\', '\') }
    }
    $candidates += @(
        (Join-Path $toolingRoot 'android-sdk'),
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
        (Join-Path $env:ProgramFiles 'Android\android-sdk'),
        'C:\Android\Sdk'
    )
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $sdkManager = Join-Path $candidate 'cmdline-tools/latest/bin/sdkmanager.bat'
        $platform = Join-Path $candidate 'platforms/android-35'
        if ((Test-Path $sdkManager) -or (Test-Path $platform)) { return $candidate }
    }
    return $null
}

function Install-Sdk {
    Write-Step "Installing the Android SDK into $toolingRoot\android-sdk"
    $sdk = Join-Path $toolingRoot 'android-sdk'
    $zip = Join-Path $toolingRoot 'cmdline-tools.zip'
    $url = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
    New-Item -ItemType Directory -Force -Path $sdk | Out-Null
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    $extract = Join-Path $toolingRoot 'cmdline-tools-extract'
    if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -Path $zip -DestinationPath $extract -Force
    $latest = Join-Path $sdk 'cmdline-tools/latest'
    New-Item -ItemType Directory -Force -Path $latest | Out-Null
    Copy-Item -Path (Join-Path $extract 'cmdline-tools/*') -Destination $latest -Recurse -Force
    Remove-Item -Recurse -Force $extract
    Remove-Item -Force $zip

    $sdkManager = Join-Path $latest 'bin/sdkmanager.bat'
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $resolvedJdk
    Write-Step "Accepting SDK licenses (automatic)"
    ("y`n" * 120) | & $sdkManager --sdk_root="$sdk" --licenses | Out-Null
    Write-Step "Downloading platform-tools, platform 35, build-tools 35.0.0"
    & $sdkManager --sdk_root="$sdk" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw "sdkmanager failed to install the required packages." }
    $env:JAVA_HOME = $previousJavaHome
    return $sdk
}

function Resolve-Adb {
    param([string]$Sdk)
    $local = Join-Path $Sdk 'platform-tools/adb.exe'
    if (Test-Path $local) { return $local }
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    return $null
}

function Invoke-Adb {
    param(
        [string]$AdbPath,
        [string[]]$AdbArgs
    )
    # ADB writes banners and daemon problems to stderr. With $ErrorActionPreference='Stop'
    # PowerShell 5.1 turns those into terminating errors, so scope it down just here.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    try {
        $result = & $AdbPath @AdbArgs 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    [pscustomobject]@{
        Output   = @($result | ForEach-Object { "$_" })
        ExitCode = $code
    }
}

# ---------------------------------------------------------------------------------------
# 1. JDK
# ---------------------------------------------------------------------------------------
Write-Step "Checking Java 17+"
$resolvedJdk = Resolve-Jdk
if (-not $resolvedJdk) {
    $resolvedJdk = Install-Jdk
}
if (-not (Test-Jdk $resolvedJdk)) {
    throw "No usable JDK 17+ was found and the automatic download did not succeed. Install a JDK and re-run."
}
$env:JAVA_HOME = $resolvedJdk
$env:PATH = "$resolvedJdk\bin;" + $env:PATH
Write-Ok "JDK: $resolvedJdk"

# ---------------------------------------------------------------------------------------
# 2. Android SDK
# ---------------------------------------------------------------------------------------
Write-Step "Checking the Android SDK"
$resolvedSdk = Resolve-Sdk
if (-not $resolvedSdk) {
    $resolvedSdk = Install-Sdk
}
Write-Ok "SDK: $resolvedSdk"

$localProperties = Join-Path $repoRoot 'local.properties'
$sdkDirValue = $resolvedSdk.Replace('\', '\\')
Set-Content -Path $localProperties -Value "sdk.dir=$sdkDirValue" -Encoding ASCII
Write-Ok "local.properties -> $resolvedSdk"

$sdkManager = Join-Path $resolvedSdk 'cmdline-tools/latest/bin/sdkmanager.bat'
$needsPackages = -not (Test-Path (Join-Path $resolvedSdk 'platforms/android-35')) -or
    -not (Test-Path (Join-Path $resolvedSdk 'build-tools/35.0.0')) -or
    -not (Test-Path (Join-Path $resolvedSdk 'platform-tools/adb.exe'))
if ($needsPackages -and (Test-Path $sdkManager)) {
    Write-Step "Installing missing SDK packages"
    ("y`n" * 120) | & $sdkManager --sdk_root="$resolvedSdk" --licenses | Out-Null
    & $sdkManager --sdk_root="$resolvedSdk" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw "sdkmanager failed to install the required packages." }
}

# ---------------------------------------------------------------------------------------
# 3. Native engine
#    Built automatically when an NDK is already installed and no prebuilt library exists.
#    Pass -LocalAi to install the NDK on demand, or -SkipLocalAi to stay cloud-only.
# ---------------------------------------------------------------------------------------
$prebuilt = Join-Path $repoRoot 'app/src/main/jniLibs/arm64-v8a/libtumba_llama.so'
$ndkPath = Join-Path $resolvedSdk "ndk/$ndkVersion"
$shouldBuildNative = -not $SkipLocalAi -and (
    $LocalAi -or ((-not (Test-Path $prebuilt)) -and (Test-Path $ndkPath))
)

if ($shouldBuildNative) {
    if (-not (Test-Path $ndkPath)) {
        if (-not (Test-Path $sdkManager)) {
            throw "The NDK is required for -LocalAi but sdkmanager was not found. Install Android cmdline-tools first."
        }
        Write-Step "Installing NDK $ndkVersion and CMake $cmakeVersion"
        & $sdkManager --sdk_root="$resolvedSdk" "ndk;$ndkVersion" "cmake;$cmakeVersion"
        if ($LASTEXITCODE -ne 0) { throw "Could not install the NDK." }
    }
    try {
        & (Join-Path $repoRoot 'tools/build-native.ps1') -SdkRoot $resolvedSdk -NdkVersion $ndkVersion -CMakeVersion $cmakeVersion
        if (-not (Test-Path $prebuilt)) {
            throw "libtumba_llama.so was not produced."
        }
        Write-Ok "Native engine: $prebuilt"
    } catch {
        # Losing the native engine must not block installing the app: cloud providers keep
        # working, and the Models screen explains that local inference is unavailable.
        if ($LocalAi) { throw }
        Write-Host "Native engine build failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Continuing with the cloud-only APK. Run tools\build-native.ps1 to retry." -ForegroundColor Yellow
    }
} elseif (-not $SkipLocalAi -and -not (Test-Path $prebuilt)) {
    Write-Host "No native engine library yet. Local GGUF inference will be unavailable." -ForegroundColor Yellow
    Write-Host "Re-run with -LocalAi to install the NDK and build it automatically." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------------------
# 4. Build
# ---------------------------------------------------------------------------------------
$variant = if ($Release) { 'Release' } else { 'Debug' }
$apk = Join-Path $repoRoot "app/build/outputs/apk/$($variant.ToLower())/app-$($variant.ToLower()).apk"

if (-not $SkipBuild) {
    Write-Step "Building the $variant APK (first run downloads Gradle and dependencies)"
    Push-Location $repoRoot
    try {
        $task = if ($Release) { 'assembleRelease' } else { 'assembleDebug' }
        & (Join-Path $repoRoot 'gradlew.bat') $task --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $apk)) {
    throw "APK not found at $apk"
}
Write-Ok "APK: $apk"

# ---------------------------------------------------------------------------------------
# 5. Install
# ---------------------------------------------------------------------------------------
Write-Step "Looking for a connected device"
$adb = Resolve-Adb -Sdk $resolvedSdk
if (-not $adb) {
    Write-Host "ADB was not found. The APK is ready at $apk - copy it to the phone and install it manually." -ForegroundColor Yellow
    exit 0
}

Invoke-Adb -AdbPath $adb -AdbArgs @('start-server') | Out-Null
$devicesResult = Invoke-Adb -AdbPath $adb -AdbArgs @('devices')
$devices = $devicesResult.Output |
    Select-Object -Skip 1 |
    Where-Object { $_ -match '^\S+\s+device$' } |
    ForEach-Object { ($_ -split '\s+')[0] }

if (-not $devices) {
    if ($devicesResult.ExitCode -ne 0) {
        Write-Host "ADB could not start (exit $($devicesResult.ExitCode))." -ForegroundColor Yellow
    }
    Write-Host "No device detected." -ForegroundColor Yellow
    Write-Host "  1. Enable Developer options and USB debugging on the phone."
    Write-Host "  2. Connect it and accept the RSA prompt."
    Write-Host "  3. Run this script again, or install manually: $apk"
    exit 0
}

$serial = if ($Device) { $Device } else { $devices | Select-Object -First 1 }
if ($Device -and ($devices -notcontains $Device)) {
    throw "Device $Device is not connected. Available: $($devices -join ', ')"
}

Write-Step "Installing on $serial"
$installResult = Invoke-Adb -AdbPath $adb -AdbArgs @('-s', $serial, 'install', '-r', '-d', $apk)
$installResult.Output | ForEach-Object { Write-Host "    $_" }
if ($installResult.ExitCode -ne 0) { throw "ADB install failed." }

if (-not $NoLaunch) {
    Invoke-Adb -AdbPath $adb -AdbArgs @('-s', $serial, 'shell', 'am', 'start', '-n', 'com.tumba.ai/.MainActivity') | Out-Null
}

Write-Step "Done"
Write-Ok "TUMBA AI is installed on $serial"