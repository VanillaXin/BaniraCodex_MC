param(
    [Parameter(Mandatory)][string]$Plan,
    [string]$Repository,
    [string]$Branch,
    [string]$IdeaOutput
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
Import-Module "$PSScriptRoot/ReleaseInputs.psm1" -Force
$input=Get-Content -LiteralPath $Plan -Encoding UTF8 -Raw|ConvertFrom-Json
if($input.schema -ne 1 -or $input.policySha256 -ne (Get-ReleaseHash "$PSScriptRoot/release-policy.json")){throw 'Release policy identity changed'}
if([bool]$Repository -ne [bool]$Branch){throw 'Repository and branch must be selected together'}
if($IdeaOutput -and !$Repository){throw 'Select one project before staging an IDEA input'}
$selected=@($input.bindings|Where-Object {!$Repository -or ($_.repository -eq $Repository -and $_.branch -eq $Branch)})
if(!$selected.Count -or ($Repository -and $selected.Count -ne 1)){throw 'Release input selection is not unique'}
$required=@($selected)
if($IdeaOutput -and $Repository -ne 'BaniraCodex'){
    $common=@($input.bindings|Where-Object {$_.repository -eq 'BaniraCodex' -and $_.branch -eq $Branch})
    if($common.Count -ne 1){throw 'Matching common-library source is absent'}
    $required+=@($common)
}
$verified=@(foreach($binding in $required){
    if(!$binding.sourceSelectionReady){throw 'Pending source selection must be resolved before verification'}
    $head=git.exe -C $binding.repositoryRoot rev-parse "refs/heads/$($binding.branch)"
    if($LASTEXITCODE -ne 0 -or $head -ne $binding.head){throw 'Release maintenance source changed'}
    Test-ReleaseArtifact $binding
})
if($IdeaOutput){Write-ReleaseIdeaInput $selected[0] $verified $IdeaOutput}else{$verified|ConvertTo-Json -Depth 12}
