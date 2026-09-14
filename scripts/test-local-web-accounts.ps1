$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$saved=Get-Content (Join-Path $root '.env.test-accounts.json') -Raw | ConvertFrom-Json
$results=@()
foreach($account in $saved.accounts){
 $credentials=@{email=$account.email;password=$account.password}|ConvertTo-Json
 $tokens=Invoke-RestMethod http://localhost:18081/api/v1/auth/login -Method POST -ContentType application/json -Body $credentials -TimeoutSec 30
 $profile=Invoke-RestMethod http://localhost:18081/api/v1/auth/me -Headers @{Authorization="Bearer $($tokens.accessToken)"} -TimeoutSec 30
 $web=New-Object Microsoft.PowerShell.Commands.WebRequestSession
 $csrf=Invoke-RestMethod http://localhost:3001/api/auth/csrf -WebSession $web -TimeoutSec 40
 $response=Invoke-WebRequest http://localhost:3001/api/auth/callback/credentials -Method POST -WebSession $web -ContentType 'application/x-www-form-urlencoded' -Headers @{'X-Auth-Return-Redirect'='1'} -Body @{csrfToken=$csrf.csrfToken;email=$account.email;password=$account.password;callbackUrl='http://localhost:3001/dashboard'} -TimeoutSec 45
 $session=Invoke-RestMethod http://localhost:3001/api/auth/session -WebSession $web -TimeoutSec 30
 if(-not $profile.role){if($session.accessToken){throw "Inactive account received an authenticated web session"};$results+=[pscustomobject]@{role=$account.role;webLogin="INACTIVE_ACCESS_REJECTED";session="PASS";pageHttpStatus=$null;platformBoundary="NO_SESSION"};continue}
 if($session.user.role -ne $profile.role){throw "Web login failed for $($account.role)"}
 $platform=Invoke-WebRequest http://localhost:3001/api/platform/hotels -WebSession $web -SkipHttpErrorCheck -TimeoutSec 30
 $expected=if($account.role -eq 'SUPER_ADMIN'){200}else{403}
 if($platform.StatusCode -ne $expected){throw "Incorrect platform API access for $($account.role)"}
 $page=if($account.role -eq 'SUPER_ADMIN'){'platform'}else{'dashboard'}
 $render=Invoke-WebRequest "http://localhost:3001/$page" -WebSession $web -TimeoutSec 40
 if($render.StatusCode -ne 200){throw "Page rendering failed for $($account.role)"}
 $results+=[pscustomobject]@{role=$account.role;webLogin='PASS';session='PASS';pageHttpStatus=$render.StatusCode;platformBoundary='PASS'}
}
$results | ConvertTo-Json | Set-Content (Join-Path $root 'docs/testing/web-account-test-results.json')
$results | Format-Table


