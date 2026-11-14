[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$SdkRoot,
    [ValidateRange(1, 8)][int]$Jobs = 2,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).Replace('\', '/')
$nativeRoot = "$repoRoot/.native"
$archives = "$nativeRoot/archives"
$sourceRoot = "$nativeRoot/ort-source/onnxruntime-1.28.2"
$buildRoot = "$nativeRoot/ort-build-arm64-2"
$installRoot = "$nativeRoot/ort-install"
$evidence = "$repoRoot/app/build/nt1-binary-evidence"
$sdk = [IO.Path]::GetFullPath($SdkRoot).Replace('\', '/')
$ndk = "$sdk/ndk/27.1.12297006"
$llvm = "$ndk/toolchains/llvm/prebuilt/windows-x86_64/bin"
$prepare = "$PSScriptRoot/neural-tts/prepare-source.py"
foreach ($directory in @($archives, "$installRoot/lib", "$installRoot/include", $evidence)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

function Get-PinnedFile([string]$Name, [string]$Url, [string]$Hash) {
    $destination = "$archives/$Name"
    if (-not (Test-Path -LiteralPath $destination)) {
        if ($Offline) { throw "Offline build is missing $Name" }
        Invoke-WebRequest -Uri $Url -OutFile $destination
    }
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Hash) {
        throw "SHA-256 mismatch: $Name"
    }
    return $destination
}

$sourceZip = Get-PinnedFile 'onnxruntime-source-v1.28.2.zip' `
    'https://codeload.github.com/microsoft/onnxruntime/zip/refs/tags/v1.28.2' `
    '0a71177b993c8406e0ae50574e2319bd0f53d3e89857cd9acdde0aea22ba27f1'
$cmakeZip = Get-PinnedFile 'cmake-3.31.6-windows-x86_64.zip' `
    'https://github.com/Kitware/CMake/releases/download/v3.31.6/cmake-3.31.6-windows-x86_64.zip' `
    'd163cd3ab4959b0a53fa8988f2ddbd2e6c501658201e6a154386bad9dbe4f836'
$protocZip = Get-PinnedFile 'protoc-21.12-win64.zip' `
    'https://github.com/protocolbuffers/protobuf/releases/download/v21.12/protoc-21.12-win64.zip' `
    '71852a30cf62975358edfcbbff93086e8857a079c8e4d6904881aa968d65c7f9'

foreach ($entry in @(
    @($sourceZip, $sourceRoot, 1),
    @($cmakeZip, "$nativeRoot/tools/cmake-3.31.6-windows-x86_64", 1),
    @($protocZip, "$nativeRoot/tools/protoc-21.12-win64", 0)
)) {
    & python $prepare $entry[0] $entry[1] --pristine --strip-components $entry[2]
    if ($LASTEXITCODE -ne 0) { throw "Archive preparation failed: $($entry[0])" }
}

$cmake = "$nativeRoot/tools/cmake-3.31.6-windows-x86_64/bin/cmake.exe"
$flags = '-ffile-prefix-map="{0}=tsuzuki" -ffile-prefix-map="{1}=android-sdk"' -f $repoRoot, $sdk
$pythonPath = (Get-Command python -CommandType Application | Select-Object -First 1).Source.Replace('\', '/')
$patchPath = (Get-Command git -CommandType Application | Select-Object -First 1).Source
$patchPath = [IO.Path]::GetFullPath((Join-Path (Split-Path $patchPath) '../usr/bin/patch.exe')).Replace('\', '/')
if (-not (Test-Path -LiteralPath $patchPath)) { throw 'Git for Windows patch.exe is required' }
$arguments = @(
    '-S', "$sourceRoot/cmake", '-B', $buildRoot, '-G', 'Ninja',
    "-DCMAKE_MAKE_PROGRAM=$sdk/cmake/3.22.1/bin/ninja.exe",
    "-DCMAKE_TOOLCHAIN_FILE=$ndk/build/cmake/android.toolchain.cmake",
    '-DANDROID_ABI=arm64-v8a', '-DANDROID_PLATFORM=android-26', '-DANDROID_STL=c++_static',
    '-DCMAKE_BUILD_TYPE=Release', '-DCMAKE_POSITION_INDEPENDENT_CODE=ON',
    '-DCMAKE_EXPORT_COMPILE_COMMANDS=ON', '-DCMAKE_TLS_VERIFY=ON', '-DCMAKE_DISABLE_FIND_PACKAGE_Git=ON',
    "-DCMAKE_C_FLAGS=$flags", "-DCMAKE_CXX_FLAGS=$flags", "-DCMAKE_ASM_FLAGS=$flags",
    "-DPython_EXECUTABLE=$pythonPath", "-DPatch_EXECUTABLE=$patchPath",
    "-DONNX_CUSTOM_PROTOC_EXECUTABLE=$nativeRoot/tools/protoc-21.12-win64/bin/protoc.exe",
    '-Donnxruntime_BUILD_UNIT_TESTS=OFF', '-Donnxruntime_BUILD_BENCHMARKS=OFF',
    '-Donnxruntime_BUILD_SHARED_LIB=OFF', '-Donnxruntime_ENABLE_PYTHON=OFF', '-Donnxruntime_ENABLE_TRAINING=OFF',
    "-DFETCHCONTENT_FULLY_DISCONNECTED=$($Offline.IsPresent.ToString().ToUpperInvariant())"
)
& $cmake @arguments *> "$evidence/ort-configure-final.log"
if ($LASTEXITCODE -ne 0) { throw 'ONNX source configure failed; see ort-configure-final.log' }
# ORT defers some external link dependencies until a final shared/executable link.
# Our standalone static bundle must request RE2 explicitly as well as the default targets.
& $cmake --build $buildRoot --target all re2 --parallel $Jobs *> "$evidence/ort-build.log"
if ($LASTEXITCODE -ne 0) { throw 'ONNX source build failed; see ort-build.log' }

# Combine compiled Android object archives; never rewrite linked ELF strings.
$parts = @(Get-ChildItem -LiteralPath $buildRoot -Recurse -File -Filter '*.a' |
    Where-Object { $_.FullName -notmatch 'CMakeScratch|CMakeTmp|subbuild' } | Sort-Object FullName)
if ($parts.Count -lt 8) { throw 'Expected ONNX component and dependency archives' }
$commands = @('CREATE ../ort-install/lib/libonnxruntime.a')
foreach ($part in $parts) {
    $relative = $part.FullName.Substring($buildRoot.Length + 1).Replace('\', '/')
    if ($relative -match '\s') { throw "Unsupported whitespace in component path: $relative" }
    $commands += "ADDLIB $relative"
}
$commands += @('SAVE', 'END')
Push-Location $buildRoot
try {
    ($commands -join "`n") | & "$llvm/llvm-ar.exe" -M
    if ($LASTEXITCODE -ne 0) { throw 'ONNX archive combination failed' }
} finally { Pop-Location }
foreach ($directory in @('core/session', 'core/framework', 'core/providers/cpu')) {
    Get-ChildItem -LiteralPath "$sourceRoot/include/onnxruntime/$directory" -Filter '*.h' -File |
        Copy-Item -Destination "$installRoot/include"
}
Copy-Item -LiteralPath "$sourceRoot/LICENSE", "$sourceRoot/ThirdPartyNotices.txt" -Destination $installRoot
$record = [ordered]@{
    source_archive_sha256 = (Get-FileHash -LiteralPath $sourceZip -Algorithm SHA256).Hash.ToLowerInvariant()
    library_sha256 = (Get-FileHash -LiteralPath "$installRoot/lib/libonnxruntime.a" -Algorithm SHA256).Hash.ToLowerInvariant()
    components = @($parts | ForEach-Object {
        [ordered]@{ path = $_.FullName.Substring($buildRoot.Length + 1).Replace('\', '/'); sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
    })
}
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$installRoot/build.json" -Encoding utf8
Write-Output "ONNX source archive ready: $installRoot"
