param(
    [Parameter(Mandatory)][string]$OutputRoot,
    [string]$WorkspaceRoot = [IO.Path]::GetFullPath("$PSScriptRoot/../../.."),
    [string]$PolicyPath = "$PSScriptRoot/release-policy.json",
    [string]$AuiReviewPath
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Import-Module "$PSScriptRoot/ReleaseInputs.psm1" -Force
$policy = Read-ReleasePolicy $PolicyPath
$output = Resolve-ReleaseDirectory $OutputRoot
if (Test-Path -LiteralPath $output) { throw 'A fresh release preparation directory is required' }

function Git([string]$Project, [string[]]$Arguments) {
    $start = [Diagnostics.ProcessStartInfo]::new('git.exe')
    $start.UseShellExecute=$false; $start.CreateNoWindow=$true
    $start.RedirectStandardOutput=$true; $start.RedirectStandardError=$true
    $start.StandardOutputEncoding=[Text.Encoding]::UTF8; $start.StandardErrorEncoding=[Text.Encoding]::UTF8
    foreach ($a in @('-C',$Project)+$Arguments) { [void]$start.ArgumentList.Add($a) }
    $p=[Diagnostics.Process]::Start($start)
    try {
        $stdout=$p.StandardOutput.ReadToEndAsync(); $stderr=$p.StandardError.ReadToEndAsync()
        if (!$p.WaitForExit(10000)) { $p.Kill(); throw 'Git read timeout' }
        $text=$stdout.GetAwaiter().GetResult(); $errorText=$stderr.GetAwaiter().GetResult()
        if ($p.ExitCode) { throw "Git read failed: $errorText" }
        $text.TrimEnd()
    } finally { $p.Dispose() }
}

$review = @()
if ($AuiReviewPath) { $review = @(Get-Content -LiteralPath $AuiReviewPath -Encoding UTF8 -Raw | ConvertFrom-Json) }
$bindings=[Collections.Generic.List[object]]::new()
foreach ($repository in $policy.repositories.PSObject.Properties) {
    $project=[IO.Path]::GetFullPath((Join-Path $WorkspaceRoot $repository.Name))
    foreach ($target in $policy.targets.PSObject.Properties) {
        $branch=$target.Name
        $head=Git $project @('rev-parse',"refs/heads/$branch")
        $tree=Git $project @('rev-parse',"$head`^{tree}")
        $properties=Read-ReleaseProperties (Git $project @('show',"${head}:gradle.properties"))
        foreach ($key in @('loader_type','minecraft_version','mod_id','mod_version')) { if (!$properties[$key]) { throw "Missing source property $key in $project $branch" } }
        $buildScript=Git $project @('show',"${head}:build.gradle")
        $groupScript=$buildScript
        if ($buildScript.Contains('aui-validation-publication.gradle')) {
            $publication=Git $project @('show',"${head}:gradle/aui-validation-publication.gradle")
            if (!$publication.Contains('banira.validationOutputRoot')) { throw "Publication helper is not isolated: $project $branch" }
            $groupScript+="`n$publication"
        }
        try { $group=Read-ReleaseGroup $properties $groupScript } catch { throw "Maven identity unavailable for $project $branch : $($_.Exception.Message)" }
        if (!$buildScript.Contains('aui-validation-output.gradle')) { throw "Archive output isolation is not wired: $project $branch" }
        $isolation=Git $project @('show',"${head}:gradle/aui-validation-output.gradle")
        if (!$isolation.Contains('banira.validationOutputRoot')) { throw "Archive output isolation contract missing: $project $branch" }
        $metadata=if($properties.loader_type -eq 'fabric'){'fabric.mod.json'}elseif($properties.loader_type -eq 'neoforge'){'META-INF/neoforge.mods.toml'}else{'META-INF/mods.toml'}
        $metadataPath="src/main/resources/$metadata"
        $text=Git $project @('show',"${head}:$metadataPath")
        [void](Test-ReleaseVersionTemplate $text $buildScript)
        if ($repository.Name -ne 'BaniraCodex' -and !$text.Contains('${banira_version_range}')) { throw "Common-library dependency is not parameterized: $project $branch" }
        $source=[pscustomobject]@{repository=$repository.Name;project=$project;branch=$branch;head=$head;sourceTree=$tree;loader=$properties.loader_type;minecraft=$properties.minecraft_version;modId=$properties.mod_id;group=$group;sourceVersion=$properties.mod_version;metadataPath=$metadata;metadataBlob=(Git $project @('rev-parse',"${head}:$metadataPath"))}
        $binding=New-ReleaseBinding $source $policy (Join-Path $output 'artifacts')
        $key=$branch.Split('/')[0]+$branch.Split('/')[1].Split('.')[0]
        $repoKey=if($repository.Name -eq 'BaniraCodex'){'B'}elseif($repository.Name -eq 'SakuraSignIn_MC'){'S'}else{''}
        $pending=@(foreach($record in $review|Where-Object { $_.key -eq $key -and $_.repository -eq $repoKey }){
            $ancestor=Git $project @('merge-base',$record.head,$head)
            if($ancestor -ne $record.head){$record}
        })
        $engine=@($review|Where-Object { $_.key -eq $key -and $_.repository -eq 'B' })
        $binding|Add-Member -NotePropertyName pendingAuiSource -NotePropertyValue @($pending|ForEach-Object {@{head=$_.head;candidate=$_.candidate}}) -Force
        $binding|Add-Member -NotePropertyName reviewedAuiStatus -NotePropertyValue @($engine|ForEach-Object {if($_.PSObject.Properties['auiRuntime']){$_.auiRuntime}else{'PREVIOUSLY_ACCEPTED'}})
        $binding|Add-Member -NotePropertyName sourceSelectionReady -NotePropertyValue ($pending.Count -eq 0) -Force
        $bindings.Add($binding)
    }
}
foreach ($binding in $bindings) {
    if ((Git $binding.repositoryRoot @('rev-parse',"refs/heads/$($binding.branch)")) -ne $binding.head) { throw 'Maintenance reference changed during preparation' }
}
if (@($bindings|Group-Object coordinate|Where-Object Count -ne 1).Count) { throw 'Duplicate release coordinate' }
[void][IO.Directory]::CreateDirectory($output)
$manifest=[ordered]@{schema=1;version=$policy.version;baniraVersion=$policy.baniraVersion;policySha256=(Get-ReleaseHash $PolicyPath);createdUtc=[DateTime]::UtcNow.ToString('o');scope='Four repositories, thirteen maintenance targets; release input preparation only';sourceCount=$bindings.Count;verifiedReleasePackages=0;pendingAuiSources=@($bindings|Where-Object {!$_.sourceSelectionReady}).Count;releaseReady=$false;bindings=$bindings.ToArray()}
if ($AuiReviewPath) { $manifest.auiReview=@{path=[IO.Path]::GetFullPath($AuiReviewPath);sha256=(Get-ReleaseHash $AuiReviewPath);scope='Historical local review, not a new upstream availability check'} }
$manifest|ConvertTo-Json -Depth 16|Set-Content -LiteralPath (Join-Path $output 'release-inputs.json') -Encoding UTF8
Write-Output "Prepared $($bindings.Count) unique release coordinates; verified packages=0; pending AUI source selections=$($manifest.pendingAuiSources). No source, Maven, IDEA or maintenance references changed."
