param(
    [string] $GeneratorPath
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$fontDirectory = Join-Path $projectRoot 'src\main\resources\assets\vanta\conversion'
$outputDirectory = Join-Path $projectRoot 'src\main\resources\assets\vanta\fonts'

if (-not $GeneratorPath) {
    $toolDirectory = Join-Path $env:TEMP 'vanta-msdf-atlas-gen-1.4'
    $GeneratorPath = Join-Path $toolDirectory 'msdf-atlas-gen\msdf-atlas-gen.exe'
    if (-not (Test-Path -LiteralPath $GeneratorPath)) {
        $archivePath = Join-Path $env:TEMP 'msdf-atlas-gen-1.4-win64.zip'
        Invoke-WebRequest `
            -Uri 'https://github.com/Chlumsky/msdf-atlas-gen/releases/download/v1.4/msdf-atlas-gen-1.4-win64.zip' `
            -OutFile $archivePath
        New-Item -ItemType Directory -Force -Path $toolDirectory | Out-Null
        Expand-Archive -LiteralPath $archivePath -DestinationPath $toolDirectory -Force
    }
}

if (-not (Test-Path -LiteralPath $GeneratorPath)) {
    throw "msdf-atlas-gen was not found at $GeneratorPath"
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

Get-ChildItem -LiteralPath $fontDirectory -File |
    Where-Object { $_.Extension -in '.otf', '.ttf' } |
    ForEach-Object {
        $resourceName = $_.BaseName.ToLowerInvariant()
        $characters = if ($resourceName -eq 'icons') { '[0xE900,0xE919]' } else { '[0x20,0xFF]' }
        & $GeneratorPath `
            -font $_.FullName `
            -fontname $_.BaseName `
            -type msdf `
            -size 48 `
            -pxrange 8 `
            -potr `
            -yorigin bottom `
            -chars $characters `
            -imageout (Join-Path $outputDirectory "$resourceName.png") `
            -json (Join-Path $outputDirectory "$resourceName.json")
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to generate the MSDF atlas for $($_.Name)"
        }
    }
