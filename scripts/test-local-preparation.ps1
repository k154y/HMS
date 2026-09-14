$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$accounts=Get-Content (Join-Path $root '.env.test-accounts.json') -Raw|ConvertFrom-Json
$run=(Get-Content (Join-Path $root 'docs/testing/operational-test-results.json') -Raw|ConvertFrom-Json).run
$base='http://localhost:8081/api/v1'
function Request($method,$path,$token,$body){$p=@{Uri="$base$path";Method=$method;ContentType='application/json';TimeoutSec=30};if($token){$p.Headers=@{Authorization="Bearer $token"}};if($null-ne$body){$p.Body=$body|ConvertTo-Json -Depth 8};Invoke-RestMethod @p}
function Token($role){$a=$accounts.accounts|Where-Object role -eq $role;(Request POST /auth/login $null @{email=$a.email;password=$a.password}).accessToken}
$owner=Token OWNER;$bar=Token BARTENDER;$waiter=Token WAITER
$branch="/hotels/$($accounts.hotelId)/branches/$($accounts.branchId)"
$allOrders=Request GET "$branch/orders" $owner $null
$orders=@($allOrders|Where-Object {$_.customer_name-eq"Workflow $run"-and$_.status-ne'VOIDED'})
if(-not$orders.Count){throw 'Run test-local-operations.ps1 first'}
$checks=@()
foreach($order in $orders){
 $detail=Request GET "$branch/orders/$($order.id)" $owner $null
 if($detail.status-eq'SERVED'){continue}
 foreach($item in $detail.items){
  $queue=Request GET "$branch/orders/queue?destination=BAR" $bar $null
  if(-not($queue|Where-Object {$_.order_id-eq$order.id})){throw 'Order missing from bar queue'}
  if($item.preparation_status-eq'SENT'){Request POST "$branch/orders/$($order.id)/items/$($item.id)/status" $bar @{status='PREPARING'}|Out-Null}
  Request POST "$branch/orders/$($order.id)/items/$($item.id)/status" $bar @{status='READY'}|Out-Null
 }
 if((Request GET "$branch/orders/$($order.id)" $owner $null).status-ne'READY'){throw 'Ready items did not advance order'}
 Request POST "$branch/orders/$($order.id)/status" $waiter @{status='SERVED'}|Out-Null
 $checks+='Bar item preparation and waiter service completed'
}
@{status='PASS';run=$run;checkedAt=(Get-Date).ToUniversalTime().ToString('o');checks=$checks}|ConvertTo-Json -Depth 5|Set-Content (Join-Path $root 'docs/testing/preparation-test-results.json')
$checks

