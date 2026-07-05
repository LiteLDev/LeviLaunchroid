param(
    [string]$Abi = "arm64-v8a",
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

$Ndk = Resolve-NdkPath $Ndk
$Toolchain = Join-Path $Ndk "build\cmake\android.toolchain.cmake"
if (-not (Test-Path $Toolchain)) {
    throw "Android NDK toolchain not found: $Toolchain"
}

$BuildDir = Join-Path $SourceDir "build\android-$Abi-$BuildType"
if ($Clean -and (Test-Path $BuildDir)) {
    Remove-Item -LiteralPath $BuildDir -Recurse -Force
}

$LinkPreloader = if ($NoLinkPreloader) { "OFF" } else { "ON" }

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

Invoke-Checked cmake `
    -S $SourceDir `
    -B $BuildDir `
    -G Ninja `
    "-DCMAKE_TOOLCHAIN_FILE=$Toolchain" `
    "-DANDROID_ABI=$Abi" `
    "-DANDROID_PLATFORM=android-24" `
    "-DCMAKE_BUILD_TYPE=$BuildType" `
    "-DPRELOADER_ANDROID_ROOT=$PreloaderRoot" `
    "-DLL_MOD_MENU_EXAMPLES_LINK_PRELOADER=$LinkPreloader"

Invoke-Checked cmake `
    --build $BuildDir `
    --target `
    modmenu_cpp_lifecycle

$OutDir = Join-Path $BuildDir "out\$Abi"
$DistDir = Join-Path $SourceDir "dist\$Abi"
if (Test-Path $DistDir) {
    Remove-Item -LiteralPath $DistDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

$Packages = @(
    @{
        Name = "cpp-lifecycle"
        Library = "libmodmenu_cpp_lifecycle.so"
        Manifest = "manifests\cpp-lifecycle\manifest.json"
    }
)

foreach ($Package in $Packages) {
    $PackageDir = Join-Path $DistDir $Package.Name
    New-Item -ItemType Directory -Force -Path $PackageDir | Out-Null
    Copy-Item -LiteralPath (Join-Path $OutDir $Package.Library) -Destination $PackageDir
    Copy-Item -LiteralPath (Join-Path $SourceDir $Package.Manifest) -Destination (Join-Path $PackageDir "manifest.json")
}

Write-Host "Built examples:"
Get-ChildItem -Path $DistDir -Recurse -Filter *.so | ForEach-Object {
    Write-Host "  $($_.FullName)"
}
