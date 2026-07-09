<#
.SYNOPSIS
    Commit + push + declenche le build TestFlight + surveille le run, en une commande.

.DESCRIPTION
    Raccourci pour generer une nouvelle version iOS apres des modifications.
    - Commit les changements en cours (si il y en a) avec le message fourni.
    - Push sur la branche courante.
    - Declenche le workflow "iOS Release (TestFlight)" via gh.
    - Suit l'avancement du run jusqu'a la fin.

.PARAMETER Message
    Message de commit. Si omis et qu'il y a des changements, un message horodate est utilise.

.PARAMETER NoWatch
    Ne pas surveiller le run apres l'avoir declenche (rend la main tout de suite).

.EXAMPLE
    ./scripts/ship-ios.ps1 "feat: nouvel ecran profil"

.EXAMPLE
    ./scripts/ship-ios.ps1        # commit horodate + push + build + watch
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Message,

    [switch]$NoWatch
)

$ErrorActionPreference = "Stop"
# Nom de fichier du workflow (plus robuste que le libelle avec gh).
$workflow = "ios-release.yml"

# Se placer a la racine du repo (le script vit dans /scripts).
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

# 0. Verifier que gh est dispo et authentifie.
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "gh (GitHub CLI) introuvable. Installe-le : https://cli.github.com/"
    exit 1
}
gh auth status 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error "gh n'est pas authentifie. Lance : gh auth login"
    exit 1
}

# 1. Commit si necessaire.
$status = git status --porcelain
if ($status) {
    if (-not $Message) {
        $Message = "chore: build iOS $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    }
    Write-Host "==> Commit des changements..." -ForegroundColor Cyan
    git add -A
    git commit -m $Message
} else {
    Write-Host "==> Aucun changement a committer." -ForegroundColor DarkGray
}

# 2. Push la branche courante.
$branch = git rev-parse --abbrev-ref HEAD
Write-Host "==> Push de la branche '$branch'..." -ForegroundColor Cyan
git push origin $branch

# 3. Declencher le workflow sur cette branche.
Write-Host "==> Declenchement du workflow '$workflow'..." -ForegroundColor Cyan
gh workflow run $workflow --ref $branch

# 4. Recuperer le run qui vient de demarrer et le surveiller.
if ($NoWatch) {
    Write-Host "==> Workflow declenche. Suivi desactive (-NoWatch)." -ForegroundColor Green
    Write-Host "    Suivre plus tard : gh run watch" -ForegroundColor DarkGray
    exit 0
}

Write-Host "==> Attente du demarrage du run..." -ForegroundColor Cyan
Start-Sleep -Seconds 6
$runId = gh run list --workflow $workflow --limit 1 --json databaseId --jq '.[0].databaseId'

if (-not $runId) {
    Write-Warning "Run non trouve automatiquement. Suis-le avec : gh run watch"
    exit 0
}

Write-Host "==> Suivi du run $runId (Ctrl+C pour arreter le suivi, le build continue cote GitHub)" -ForegroundColor Cyan
gh run watch $runId --exit-status
$watchExit = $LASTEXITCODE

Write-Host ""
if ($watchExit -eq 0) {
    Write-Host "OK - Build uploade vers TestFlight." -ForegroundColor Green
    Write-Host "    Le build apparaitra dans App Store Connect dans ~5-15 min (traitement Apple)." -ForegroundColor DarkGray
} else {
    Write-Host "ECHEC du workflow. Voir les logs :" -ForegroundColor Red
    Write-Host "    gh run view $runId --log-failed" -ForegroundColor DarkGray
}
exit $watchExit
