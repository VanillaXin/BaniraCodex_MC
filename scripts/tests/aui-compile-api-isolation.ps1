param(
    [Parameter(Mandatory)][string]$OutputRoot,
    [Parameter(Mandatory)][string]$MavenRoot,
    [Parameter(Mandatory)][string]$EvidenceRoot,
    [Parameter(Mandatory)][string]$Gradle,
    [switch]$DefaultModel
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$project = [IO.Path]::GetFullPath("$PSScriptRoot/../..")
$owned = [IO.Path]::GetFullPath("$OutputRoot/aui-compile-api")
$seed = $env:BANIRA_AUI_COMPILE_SEED
if (!$seed -or !(Test-Path -LiteralPath "$seed/api-52.jar")) { throw 'Generated declaration seed is required' }
if (Test-Path -LiteralPath $owned) { throw 'A fresh declaration output directory is required' }
[void][IO.Directory]::CreateDirectory($owned)
Copy-Item -LiteralPath "$seed/api-52.jar", "$seed/api-61.jar", "$seed/fixture" -Destination $owned -Recurse
$checks = [Collections.Generic.List[object]]::new()
function Check([string]$Name, [bool]$Passed) {
    $checks.Add(@{name=$Name;passed=$Passed})
    if (!$Passed) { throw "Assertion failed: $Name" }
}
function Invoke-Model([string]$Name, [string[]]$Extra, [string]$Failure = '') {
    $arguments = @('-p',$project,'--offline','--no-daemon','--max-workers=1',
        '-Dorg.gradle.jvmargs=-Xmx1024m','--console=plain',
        "-Pbanira.validationOutputRoot=$OutputRoot","-Dmaven.repo.local=$MavenRoot") + $Extra
    $output = & $Gradle @arguments 2>&1 | Out-String
    $code = $LASTEXITCODE
    [IO.File]::WriteAllText("$owned/$Name.log",$output,[Text.UTF8Encoding]::new($false))
    if ($Failure) {
        Check $Name ($code -ne 0 -and $output.Contains($Failure))
        return
    }
    Check $Name ($code -eq 0)
}
function Read-Model([string]$Name) {
    Get-Content -LiteralPath "$owned/$Name.json" -Encoding UTF8 -Raw | ConvertFrom-Json
}
function Check-Isolation([string]$Name, $Model, [string]$Selected) {
    foreach ($configuration in @('runtimeClasspath','testRuntimeClasspath','annotationProcessor','testAnnotationProcessor')) {
        Check "$Name-$configuration" (@($Model.paths.$configuration | Where-Object {
            $_ -match 'api-(52|61)\.jar|apricityui|apricity-ui'
        }).Count -eq 0)
    }
    if ($Selected) {
        Check "$Name-selected" (@($Model.configuration).Count -eq 1 -and
            [IO.Path]::GetFullPath($Model.configuration[0]) -eq $Selected -and
            @($Model.paths.compileClasspath | Where-Object { [IO.Path]::GetFullPath($_) -eq $Selected }).Count -eq 1 -and
            @($Model.paths.compileClasspath | Where-Object { $_ -match 'ApricityUI-forge-1\.20\.1' }).Count -eq 0)
    } else {
        Check 'control:default-official' (@($Model.configuration).Count -eq 0 -and
            @($Model.paths.compileClasspath | Where-Object { $_ -match 'ApricityUI-forge-1\.20\.1' }).Count -eq 1)
    }
}
function Mutate-Jar([string]$Name, [scriptblock]$Mutation) {
    $path = "$owned/$Name.jar"
    Copy-Item -LiteralPath "$owned/api-61.jar" -Destination $path
    $archive = [IO.Compression.ZipFile]::Open($path,[IO.Compression.ZipArchiveMode]::Update)
    try { & $Mutation $archive } finally { $archive.Dispose() }
    $path
}
try {
    $compiler = 'D:/Programs/Java/corretto-1.8.0_452/bin/javac.exe'
    $sources = @(Get-ChildItem -LiteralPath "$owned/fixture" -Recurse -Filter '*.java' -File | ForEach-Object FullName)
    Check 'control:fixture-sources' ($sources.Count -eq 3)
    [void][IO.Directory]::CreateDirectory("$owned/fixture-classes")
    $compilerOutput = & $compiler '-J-Duser.language=en' '-J-Duser.country=US' '-encoding' 'UTF-8' '-classpath' "$owned/api-52.jar" '-d' "$owned/fixture-classes" @sources 2>&1 | Out-String
    $compilerExit = $LASTEXITCODE
    [IO.File]::WriteAllText("$owned/java8-compile.log",$compilerOutput,[Text.UTF8Encoding]::new($false))
    Check 'java8-signature-compilation' ($compilerExit -eq 0)
    $classes = @(Get-ChildItem -LiteralPath "$owned/fixture-classes" -Recurse -File -Filter '*.class')
    Check 'java8-fixture-major52' ($classes.Count -eq 3 -and @($classes | Where-Object {
        $bytes = [IO.File]::ReadAllBytes($_.FullName)
        $bytes[6] -ne 0 -or $bytes[7] -ne 52
    }).Count -eq 0)
    Invoke-Model 'default' @("-Pbanira.auiCompileApiModel=$owned/default.json",'auiCompileApiModel')
    Check-Isolation 'default' (Read-Model 'default') ''
    foreach ($major in @(61,52)) {
        $selected = [IO.Path]::GetFullPath("$owned/api-$major.jar")
        $tasks = @('auiCompileApiModel')
        if ($major -eq 61) { $tasks = @('compileJava','auiCompileApiModel') }
        Invoke-Model "selected-$major" (@("-Pbanira.auiCompileApi=$selected","-Pbanira.auiCompileApiModel=$owned/selected-$major.json") + $tasks)
        Check-Isolation "selected-$major" (Read-Model "selected-$major") $selected
    }
    $wrongMajor = Mutate-Jar 'wrong-major' {
        param($zip)
        $entry = @($zip.Entries | Where-Object FullName -like '*.class')[0]
        $name = $entry.FullName
        $stream = $entry.Open(); $memory = [IO.MemoryStream]::new()
        try { $stream.CopyTo($memory); $bytes = $memory.ToArray() } finally { $stream.Dispose(); $memory.Dispose() }
        $bytes[6] = 0; $bytes[7] = 62; $entry.Delete()
        $stream = $zip.CreateEntry($name).Open()
        try { $stream.Write($bytes,0,$bytes.Length) } finally { $stream.Dispose() }
    }
    Invoke-Model 'reject-wrong-major' @("-Pbanira.auiCompileApi=$wrongMajor",'verifyAuiCompileApi') 'Declaration major or hierarchy mismatch'
    $missing = Mutate-Jar 'missing-symbol' {
        param($zip)
        $entry = @($zip.Entries | Where-Object FullName -like '*/Document.class')
        if ($entry.Count -ne 1) { throw 'Expected one Document declaration' }
        $entry[0].Delete()
    }
    Invoke-Model 'reject-missing-symbol' @("-Pbanira.auiCompileApi=$missing",'verifyAuiCompileApi') 'Unexpected declaration symbols'
    $unmarked = Mutate-Jar 'unmarked' { param($zip) $zip.GetEntry('META-INF/banira-aui-compile-api.properties').Delete() }
    Invoke-Model 'reject-unmarked' @("-Pbanira.auiCompileApi=$unmarked",'verifyAuiCompileApi') 'Unmarked or untraced AUI declarations'
    $outside = "$EvidenceRoot/unowned-api.jar"
    Copy-Item -LiteralPath "$owned/api-61.jar" -Destination $outside
    Invoke-Model 'reject-unowned' @("-Pbanira.auiCompileApi=$outside",'verifyAuiCompileApi') 'AUI declarations require an owned absolute path'
    foreach ($configuration in @('runtimeOnly','testRuntimeOnly')) {
        $init = "$owned/$configuration.init.gradle"
        $declaration = [IO.Path]::GetFullPath("$owned/api-61.jar").Replace('\','/')
        $text = "gradle.projectsEvaluated { rootProject.dependencies.add('$configuration', rootProject.files('$declaration')) }"
        [IO.File]::WriteAllText($init,$text,[Text.UTF8Encoding]::new($false))
        $resolved = if ($configuration -eq 'runtimeOnly') { 'runtimeClasspath' } else { 'testRuntimeClasspath' }
        Invoke-Model "reject-$configuration" @("-Pbanira.auiCompileApi=$declaration",'--init-script',$init,'verifyAuiCompileApi') "AUI declarations leaked into $resolved"
    }
} finally {
    @{kind='assertions';checks=@($checks.ToArray())} | ConvertTo-Json -Depth 6 |
        Set-Content -LiteralPath "$EvidenceRoot/assertions.json" -Encoding UTF8
}
