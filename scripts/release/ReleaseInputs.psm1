Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-ReleaseCondition($Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
}

function Get-ReleaseHash([string]$Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Resolve-ReleaseDirectory([string]$Path) {
    Assert-ReleaseCondition ([IO.Path]::IsPathFullyQualified($Path)) 'An absolute output path is required'
    $full = [IO.Path]::GetFullPath($Path).TrimEnd([char[]]@('/','\'))
    Assert-ReleaseCondition ($full -and [IO.Path]::GetPathRoot($full).TrimEnd('\','/') -ne $full) 'A filesystem root is not an output directory'
    Assert-ReleaseCondition ($full -notmatch '(?i)(^|[\\/])(builds|\.m2|\.idea|\.run)([\\/]|$)') 'Protected output directory'
    $cursor = $full
    while ($cursor) {
        if (Test-Path -LiteralPath $cursor) {
            Assert-ReleaseCondition (!((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) 'Output path contains a reparse point'
        }
        $parent = Split-Path -Parent $cursor
        if ($parent -eq $cursor) { break }
        $cursor = $parent
    }
    $full
}

function Read-ReleaseProperties([string]$Text) {
    $result = @{}
    foreach ($line in $Text -split '\r?\n') {
        if ($line.TrimStart().StartsWith('#') -or !$line.Trim()) { continue }
        $parts = $line.Split('=', 2)
        if ($parts.Count -ne 2) { continue }
        $key = $parts[0].Trim()
        Assert-ReleaseCondition (!$result.ContainsKey($key)) "Duplicate property: $key"
        $result[$key] = $parts[1].Trim()
    }
    $result
}

function Read-ReleasePolicy([string]$Path) {
    $policy = Get-Content -LiteralPath $Path -Encoding UTF8 -Raw | ConvertFrom-Json
    Assert-ReleaseCondition ($policy.schema -eq 1) 'Unsupported release policy'
    foreach ($value in @($policy.version, $policy.baniraVersion, $policy.baniraNextMajor)) {
        Assert-ReleaseCondition ($value -match '^\d+\.\d+\.\d+$') 'Stable release versions are required'
    }
    Assert-ReleaseCondition ($policy.version -eq $policy.baniraVersion -and [version]$policy.baniraNextMajor -gt [version]$policy.baniraVersion) 'Inconsistent common-library versions'
    $policy
}

function Read-ReleaseGroup([hashtable]$Properties, [string]$BuildScript) {
    if ($Properties.ContainsKey('mod_group_id')) { return $Properties.mod_group_id }
    $literal = [regex]::Matches($BuildScript, '(?m)^\s*group\s*=\s*[''"](?<group>[a-zA-Z0-9_.]+)[''"]\s*$')
    if (!$literal.Count) { $literal = [regex]::Matches($BuildScript, '(?m)^\s*groupId\s*=\s*[''"](?<group>[a-zA-Z0-9_.]+)[''"]\s*$') }
    $groups = @($literal | ForEach-Object { $_.Groups['group'].Value } | Sort-Object -Unique)
    Assert-ReleaseCondition ($groups.Count -eq 1) 'A unique literal or property-based Maven group is required'
    $groups[0]
}

function Test-ReleaseVersionTemplate([string]$Metadata, [string]$BuildScript) {
    if ($Metadata.Contains('${mod_version}')) { return $true }
    $derived = $Metadata.Contains('${version}') -and $BuildScript -match '(?m)^\s*version\s*=\s*(project\.)?mod_version\s*$' -and $BuildScript -match '[''"]version[''"]\s*:\s*project\.version'
    Assert-ReleaseCondition $derived 'Release version is not parameterized from mod_version'
    $true
}

function New-ReleaseBinding($Source, $Policy, [string]$OutputRoot) {
    $root = Resolve-ReleaseDirectory $OutputRoot
    $ids = $Policy.repositories.PSObject.Properties
    $repo = @($ids | Where-Object Name -eq $Source.repository)
    $target = @($Policy.targets.PSObject.Properties | Where-Object Name -eq $Source.branch)
    Assert-ReleaseCondition ($repo.Count -eq 1 -and $target.Count -eq 1) 'Unsupported repository or maintenance branch'
    Assert-ReleaseCondition ($Source.head -match '^[0-9a-f]{40}$') 'A full source commit is required'
    Assert-ReleaseCondition ($Source.minecraft -eq $target[0].Value -and $Source.loader -eq $Source.branch.Split('/')[0]) 'Source loader or Minecraft version mismatch'
    Assert-ReleaseCondition ($Source.modId -eq $repo[0].Value -and $Source.group -match '^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$') 'Source Maven identity mismatch'
    $key = $Source.branch.Replace('/','-')
    $version = "$($Source.loader)-$($Source.minecraft)-$($Policy.version)"
    $directory = Join-Path $root "$key/$($Source.repository)"
    $maven = Join-Path $root "$key/maven"
    $worktree = Join-Path $root "sources/$key/$($Source.repository)"
    $range = if ($Source.loader -eq 'fabric') { ">=$($Policy.baniraVersion) <$($Policy.baniraNextMajor)" } else { "[$($Policy.baniraVersion),$($Policy.baniraNextMajor))" }
    $arguments = @('--project-dir',$worktree,'--offline','--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx1024m',"-Dmaven.repo.local=$maven", "-Pmod_version=$($Policy.version)", "-Pbanira.validationOutputRoot=$directory")
    if ($Source.repository -ne 'BaniraCodex') { $arguments += @("-Pbanira_version=$($Policy.baniraVersion)","-Pbanira_version_range=$range") }
    [pscustomobject]@{
        repository=$Source.repository; repositoryRoot=$Source.project; project=$worktree; branch=$Source.branch; head=$Source.head
        loader=$Source.loader; minecraft=$Source.minecraft; modId=$Source.modId; group=$Source.group
        sourceVersion=$Source.sourceVersion; version=$Policy.version; publicationVersion=$version
        coordinate="$($Source.group):$($Source.modId):$version"; baniraVersion=$Policy.baniraVersion; baniraRange=$range
        mavenRoot=$maven; outputRoot=$directory; gradleArguments=$arguments
        sourceTree=$Source.sourceTree; metadataPath=$Source.metadataPath; metadataBlob=$Source.metadataBlob
        packageVerified=$false; ideaActivated=$false; sourceSelectionReady=$true; pendingAuiSource=@()
    }
}

function Get-ReleaseArtifactPaths($Binding) {
    $directory = Join-Path $Binding.mavenRoot ($Binding.group.Replace('.','/') + "/$($Binding.modId)/$($Binding.publicationVersion)")
    $stem = "$($Binding.modId)-$($Binding.publicationVersion)"
    [pscustomobject]@{ jar=Join-Path $directory "$stem.jar"; pom=Join-Path $directory "$stem.pom"; sources=Join-Path $directory "$stem-sources.jar"; receipt=Join-Path $directory "$stem.release-build.json" }
}

function Read-ReleaseZipText($Zip, [string]$Name) {
    $entry = $Zip.GetEntry($Name)
    Assert-ReleaseCondition ($null -ne $entry -and $entry.Length -le 2097152) "Missing or oversized metadata: $Name"
    $reader = [IO.StreamReader]::new($entry.Open(), [Text.Encoding]::UTF8)
    try { $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Test-ReleaseArtifact($Binding) {
    Assert-ReleaseCondition ($Binding.version -match '^\d+\.\d+\.\d+$' -and $Binding.publicationVersion -eq "$($Binding.loader)-$($Binding.minecraft)-$($Binding.version)" -and $Binding.coordinate -eq "$($Binding.group):$($Binding.modId):$($Binding.publicationVersion)") 'Invalid release binding identity'
    $paths = Get-ReleaseArtifactPaths $Binding
    foreach ($path in @($paths.jar,$paths.pom,$paths.sources,$paths.receipt)) {
        [void](Resolve-ReleaseDirectory (Split-Path -Parent $path))
        Assert-ReleaseCondition (Test-Path -LiteralPath $path -PathType Leaf) "Release artifact is not ready: $path"
        Assert-ReleaseCondition (!((Get-Item -LiteralPath $path).Attributes -band [IO.FileAttributes]::ReparsePoint)) 'Release artifact is a reparse point'
    }
    $receipt = Get-Content -LiteralPath $paths.receipt -Encoding UTF8 -Raw | ConvertFrom-Json
    Assert-ReleaseCondition ($receipt.schema -eq 1 -and $receipt.sourceCommit -eq $Binding.head -and $receipt.sourceTree -eq $Binding.sourceTree -and $receipt.coordinate -eq $Binding.coordinate) 'Build receipt source/coordinate mismatch'
    foreach ($name in @('jar','pom','sources')) {
        Assert-ReleaseCondition ((Get-ReleaseHash $paths.$name) -eq $receipt.hashes.$name) "Release artifact hash mismatch: $name"
    }
    $settings = [Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
    $reader = [Xml.XmlReader]::Create($paths.pom, $settings)
    try { $pom=[Xml.XmlDocument]::new(); $pom.Load($reader) } finally { $reader.Dispose() }
    $namespaces = [Xml.XmlNamespaceManager]::new($pom.NameTable)
    $namespaces.AddNamespace('m','http://maven.apache.org/POM/4.0.0')
    $project = $pom.SelectSingleNode('/m:project', $namespaces)
    Assert-ReleaseCondition ($null -ne $project) 'A namespaced Maven POM is required'
    foreach ($pair in @(@('groupId',$Binding.group),@('artifactId',$Binding.modId),@('version',$Binding.publicationVersion))) {
        $node = $project.SelectSingleNode("m:$($pair[0])", $namespaces)
        Assert-ReleaseCondition ($node -and $node.InnerText -eq $pair[1]) "POM identity mismatch: $($pair[0])"
    }
    $dependencies = @($project.SelectNodes('m:dependencies/m:dependency', $namespaces) | Where-Object { $_.SelectSingleNode('m:groupId',$namespaces).InnerText -eq 'xin.vanilla.banira' -and $_.SelectSingleNode('m:artifactId',$namespaces).InnerText -eq 'banira_codex' })
    if ($Binding.repository -ne 'BaniraCodex') {
        $commonVersion = "$($Binding.loader)-$($Binding.minecraft)-$($Binding.baniraVersion)"
        Assert-ReleaseCondition ($dependencies.Count -le 1) 'Duplicate POM common-library dependency'
        if ($dependencies.Count) { Assert-ReleaseCondition ($dependencies[0].SelectSingleNode('m:version',$namespaces).InnerText -eq $commonVersion) 'POM common-library dependency mismatch' }
        $commonJar = Join-Path $Binding.mavenRoot "xin/vanilla/banira/banira_codex/$commonVersion/banira_codex-$commonVersion.jar"
        Assert-ReleaseCondition ($receipt.baniraCoordinate -eq "xin.vanilla.banira:banira_codex:$commonVersion" -and (Test-Path -LiteralPath $commonJar) -and (Get-ReleaseHash $commonJar) -eq $receipt.baniraSha256) 'Built common-library input mismatch'
    }
    $zip = [IO.Compression.ZipFile]::OpenRead($paths.jar)
    try {
        $text = Read-ReleaseZipText $zip $Binding.metadataPath
        Assert-ReleaseCondition (!$text.Contains('${')) 'Unexpanded release metadata'
        if ($Binding.loader -eq 'fabric') {
            $metadata = $text | ConvertFrom-Json
            Assert-ReleaseCondition ($metadata.id -eq $Binding.modId -and $metadata.version -eq $Binding.version) 'Fabric mod version mismatch'
            if ($Binding.repository -ne 'BaniraCodex') { Assert-ReleaseCondition ($metadata.depends.banira_codex -eq $Binding.baniraRange) 'Fabric Banira range mismatch' }
        } else {
            Assert-ReleaseCondition ($text -match ('(?m)^\s*modId\s*=\s*"' + [regex]::Escape($Binding.modId) + '"') -and $text -match ('(?m)^\s*version\s*=\s*"' + [regex]::Escape($Binding.version) + '"')) 'Forge mod version mismatch'
            if ($Binding.repository -ne 'BaniraCodex') {
                $section = [regex]::Match($text, '(?ms)^\s*\[\[dependencies\.[^\r\n]+\]\]\s*\r?\n(?:(?!^\s*\[\[).)*?modId\s*=\s*"banira_codex"(?:(?!^\s*\[\[).)*').Value
                Assert-ReleaseCondition ($section -match ('versionRange\s*=\s*"' + [regex]::Escape($Binding.baniraRange) + '"')) 'Forge Banira range mismatch'
            }
        }
    } finally { $zip.Dispose() }
    $sources = [IO.Compression.ZipFile]::OpenRead($paths.sources)
    try { Assert-ReleaseCondition (@($sources.Entries | Where-Object FullName -Like '*.java').Count -gt 0) 'Empty source archive' } finally { $sources.Dispose() }
    [pscustomobject]@{ binding=$Binding; paths=$paths; hashes=$receipt.hashes; verified=$true }
}

function Write-ReleaseIdeaInput($Binding, [object[]]$Verified, [string]$Destination) {
    Assert-ReleaseCondition ($Binding.sourceSelectionReady) 'Pending source selection must be resolved before staging an IDEA input'
    $root = Resolve-ReleaseDirectory $Destination
    Assert-ReleaseCondition (!(Test-Path -LiteralPath $root)) 'IDEA staging directory must be fresh'
    $wanted = @($Binding)
    if ($Binding.repository -ne 'BaniraCodex') { $wanted += @('BaniraCodex') }
    $pins = foreach ($item in $wanted) {
        $name = if ($item -is [string]) {$item} else {$item.repository}
        $match = @($Verified | Where-Object { $_.verified -and $_.binding.repository -eq $name -and $_.binding.branch -eq $Binding.branch -and $_.binding.mavenRoot -eq $Binding.mavenRoot -and $_.binding.version -eq $Binding.version })
        Assert-ReleaseCondition ($match.Count -eq 1) "Verified same-target package missing: $name"
        $checked = Test-ReleaseArtifact $match[0].binding
        Assert-ReleaseCondition ($checked.hashes.jar -eq $match[0].hashes.jar -and $checked.hashes.pom -eq $match[0].hashes.pom -and $checked.hashes.sources -eq $match[0].hashes.sources) 'Verified package changed'
        $checked
    }
    [void][IO.Directory]::CreateDirectory($root)
    $pinPath = Join-Path $root 'input.json'
    @{binding=$Binding;packages=@($pins)} | ConvertTo-Json -Depth 14 | Set-Content -LiteralPath $pinPath -Encoding UTF8
    $initPath = Join-Path $root 'verify-input.gradle'
    $escapedPin = $pinPath.Replace('\','/').Replace("'","\'")
    $init = @'
import groovy.json.JsonSlurper
import java.security.MessageDigest
def input = new JsonSlurper().parse(new File('__PIN__'), 'UTF-8')
gradle.settingsEvaluated { settings ->
    def project = settings.settingsDir.canonicalFile
    if (project != new File(input.binding.project).canonicalFile) throw new GradleException('Release project mismatch')
    def git = { List args ->
        def process = new ProcessBuilder(['git', '-C', project.path] + args).redirectErrorStream(true).start()
        if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new GradleException('Git identity timeout') }
        def text = process.inputStream.getText('UTF-8').trim()
        if (process.exitValue() != 0) throw new GradleException('Git identity failed')
        text
    }
    if (git(['rev-parse','HEAD']) != input.binding.head || git(['branch','--show-current']) || git(['status','--porcelain','--untracked-files=no'])) throw new GradleException('Release source identity changed; use the pinned detached worktree')
    input.packages.each { p ->
        ['jar','pom','sources'].each { name ->
            def digest = MessageDigest.getInstance('SHA-256')
            new File(p.paths[name]).withInputStream { stream ->
                byte[] buffer = new byte[8192]
                int count
                while ((count = stream.read(buffer)) >= 0) { if (count > 0) digest.update(buffer, 0, count) }
            }
            def hash = digest.digest().encodeHex().toString()
            if (!hash.equalsIgnoreCase(p.hashes[name])) throw new GradleException('Release package identity changed')
        }
    }
}
'@
    $init.Replace('__PIN__',$escapedPin) | Set-Content -LiteralPath $initPath -Encoding UTF8
    $xml = [Xml.XmlDocument]::new()
    [void]$xml.AppendChild($xml.CreateXmlDeclaration('1.0','UTF-8',$null))
    $component = $xml.CreateElement('component'); $component.SetAttribute('name','ProjectRunConfigurationManager'); [void]$xml.AppendChild($component)
    $configuration = $xml.CreateElement('configuration'); $configuration.SetAttribute('default','false'); $configuration.SetAttribute('name',"$($Binding.branch) - $($Binding.modId) $($Binding.version)"); $configuration.SetAttribute('type','GradleRunConfiguration'); $configuration.SetAttribute('factoryName','Gradle'); [void]$component.AppendChild($configuration)
    $external=$xml.CreateElement('ExternalSystemSettings'); [void]$configuration.AppendChild($external)
    $args = @($Binding.gradleArguments) + @('--init-script',$initPath)
    $parameters = ($args | ForEach-Object {'"' + $_.Replace('\','/').Replace('"','\"') + '"'}) -join ' '
    foreach ($pair in @(@('externalProjectPath',$Binding.project),@('externalSystemIdString','GRADLE'),@('scriptParameters',$parameters))) {
        $option=$xml.CreateElement('option'); $option.SetAttribute('name',$pair[0]); $option.SetAttribute('value',$pair[1]); [void]$external.AppendChild($option)
    }
    $tasks=$xml.CreateElement('option'); $tasks.SetAttribute('name','taskNames'); [void]$external.AppendChild($tasks)
    $list=$xml.CreateElement('list'); [void]$tasks.AppendChild($list)
    $task=$xml.CreateElement('option'); $task.SetAttribute('value',':runClient'); [void]$list.AppendChild($task)
    $writerSettings=[Xml.XmlWriterSettings]::new(); $writerSettings.Encoding=[Text.UTF8Encoding]::new($false); $writerSettings.Indent=$true
    $writer=[Xml.XmlWriter]::Create((Join-Path $root 'Release_Client.xml'),$writerSettings)
    try { $xml.Save($writer) } finally { $writer.Dispose() }
    Join-Path $root 'Release_Client.xml'
}

Export-ModuleMember -Function Read-ReleasePolicy,Read-ReleaseProperties,Read-ReleaseGroup,Test-ReleaseVersionTemplate,New-ReleaseBinding,Get-ReleaseArtifactPaths,Get-ReleaseHash,Resolve-ReleaseDirectory,Test-ReleaseArtifact,Write-ReleaseIdeaInput
