$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8081/api/v1'
$root = Split-Path $PSScriptRoot
$credentialsPath = Join-Path $root '.env.test-accounts.json'
$bootstrap = @{}
Get-Content (Join-Path $root 'apps/web/.env.admin.local') | ForEach-Object {
 $pair = $_ -split '=',2
 if ($pair.Length -eq 2) { $bootstrap[$pair[0]]=$pair[1] }
}
function Api($method,$path,$token,$body) {
 $params = @{Uri="$base$path";Method=$method;ContentType='application/json';TimeoutSec=45}
 if($token){$params.Headers=@{Authorization="Bearer $token"}}
 if($null -ne $body){$params.Body=ConvertTo-Json $body -Depth 12 -Compress}
 Invoke-RestMethod @params
}
function Login($account){(Api 'POST' '/auth/login' $null @{email=$account.email;password=$account.password}).accessToken}
function Password { [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)) }
$admin=@{role='SUPER_ADMIN';email=$bootstrap.HMS_BOOTSTRAP_ADMIN_EMAIL;password=$bootstrap.HMS_BOOTSTRAP_ADMIN_PASSWORD}
$adminToken=Login $admin
$profile=Api 'GET' '/auth/me' $adminToken $null
if($profile.role -ne 'SUPER_ADMIN'){throw 'Super-admin profile verification failed'}
if(Test-Path $credentialsPath){$saved=Get-Content $credentialsPath -Raw | ConvertFrom-Json -AsHashtable}
else{
 $owner=@{role='OWNER';email='owner@hms-demo.local';password=(Password)}
 $created=Api 'POST' '/platform/hotels' $adminToken @{email=$owner.email;password=$owner.password;fullName='Demo Hotel Owner';preferredLanguage='en';hotel=@{code='HMS-DEMO';legalName='HMS Demo Hotel';displayName='HMS Demo Hotel';currencyCode='RWF';timezone='Africa/Kigali';defaultLanguage='en'}}
 $saved=@{hotelId=$created.hotel.id;branchId=$created.branch.id;accounts=@($admin,$owner)}
 $saved | ConvertTo-Json -Depth 10 | Set-Content $credentialsPath
}
$owner=$saved.accounts | Where-Object role -eq 'OWNER'
$ownerToken=Login $owner
$roles=(Api 'GET' "/hotels/$($saved.hotelId)/roles" $ownerToken $null).content
foreach($role in $roles){
 if($role.code -eq 'OWNER' -or ($saved.accounts | Where-Object role -eq $role.code)){continue}
 $account=@{role=$role.code;email="$($role.code.ToLower())@hms-demo.local";password=(Password)}
 $member=Api 'POST' "/hotels/$($saved.hotelId)/memberships" $ownerToken @{email=$account.email;password=$account.password;fullName="Demo $($role.code)";preferredLanguage='en';allBranches=$true}
 Api 'PUT' "/hotels/$($saved.hotelId)/memberships/$($member.id)/roles/$($role.id)" $ownerToken $null | Out-Null
 $saved.accounts+= $account
 $saved | ConvertTo-Json -Depth 10 | Set-Content $credentialsPath
}
$results=@()
foreach($account in $saved.accounts){
 $token=Login $account
 $me=Api 'GET' '/auth/me' $token $null
 if($me.role -ne $account.role){throw "Incorrect role for $($account.role)"}
 if($account.role -ne 'SUPER_ADMIN'){
  if($me.hotelId -ne $saved.hotelId -or $me.branchId -ne $saved.branchId){throw "Incorrect tenant context for $($account.role)"}
  $denied=$false
  try{Api 'GET' '/platform/hotels' $token $null | Out-Null}catch{if([int]$_.Exception.Response.StatusCode -eq 403){$denied=$true}else{throw}}
  if(!$denied){throw "Platform access unexpectedly allowed for $($account.role)"}
 }
 $results+=[pscustomobject]@{role=$account.role;login='PASS';profile='PASS';platformBoundary='PASS'}
}
$report=Join-Path $root 'docs/testing/account-test-results.json'
$results | ConvertTo-Json | Set-Content $report
$results | Format-Table

