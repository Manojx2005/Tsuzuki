[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$SdkRoot,
    [string]$UpstreamAar,
    [ValidateRange(1, 8)][int]$Jobs = 4,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$nativeRoot = Join-Path $repoRoot '.native'
$sourceRoot = Join-Path $nativeRoot 'src'
$archiveRoot = Join-Path $nativeRoot 'archives'
$buildRoot = Join-Path $nativeRoot 'build-arm64'
$outputRoot = Join-Path $nativeRoot 'output'
$evidenceRoot = Join-Path $repoRoot 'app/build/nt1-binary-evidence'
$profileRoot = Join-Path $PSScriptRoot 'neural-tts'
$ndkRoot = Join-Path $SdkRoot 'ndk/27.1.12297006'
$llvmRoot = Join-Path $ndkRoot 'toolchains/llvm/prebuilt/windows-x86_64/bin'
$cmakeRoot = Join-Path $SdkRoot 'cmake/3.22.1/bin'
$cmake = Join-Path $cmakeRoot 'cmake.exe'
foreach ($directory in @($nativeRoot, $archiveRoot, $evidenceRoot, $outputRoot)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}
foreach ($required in @($cmake, (Join-Path $llvmRoot 'llvm-strip.exe'), (Join-Path $llvmRoot 'llvm-nm.exe'))) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required pinned tool is missing: $required" }
}

function Get-VerifiedFile([string]$Url, [string]$Destination, [string]$Sha256) {
    if (-not (Test-Path -LiteralPath $Destination)) {
        if ($Offline) { throw "Offline build is missing: $Destination" }
        Invoke-WebRequest -Uri $Url -OutFile $Destination
    }
    $actual = (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $Sha256) { throw "SHA-256 mismatch: $Destination" }
}

$sourceArchive = Join-Path $archiveRoot 'source-v2.0.1.zip'
Get-VerifiedFile 'https://codeload.github.com/k2-fsa/sherpa-onnx/zip/refs/tags/v2.0.1' $sourceArchive `
    '71397fbf7c30cf81a56247e4c45bd3748abb590e761fbcfa2989a33ca7644282'
& python (Join-Path $profileRoot 'prepare-source.py') $sourceArchive $sourceRoot
if ($LASTEXITCODE -ne 0) { throw 'Source verification failed' }

# Apply only a reviewed source patch. Never alter bytes in an already-linked library.
$patch = Join-Path $profileRoot 'kokoro-only-v2.0.1.patch'
Push-Location $repoRoot
try {
    & git apply --check --directory=.native/src $patch 2>$null
    if ($LASTEXITCODE -eq 0) {
        & git apply --directory=.native/src $patch
        if ($LASTEXITCODE -ne 0) { throw 'Source patch failed' }
    } else {
        & git apply --reverse --check --directory=.native/src $patch
        if ($LASTEXITCODE -ne 0) { throw 'Source is neither pristine nor the recorded profile' }
    }
} finally { Pop-Location }
$profileHashes = Get-Content -LiteralPath (Join-Path $profileRoot 'profile-files-v2.0.1.json') -Raw | ConvertFrom-Json
foreach ($entry in $profileHashes.PSObject.Properties) {
    $actual = (Get-FileHash -LiteralPath (Join-Path $sourceRoot $entry.Name) -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $entry.Value) { throw "Patched source differs: $($entry.Name)" }
}
$dependencies = Get-Content -LiteralPath (Join-Path $profileRoot 'dependencies-v2.0.1.json') -Raw | ConvertFrom-Json
foreach ($dependency in $dependencies) {
    Get-VerifiedFile $dependency.url (Join-Path $sourceRoot $dependency.filename) $dependency.sha256
}

$configArgs = @(
    '-S', $sourceRoot, '-B', $buildRoot, '-G', 'Ninja',
    "-DCMAKE_MAKE_PROGRAM=$cmakeRoot/ninja.exe",
    "-DCMAKE_TOOLCHAIN_FILE=$ndkRoot/build/cmake/android.toolchain.cmake",
    "-DCMAKE_INSTALL_PREFIX=$nativeRoot/install-arm64",
    '-DANDROID_ABI=arm64-v8a', '-DANDROID_PLATFORM=android-26', '-DANDROID_STL=c++_static',
    '-DCMAKE_BUILD_TYPE=Release', '-DBUILD_SHARED_LIBS=OFF', '-DCMAKE_EXPORT_COMPILE_COMMANDS=ON',
    '-DSHERPA_ONNX_ENABLE_JNI=ON', '-DSHERPA_ONNX_ENABLE_C_API=OFF',
    '-DSHERPA_ONNX_ENABLE_SPEAKER_DIARIZATION=OFF', '-DSHERPA_ONNX_ENABLE_BINARY=OFF',
    '-DSHERPA_ONNX_ENABLE_PORTAUDIO=OFF', '-DSHERPA_ONNX_ENABLE_WEBSOCKET=OFF',
    '-DSHERPA_ONNX_USE_PRE_INSTALLED_ONNXRUNTIME_IF_AVAILABLE=OFF',
    '-DCMAKE_DISABLE_FIND_PACKAGE_Git=ON', '-DCMAKE_TLS_VERIFY=ON'
)
& $cmake @configArgs *> (Join-Path $evidenceRoot 'configure-arm64.log')
if ($LASTEXITCODE -ne 0) { throw 'Native configure failed; see configure-arm64.log' }
& $cmake --build $buildRoot --target sherpa-onnx-jni --parallel $Jobs *> (Join-Path $evidenceRoot 'build-arm64-final.log')
if ($LASTEXITCODE -ne 0) { throw 'Native build failed; see build-arm64-final.log' }
$libraryDirectory = Join-Path $outputRoot 'arm64-v8a'
New-Item -ItemType Directory -Path $libraryDirectory -Force | Out-Null
$library = Join-Path $libraryDirectory 'libsherpa-onnx-jni.so'
& (Join-Path $llvmRoot 'llvm-strip.exe') --strip-unneeded (Join-Path $buildRoot 'lib/libsherpa-onnx-jni.so') -o $library
if ($LASTEXITCODE -ne 0) { throw 'Native strip failed' }

if (-not $UpstreamAar) {
    $UpstreamAar = Join-Path $archiveRoot 'sherpa-onnx-2.0.1.aar'
}
Get-VerifiedFile 'https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-2.0.1.aar' $UpstreamAar `
    'a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7'
$candidate = Join-Path $outputRoot 'sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar'
& python (Join-Path $profileRoot 'package-runtime.py') --upstream-aar $UpstreamAar --library $library `
    --build $buildRoot --llvm-nm (Join-Path $llvmRoot 'llvm-nm.exe') `
    --ndk-notice (Join-Path $ndkRoot 'NOTICE') --output $candidate
if ($LASTEXITCODE -ne 0) { throw 'Candidate API/packaging check failed' }
& python (Join-Path $PSScriptRoot 'audit-neural-runtime.py') $candidate `
    --output (Join-Path $evidenceRoot 'candidate-aar-audit.json')
if ($LASTEXITCODE -ne 0) { throw 'Candidate fails NT1 artifact gates' }
Write-Output "Candidate ready for independent review: $candidate"
Write-Output 'No app dependency was changed and no voice model was downloaded.'
