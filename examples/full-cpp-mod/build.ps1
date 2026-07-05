param(
    [string]$BuildType = "Release",
    [string]$Ndk = "",
    [string]$PreloaderRoot = "",
    [switch]$Clean,
    [switch]$NoLinkPreloader
)

$ErrorActionPreference = "Stop"

$SourceDir = $PSScriptRoot
$RepoRoot = Resolve-Path (Join-Path $SourceDir "..\..")
if ([string]::IsNullOrWhiteSpace($PreloaderRoot)) {
    $PreloaderRoot = Join-Path $RepoRoot "app\src\main\cpp\preloader"
}

$Abi = "arm64-v8a"

function Get-FullPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return [System.IO.Path]::GetFullPath($Path)
}

function Resolve-NdkPath {
    param([string]$ExplicitNdk)

    $Candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($ExplicitNdk)) {
        $Candidates += $ExplicitNdk
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_NDK_HOME)) {
        $Candidates += $env:ANDROID_NDK_HOME
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_NDK_ROOT)) {
        $Candidates += $env:ANDROID_NDK_ROOT
    }

    $SdkRoots = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT) |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique
    foreach ($SdkRoot in $SdkRoots) {
        $NdkRoot = Join-Path $SdkRoot "ndk"
        if (Test-Path $NdkRoot) {
            Get-ChildItem -LiteralPath $NdkRoot -Directory |
                Sort-Object Name -Descending |
                ForEach-Object { $Candidates += $_.FullName }
        }

        $NdkBundle = Join-Path $SdkRoot "ndk-bundle"
        if (Test-Path $NdkBundle) {
            $Candidates += $NdkBundle
        }
    }

    foreach ($Candidate in ($Candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)) {
        $Toolchain = Join-Path $Candidate "build\cmake\android.toolchain.cmake"
        if (Test-Path $Toolchain) {
            return Get-FullPath $Candidate
        }
    }

    throw "Android NDK not found. Pass -Ndk or set ANDROID_NDK_HOME, ANDROID_NDK_ROOT, ANDROID_HOME, or ANDROID_SDK_ROOT."
}

function Assert-UnderDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Root
    )

    $FullPath = Get-FullPath $Path
    $FullRoot = Get-FullPath $Root
    if (-not $FullRoot.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $FullRoot = $FullRoot + [System.IO.Path]::DirectorySeparatorChar
    }

    if (-not $FullPath.StartsWith($FullRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside ${FullRoot}: $FullPath"
    }
}

function Remove-DirectoryIfExists {
    param([Parameter(Mandatory = $true)][string]$Path)
    Assert-UnderDirectory -Path $Path -Root $SourceDir
    if (Test-Path $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath failed with exit code $LASTEXITCODE"
    }
}

$Ndk = Resolve-NdkPath $Ndk
$Toolchain = Join-Path $Ndk "build\cmake\android.toolchain.cmake"
if (-not (Test-Path $Toolchain)) {
    throw "Android NDK toolchain not found: $Toolchain"
}

$HostBuildDir = Join-Path $SourceDir "build\host-$BuildType"
$AndroidBuildDir = Join-Path $SourceDir "build\android-$Abi-$BuildType"
$DistDir = Join-Path $SourceDir "dist\$Abi"

if ($Clean) {
    Remove-DirectoryIfExists $HostBuildDir
    Remove-DirectoryIfExists $AndroidBuildDir
}

Invoke-Checked cmake `
    -S $SourceDir `
    -B $HostBuildDir `
    -G Ninja `
    "-DCMAKE_BUILD_TYPE=$BuildType" `
    "-DPRELOADER_ANDROID_ROOT=$PreloaderRoot"

Invoke-Checked cmake `
    --build $HostBuildDir `
    --target full_cpp_mod_config_gen

$GeneratorCandidates = @(
    (Join-Path $HostBuildDir "full_cpp_mod_config_gen.exe"),
    (Join-Path $HostBuildDir "full_cpp_mod_config_gen")
)
$Generator = $GeneratorCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Generator) {
    throw "Config generator was not produced in $HostBuildDir"
}

$LinkPreloader = if ($NoLinkPreloader) { "OFF" } else { "ON" }

Invoke-Checked cmake `
    -S $SourceDir `
    -B $AndroidBuildDir `
    -G Ninja `
    "-DCMAKE_TOOLCHAIN_FILE=$Toolchain" `
    "-DANDROID_ABI=$Abi" `
    "-DANDROID_PLATFORM=android-24" `
    "-DCMAKE_BUILD_TYPE=$BuildType" `
    "-DPRELOADER_ANDROID_ROOT=$PreloaderRoot" `
    "-DLL_FULL_CPP_MOD_LINK_PRELOADER=$LinkPreloader"

Invoke-Checked cmake `
    --build $AndroidBuildDir `
    --target full_cpp_mod

Remove-DirectoryIfExists $DistDir
$PackageDir = Join-Path $DistDir "full-cpp-mod"
$ConfigDir = Join-Path $PackageDir "config"
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null

$LibraryPath = Join-Path $AndroidBuildDir "out\$Abi\libfull_cpp_mod.so"
if (-not (Test-Path $LibraryPath)) {
    throw "Built library not found: $LibraryPath"
}

Copy-Item -LiteralPath (Join-Path $SourceDir "manifest.json") `
    -Destination (Join-Path $PackageDir "manifest.json")
Copy-Item -LiteralPath $LibraryPath `
    -Destination (Join-Path $PackageDir "libfull_cpp_mod.so")

Invoke-Checked $Generator $PackageDir

$ArchivePath = Join-Path $DistDir "full-cpp-mod.levipack"
$TempZipPath = Join-Path $DistDir "full-cpp-mod.zip"
if (Test-Path $ArchivePath) {
    Remove-Item -LiteralPath $ArchivePath -Force
}
if (Test-Path $TempZipPath) {
    Remove-Item -LiteralPath $TempZipPath -Force
}

Compress-Archive -Path (Join-Path $PackageDir "*") -DestinationPath $TempZipPath -Force
Move-Item -LiteralPath $TempZipPath -Destination $ArchivePath -Force

Write-Host "Built full C++ mod example:"
Write-Host "  $PackageDir"
Write-Host "  $ArchivePath"
