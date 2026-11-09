param(
    [Parameter(Mandatory)][string]$OutputRoot,
    [Parameter(Mandatory)][string]$EvidenceRoot,
    [string]$MavenRoot,
    [string]$Gradle
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$project=[IO.Path]::GetFullPath("$PSScriptRoot/../..")
Import-Module "$project/scripts/release/ReleaseInputs.psm1" -Force
$policy=Read-ReleasePolicy "$project/scripts/release/release-policy.json"
$root=Resolve-ReleaseDirectory (Join-Path $OutputRoot 'release-input-tests')
if(Test-Path -LiteralPath $root){throw 'Fresh fixture directory required'}
[void][IO.Directory]::CreateDirectory($root)
$checks=[Collections.Generic.List[object]]::new()
function Check([string]$Label,[scriptblock]$Action){
    try { & $Action; $checks.Add(@{name=$Label;passed=$true}) }
    catch { $checks.Add(@{name=$Label;passed=$false;message=$_.Exception.Message}); throw }
}
function Need($Condition,[string]$Message){if(!$Condition){throw $Message}}
function Reject([scriptblock]$Action,[string]$Pattern){
    $caught=$null
    try { & $Action|Out-Null } catch { $caught=$_.Exception.Message }
    Need ($caught -and $caught -match $Pattern) "Expected rejection matching $Pattern; actual=$caught"
}
function Source([string]$Repository,[string]$Branch){
    $group=if($Repository -eq 'BaniraCodex'){'xin.vanilla.banira'}elseif($Repository -eq 'SakuraSignIn_MC'){'xin.vanilla.sakura'}elseif($Repository -eq 'AotakeSweep'){'xin.vanilla.aotake'}else{'xin.vanilla.narcissus'}
    $loader=$Branch.Split('/')[0]
    [pscustomobject]@{repository=$Repository;project=$project;branch=$Branch;head=('a'*40);sourceTree=('b'*40);loader=$loader;minecraft=$policy.targets.$Branch;modId=$policy.repositories.$Repository;group=$group;sourceVersion=if($Repository -eq 'BaniraCodex'){'1.0.4'}else{'2.0.0-alpha.3'};metadataPath=if($loader -eq 'fabric'){'fabric.mod.json'}elseif($loader -eq 'neoforge'){'META-INF/neoforge.mods.toml'}else{'META-INF/mods.toml'};metadataBlob=('c'*40)}
}
function Binding([string]$Repository,[string]$Branch,[string]$Case){New-ReleaseBinding (Source $Repository $Branch) $policy (Join-Path $root $Case)}
function ZipText([string]$Path,[hashtable]$Entries){
    [void][IO.Directory]::CreateDirectory((Split-Path -Parent $Path))
    $zip=[IO.Compression.ZipFile]::Open($Path,[IO.Compression.ZipArchiveMode]::Create)
    try { foreach($key in $Entries.Keys){$w=[IO.StreamWriter]::new($zip.CreateEntry($key).Open(),[Text.UTF8Encoding]::new($false));try{$w.Write($Entries[$key])}finally{$w.Dispose()}} }finally{$zip.Dispose()}
}
function Fixture($Binding,[string]$ModVersion=$Binding.version,[string]$DependencyRange=$Binding.baniraRange,[string]$PomDependency='',[string]$PomPrefix=''){
    $paths=Get-ReleaseArtifactPaths $Binding
    [void][IO.Directory]::CreateDirectory((Split-Path -Parent $paths.jar))
    if($Binding.loader -eq 'fabric'){
        $meta=@{schemaVersion=1;id=$Binding.modId;version=$ModVersion;depends=@{minecraft=$Binding.minecraft}}
        if($Binding.repository -ne 'BaniraCodex'){$meta.depends.banira_codex=$DependencyRange}
        $text=$meta|ConvertTo-Json -Depth 4
    }else{
        $text="modLoader=`"javafml`"`n[[mods]]`nmodId=`"$($Binding.modId)`"`nversion=`"$ModVersion`"`n"
        if($Binding.repository -ne 'BaniraCodex'){$text+="[[dependencies.$($Binding.modId)]]`nmodId=`"banira_codex`"`nversionRange=`"$DependencyRange`"`n"}
    }
    ZipText $paths.jar @{$Binding.metadataPath=$text}
    ZipText $paths.sources @{'example/Source.java'='class Source {}'}
    $pom="$PomPrefix<project xmlns=`"http://maven.apache.org/POM/4.0.0`"><modelVersion>4.0.0</modelVersion><groupId>$($Binding.group)</groupId><artifactId>$($Binding.modId)</artifactId><version>$($Binding.publicationVersion)</version>$PomDependency</project>"
    $pom|Set-Content -LiteralPath $paths.pom -Encoding UTF8
    $receipt=@{schema=1;sourceCommit=$Binding.head;sourceTree=$Binding.sourceTree;coordinate=$Binding.coordinate;hashes=@{jar=(Get-ReleaseHash $paths.jar);pom=(Get-ReleaseHash $paths.pom);sources=(Get-ReleaseHash $paths.sources)}}
    if($Binding.repository -ne 'BaniraCodex'){
        $common=Binding 'BaniraCodex' $Binding.branch 'placeholder'
        $common.mavenRoot=$Binding.mavenRoot
        $commonPaths=Get-ReleaseArtifactPaths $common
        $receipt.baniraCoordinate=$common.coordinate;$receipt.baniraSha256=Get-ReleaseHash $commonPaths.jar
    }
    $receipt|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $paths.receipt -Encoding UTF8
    $paths
}
function MutateReceipt($Paths,[scriptblock]$Action){$value=Get-Content -LiteralPath $Paths.receipt -Encoding UTF8 -Raw|ConvertFrom-Json;& $Action $value;$value|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $Paths.receipt -Encoding UTF8}
try{
    Check 'control:stable-policy' {Need ($policy.version -eq '2.0.0' -and $policy.baniraVersion -eq '2.0.0') 'Release version drift'}
    $all=@(foreach($repo in $policy.repositories.PSObject.Properties){foreach($branch in $policy.targets.PSObject.Properties){Binding $repo.Name $branch.Name 'all'}})
    Check 'control:52-unique-coordinates' {Need ($all.Count -eq 52 -and @($all|Group-Object coordinate|Where-Object Count -ne 1).Count -eq 0) 'Coordinate collision'}
    Check 'control:13-maven-isolations' {Need (@($all.mavenRoot|Sort-Object -Unique).Count -eq 13) 'Targets share Maven roots'}
    Check 'control:52-isolated-source-paths' {Need (@($all.project|Sort-Object -Unique).Count -eq 52 -and @($all|Where-Object {$_.project -eq $_.repositoryRoot}).Count -eq 0) 'Release builds reuse an active source path'}
    foreach($binding in $all){
        Check "binding:$($binding.repository):$($binding.branch)" {
            Need ($binding.gradleArguments -contains '-Pmod_version=2.0.0' -and $binding.gradleArguments -contains '--max-workers=1' -and !$binding.packageVerified -and !$binding.ideaActivated) 'Unsafe initial binding'
            Need ($binding.gradleArguments -contains "-Dmaven.repo.local=$($binding.mavenRoot)" -and $binding.gradleArguments -contains "-Pbanira.validationOutputRoot=$($binding.outputRoot)") 'Output isolation missing'
            Need ($binding.gradleArguments[0] -eq '--project-dir' -and $binding.gradleArguments[1] -eq $binding.project) 'Source isolation missing'
            if($binding.repository -ne 'BaniraCodex'){Need ($binding.gradleArguments -contains '-Pbanira_version=2.0.0') 'Old common-library version'}
        }
    }
    Check 'reject:relative-output' {Reject {Resolve-ReleaseDirectory 'relative/path'} 'absolute'}
    foreach($name in @('builds','.m2','.idea','.run')){Check "reject:protected-$name" {Reject {Resolve-ReleaseDirectory (Join-Path $root "$name/inside")} 'Protected'}}
    Check 'reject:duplicate-properties' {Reject {Read-ReleaseProperties "mod_version=1`nmod_version=2"} 'Duplicate'}
    Check 'control:properties-with-range' {Need ((Read-ReleaseProperties "# UTF-8`nbanira_version_range=>=2.0.0 <3.0.0").banira_version_range -eq '>=2.0.0 <3.0.0') 'Range parsing failed'}
    Check 'control:literal-maven-group' {Need ((Read-ReleaseGroup @{} "group = 'xin.vanilla.banira'") -eq 'xin.vanilla.banira') 'Literal Maven group not read'}
    Check 'control:property-maven-group' {Need ((Read-ReleaseGroup @{mod_group_id='xin.vanilla.sakura'} 'group = mod_group_id') -eq 'xin.vanilla.sakura') 'Property Maven group not read'}
    Check 'reject:dynamic-maven-group' {Reject {Read-ReleaseGroup @{} 'group = arbitraryFunction()'} 'unique literal'}
    Check 'control:repeated-identical-maven-group' {Need ((Read-ReleaseGroup @{} "group = 'xin.vanilla.aotake'`ngroup = 'xin.vanilla.aotake'") -eq 'xin.vanilla.aotake') 'Repeated identical group rejected'}
    Check 'control:publication-maven-group' {Need ((Read-ReleaseGroup @{} "groupId = 'xin.vanilla.aotake'") -eq 'xin.vanilla.aotake') 'Publication group rejected'}
    Check 'reject:conflicting-maven-group' {Reject {Read-ReleaseGroup @{} "group = 'xin.vanilla.aotake'`ngroup = 'wrong.group'"} 'unique literal'}
    Check 'control:direct-version-template' {Need (Test-ReleaseVersionTemplate '"version":"${mod_version}"' '') 'Direct version template failed'}
    Check 'control:derived-version-template' {Need (Test-ReleaseVersionTemplate '"version":"${version}"' "version = project.mod_version`nexpand 'version': project.version") 'Derived version template failed'}
    Check 'reject:unbound-version-template' {Reject {Test-ReleaseVersionTemplate '"version":"${version}"' "version = '1.0.4'"} 'parameterized'}
    Check 'reject:wrong-source-loader' {$s=Source 'SakuraSignIn_MC' 'fabric/20.1';$s.loader='forge';Reject {New-ReleaseBinding $s $policy $root} 'mismatch'}
    Check 'reject:wrong-minecraft' {$s=Source 'SakuraSignIn_MC' 'fabric/20.1';$s.minecraft='26.2';Reject {New-ReleaseBinding $s $policy $root} 'mismatch'}
    Check 'reject:short-source-commit' {$s=Source 'SakuraSignIn_MC' 'fabric/20.1';$s.head='abcd';Reject {New-ReleaseBinding $s $policy $root} 'full source'}
    Check 'reject:maven-group-traversal' {$s=Source 'SakuraSignIn_MC' 'fabric/20.1';$s.group='..';Reject {New-ReleaseBinding $s $policy $root} 'identity'}
    $verified=[Collections.Generic.List[object]]::new()
    foreach($branch in @('fabric/20.1','forge/20.1','neoforge/21.1')){
        $b=Binding 'BaniraCodex' $branch 'valid';$s=Binding 'SakuraSignIn_MC' $branch 'valid'
        [void](Fixture $b);[void](Fixture $s)
        Check "control:common-package-$branch" {$verified.Add((Test-ReleaseArtifact $b))}
        Check "control:child-package-$branch" {$verified.Add((Test-ReleaseArtifact $s))}
    }
    $b=Binding 'BaniraCodex' 'fabric/20.1' 'missing';$s=Binding 'SakuraSignIn_MC' 'fabric/20.1' 'missing'
    Check 'reject:missing-artifacts' {Reject {Test-ReleaseArtifact $b} 'not ready'}
    Check 'reject:premature-idea-input' {Reject {Write-ReleaseIdeaInput $s @() (Join-Path $root 'not-activated')} 'package missing';Need (!(Test-Path (Join-Path $root 'not-activated'))) 'Partial activation directory'}
    Check 'reject:unresolved-source-selection' {$pending=Binding 'SakuraSignIn_MC' 'fabric/20.1' 'pending';$pending.sourceSelectionReady=$false;Reject {Write-ReleaseIdeaInput $pending @() (Join-Path $root 'pending-idea')} 'Pending source selection';Need (!(Test-Path (Join-Path $root 'pending-idea'))) 'Pending source activated'}
    $valid=$all|Where-Object {$_.repository -eq 'SakuraSignIn_MC' -and $_.branch -eq 'fabric/20.1'}
    $valid.mavenRoot=(Binding 'SakuraSignIn_MC' 'fabric/20.1' 'valid').mavenRoot
    Check 'control:staged-idea-input' {
        $path=Write-ReleaseIdeaInput $valid $verified.ToArray() (Join-Path $root 'idea-staged')
        [xml]$xml=Get-Content -LiteralPath $path -Encoding UTF8 -Raw
        $external=$xml.component.configuration.ExternalSystemSettings
        Need ($external.SelectSingleNode("option[@name='taskNames']/list/option").value -eq ':runClient') 'Wrong Gradle task'
        $options=$external.SelectSingleNode("option[@name='scriptParameters']").value
        Need ($options.Contains('--no-daemon') -and $options.Contains('-Pmod_version=2.0.0') -and $options.Contains('--init-script')) 'Missing Gradle options'
        $init=Get-Content -LiteralPath (Join-Path $root 'idea-staged/verify-input.gradle') -Encoding UTF8 -Raw
        Need ($init.Contains('Release source identity changed') -and $init.Contains('Release package identity changed')) 'Missing launch guard'
        Need ($init.Contains('stream.read(buffer)') -and !$init.Contains('readAllBytes()')) 'Launch hashing requires a newer Java API'
    }
    Check 'control:idea-path-escaping' {
        [void](Write-ReleaseIdeaInput $valid $verified.ToArray() (Join-Path $root "idea's stage"))
        $init=Get-Content -LiteralPath (Join-Path $root "idea's stage/verify-input.gradle") -Encoding UTF8 -Raw
        Need ($init.Contains("idea\'s stage/input.json") -and !$init.Contains("idea\\'s stage")) 'Groovy path quoting changed'
    }
    foreach($case in @('source','hash','coordinate','common-input')){
        $b=Binding 'BaniraCodex' 'fabric/20.1' $case;$s=Binding 'SakuraSignIn_MC' 'fabric/20.1' $case
        [void](Fixture $b);$paths=Fixture $s
        switch($case){
            'source'{MutateReceipt $paths {param($x)$x.sourceCommit='0'*40}}
            'hash'{MutateReceipt $paths {param($x)$x.hashes.jar='0'*64}}
            'coordinate'{MutateReceipt $paths {param($x)$x.coordinate='wrong:coordinate:2.0.0'}}
            'common-input'{MutateReceipt $paths {param($x)$x.baniraSha256='0'*64}}
        }
        Check "reject:$case-drift" {Reject {Test-ReleaseArtifact $s} 'mismatch'}
    }
    foreach($case in @('alpha','range','pom-dependency','dtd')){
        $b=Binding 'BaniraCodex' 'forge/20.1' $case;$s=Binding 'SakuraSignIn_MC' 'forge/20.1' $case
        [void](Fixture $b)
        switch($case){
            'alpha'{[void](Fixture $s -ModVersion '2.0.0-alpha.3')}
            'range'{[void](Fixture $s -DependencyRange '[1.0.4,1.1.0)')}
            'pom-dependency'{[void](Fixture $s -PomDependency '<dependencies><dependency><groupId>xin.vanilla.banira</groupId><artifactId>banira_codex</artifactId><version>forge-1.20.1-1.0.4</version></dependency></dependencies>')}
            'dtd'{[void](Fixture $s -PomPrefix '<!DOCTYPE project [<!ENTITY x SYSTEM "file:///nonexistent">]>')}
        }
        Check "reject:$case" {Reject {Test-ReleaseArtifact $s} 'mismatch|DTD|DTD|DTD'}
    }
    $again=Binding 'SakuraSignIn_MC' 'fabric/20.1' 'valid'
    $paths=Get-ReleaseArtifactPaths $again
    Add-Content -LiteralPath $paths.jar -Value 'changed-after-verification' -Encoding UTF8
    Check 'reject:changed-after-verification' {Reject {Write-ReleaseIdeaInput $again $verified.ToArray() (Join-Path $root 'idea-drift')} 'hash mismatch';Need (!(Test-Path (Join-Path $root 'idea-drift'))) 'Partial output on rejection'}
}finally{
    [void][IO.Directory]::CreateDirectory($EvidenceRoot)
    @{kind='assertions';scope='Release input tooling only; synthetic archives, no Gradle, Java or game';checks=$checks.ToArray()}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $EvidenceRoot 'assertions.json') -Encoding UTF8
}
"PASS $($checks.Count) release-input controls; no build or game launched."
