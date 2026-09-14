$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$saved=Get-Content (Join-Path $root '.env.test-accounts.json') -Raw | ConvertFrom-Json
$base='http://localhost:18081/api/v1'
function Call($method,$path,$token,$body){
 $p=@{Uri="$base$path";Method=$method;ContentType='application/json';TimeoutSec=60}
 if($token){$p.Headers=@{Authorization="Bearer $token"}}
 if($null-ne $body){$p.Body=ConvertTo-Json $body -Depth 15 -Compress}
 Invoke-RestMethod @p
}
function Login($role){$a=$saved.accounts | Where-Object role -eq $role;(Call POST /auth/login $null @{email=$a.email;password=$a.password}).accessToken}
function Equal($actual,$expected,$name){if([decimal]$actual-ne[decimal]$expected){throw "$name expected $expected; got $actual"};$script:checks.Add($name)}
function Reject($method,$path,$token,$body,$status){try{Call $method $path $token $body | Out-Null;throw 'Request unexpectedly succeeded'}catch{if([int]$_.Exception.Response.StatusCode -ne $status){throw}}}
$checks=[System.Collections.Generic.List[string]]::new()
$owner=Login OWNER;$cashier=Login CASHIER;$waiter=Login WAITER
$hotel="/hotels/$($saved.hotelId)";$branch="$hotel/branches/$($saved.branchId)";$run=[guid]::NewGuid().ToString('N').Substring(0,10)
$cashierProfile=Call GET /auth/me $cashier $null
$existingShifts=Call GET "$branch/cashier-shifts" $cashier $null
$openShift=$existingShifts|Where-Object {$_.cashier_user_id-eq$cashierProfile.id-and$_.status-eq'OPEN'}
if(-not$openShift){Call POST "$branch/cashier-shifts" $cashier @{openingFloat=0;notes="Workflow approval checks"}|Out-Null}
$customer=Call POST "$hotel/customers" $owner @{code="TEST-$run";kind='INDIVIDUAL';name="Workflow $run";active=$true}
$vendor=Call POST "$hotel/vendors" $owner @{code="TEST-$run";name="Vendor $run";paymentTermsDays=30;active=$true}
$product=Call POST "$hotel/products" $owner @{sku="TEST-$run";name="Juice $run";category='BEVERAGE';purchaseUnit='CASE';sellingUnit='BOTTLE';stockUnit='BOTTLE';purchaseFactor=6;sellingFactor=1;purchasePrice=600;sellingPrice=200;taxRate=0;stockTracked=$true;sellable=$true;purchasable=$true;active=$true;reorderLevel=2;destination='BAR'}
$purchase=Call POST "$branch/purchase-orders" $owner @{vendorId=$vendor.id;reference="PO-$run";items=@(@{productId=$product.id;quantity=2;unitPrice=600})}
Reject POST "$branch/purchase-orders/$($purchase.id)/receive" $owner $null 409
Call POST "$branch/purchase-orders/$($purchase.id)/approve" $owner $null | Out-Null
Call POST "$branch/purchase-orders/$($purchase.id)/receive" $owner $null | Out-Null
Equal (Call GET "$branch/inventory/$($product.id)" $owner $null).quantity 12 'Purchase conversion to stock units'
Call POST "$branch/purchase-orders/$($purchase.id)/receive" $owner $null | Out-Null
Equal (Call GET "$branch/inventory/$($product.id)" $owner $null).quantity 12 'Repeated receipt does not duplicate stock'
$vp=@{amount=400;method='CASH';requestId=[guid]::NewGuid().ToString()}
$paid=Call POST "$branch/purchase-orders/$($purchase.id)/payments" $owner $vp
Equal $paid.balance 800 'Vendor outstanding balance'
Call POST "$branch/purchase-orders/$($purchase.id)/payments" $owner $vp | Out-Null
Equal (Call GET "$branch/purchase-orders/$($purchase.id)" $owner $null).paid 400 'Vendor payment idempotency'
$order=Call POST "$branch/orders" $waiter @{customerId=$customer.id;destination='BAR';items=@(@{productId=$product.id;quantity=3})}
Equal $order.total 600 'POS total'
Call POST "$branch/orders/$($order.id)/send" $waiter $null | Out-Null
Equal (Call GET "$branch/inventory/$($product.id)" $owner $null).quantity 9 'POS stock deduction'
Equal (Call GET "$branch/folios/$($order.folioId)" $owner $null).balance 600 'POS charge posted to customer folio'
$split=Call POST "$branch/payment-approvals" $waiter @{folioId=$order.folioId;requestId=[guid]::NewGuid().ToString();parts=@(@{method='CASH';amount=200},@{method='MOBILE_MONEY';amount=300})}
Equal (Call GET "$branch/folios/$($order.folioId)" $owner $null).balance 600 'Pending payment does not alter folio'
Call POST "$branch/payment-approvals/$($split.id)/approve" $cashier $null | Out-Null
Equal (Call GET "$branch/folios/$($order.folioId)" $owner $null).balance 100 'Approved split reduces balance'
$today=(Get-Date).ToString('yyyy-MM-dd')
$tx=Call GET "$branch/reports/transactions?from=$today&to=$today&method=CASH" $owner $null
$payment=$tx | Where-Object folio_id -eq $order.folioId
Call POST "$branch/payments/$($payment.id)/refund" $owner $null | Out-Null
Equal (Call GET "$branch/folios/$($order.folioId)" $owner $null).balance 300 'Refund restores outstanding balance'
Reject POST "$branch/payments/$($payment.id)/refund" $owner $null 409
$second=Call POST "$branch/orders" $owner @{customerId=$customer.id;destination='BAR';items=@(@{productId=$product.id;quantity=1})}
if($second.folioId-ne$order.folioId){throw "Customer orders did not reuse the open folio"};$checks.Add("Customer orders reuse the same open folio")
Call POST "$branch/orders/$($second.id)/send" $owner $null | Out-Null
Call POST "$branch/orders/$($second.id)/void" $owner $null | Out-Null
Equal (Call GET "$branch/inventory/$($product.id)" $owner $null).quantity 9 'Order void restores stock'
Equal (Call GET "$branch/folios/$($second.folioId)" $owner $null).balance 300 'Order void reverses only its own folio charge'
Call GET "$branch/reports/sales?from=$today&to=$today" $owner $null | Out-Null
Call GET "$branch/reports/financial?from=$today&to=$today" $owner $null | Out-Null
$checks.Add('Sales and financial reports execute on PostgreSQL')
$typeBody=@{code="TYPE-$run";name="Suite $run";description='Integration test room type';standardOccupancy=2;maxAdults=3;maxChildren=2;defaultRate=50000;bedType='KING';bedDimensions='180 x 200 cm'}
$roomType=Call POST "$branch/room-types" $owner $typeBody
$roomBody=@{roomTypeId=$roomType.id;code="ROOM-$run";floor='2';beds=2}
$room=Call POST "$branch/rooms" $owner $roomBody
Equal $room.nightlyRate 50000 'Room inherits nightly price'
Equal $room.adults 3 'Room inherits adult capacity'
if($room.bedType-ne'KING'-or$room.bedDimensions-ne'180 x 200 cm'){throw 'Room bed defaults were not inherited'}
$checks.Add('Room inherits bed type and dimensions')
$roomBody.nightlyRate=55000;$roomBody.floor='3'
$edited=Call PUT "$branch/rooms/$($room.id)" $owner $roomBody
Equal $edited.nightlyRate 55000 'Room-specific price can be edited'
Reject DELETE "$branch/room-types/$($roomType.id)" $owner $null 409
$reservation=Call POST "$branch/reservations" $owner @{bookingCustomerId=$customer.id;checkIn=(Get-Date).AddDays(20).ToString('yyyy-MM-dd');checkOut=(Get-Date).AddDays(22).ToString('yyyy-MM-dd');adults=2;children=1;rooms=@(@{roomId=$room.id;adults=2;children=1})}
Reject DELETE "$branch/rooms/$($room.id)" $owner $null 409
$checks.Add('Booked room cannot be deleted')
Call POST "$branch/reservations/$($reservation.id)/cancel" $owner $null | Out-Null
$stayBooking=Call POST "$branch/reservations" $owner @{bookingCustomerId=$customer.id;checkIn=$today;checkOut=(Get-Date).AddDays(2).ToString('yyyy-MM-dd');adults=2;children=0;rooms=@(@{roomId=$room.id;adults=2;children=0})}
Call POST "$branch/reservations/$($stayBooking.id)/check-in" $owner $null | Out-Null
Equal (Call GET "$branch/folios/$($stayBooking.folioId)" $owner $null).balance 110000 'Stay uses room-specific nightly price'
$roomOrder=Call POST "$branch/orders" $waiter @{customerId=$customer.id;destination='BAR';items=@(@{productId=$product.id;quantity=1})}
if($roomOrder.folioId-ne$stayBooking.folioId){throw 'Resident order did not use the stay folio'}
Call POST "$branch/orders/$($roomOrder.id)/send" $waiter $null | Out-Null
$foodPay=Call POST "$branch/payment-approvals" $waiter @{folioId=$stayBooking.folioId;requestId=[guid]::NewGuid().ToString();collectionScope='FOOD';parts=@(@{method='CASH';amount=200})}
Call POST "$branch/payment-approvals/$($foodPay.id)/approve" $cashier $null | Out-Null
Equal (Call GET "$branch/folios/$($stayBooking.folioId)" $owner $null).balance 110000 'Waiter can collect only food on a room folio'
Reject POST "$branch/payment-approvals" $waiter @{folioId=$stayBooking.folioId;requestId=[guid]::NewGuid().ToString();collectionScope='ROOM';parts=@(@{method='CASH';amount=110000})} 403
$roomPay=Call POST "$branch/payment-approvals" $owner @{folioId=$stayBooking.folioId;requestId=[guid]::NewGuid().ToString();collectionScope='ROOM';parts=@(@{method='CASH';amount=110000})}
Reject POST "$branch/payment-approvals/$($roomPay.id)/approve" $owner $null 409
Call POST "$branch/payment-approvals/$($roomPay.id)/approve" $cashier $null | Out-Null
Equal (Call GET "$branch/folios/$($stayBooking.folioId)" $owner $null).balance 0 'Reception room payment settles the stay'
Call POST "$branch/reservations/$($stayBooking.id)/check-out" $owner $null | Out-Null
$staff=Call GET "$branch/housekeeping/tasks/staff" $owner $null
$task=Call POST "$branch/housekeeping/tasks" $owner @{roomId=$room.id;taskDate=$today;assignedUserId=$staff[0].id;notes="Workflow $run"}
Call POST "$branch/housekeeping/tasks/$task/IN_PROGRESS" $owner $null | Out-Null
Call POST "$branch/housekeeping/tasks/$task/DONE" $owner $null | Out-Null
if((Call GET "$branch/rooms/$($room.id)" $owner $null).housekeeping-ne'CLEAN'){throw 'Completed housekeeping did not clean room'}
$checks.Add('Assigned housekeeping completes and cleans the checked-out room')
Call DELETE "$branch/rooms/$($room.id)" $owner $null | Out-Null
Call DELETE "$branch/room-types/$($roomType.id)" $owner $null | Out-Null
if((Call GET "$branch/rooms/$($room.id)" $owner $null).active){throw 'Room was not archived'}
$checks.Add('Room deletion archives the record and preserves history')
$accountant=Login ACCOUNTANT
$productBody=@{sku=$product.sku;name="Corrected Juice $run";category='BEVERAGE';purchaseUnit='CASE';sellingUnit='BOTTLE';stockUnit='BOTTLE';purchaseFactor=6;sellingFactor=1;purchasePrice=600;sellingPrice=250;taxRate=0;stockTracked=$true;sellable=$true;purchasable=$true;active=$true;reorderLevel=2;destination='BAR'}
Equal (Call PUT "$hotel/products/$($product.id)" $accountant $productBody).sellingPrice 250 'Accountant can correct product price'
Reject PUT "$hotel/products/$($product.id)" $waiter $productBody 403
$productBody.purchaseFactor=12
Reject PUT "$hotel/products/$($product.id)" $accountant $productBody 409
$checks.Add('Recorded stock conversion cannot be changed retroactively')
Reject POST "$branch/credit/folios/$($order.folioId)/approve" $waiter $null 403
Call POST "$branch/credit/folios/$($order.folioId)/approve" $owner $null | Out-Null
Equal (Call GET "$branch/credit/$($customer.id)" $owner $null).balance 300 'Approved credit appears in the customer ledger'
$settlement=Call POST "$branch/payment-approvals" $owner @{folioId=$order.folioId;requestId=[guid]::NewGuid().ToString();collectionScope='FOOD';parts=@(@{method='BANK_TRANSFER';amount=300})}
Call POST "$branch/payment-approvals/$($settlement.id)/approve" $cashier $null | Out-Null
Equal (Call GET "$branch/credit/$($customer.id)" $owner $null).balance 0 'Approved credit settlement clears the customer ledger'
$roles=(Call GET "$hotel/roles?size=100" $owner $null).content
$passwordBytes=New-Object byte[] 18; $rng=[Security.Cryptography.RandomNumberGenerator]::Create(); try { $rng.GetBytes($passwordBytes) } finally { $rng.Dispose() }
$memberPassword='Test9!'+[Convert]::ToBase64String($passwordBytes)
$member=Call POST "$hotel/memberships" $owner @{fullName="Role workflow $run";email="roles-$run@hms-demo.local";password=$memberPassword;preferredLanguage='en';allBranches=$true}
try {
 foreach($code in @('ACCOUNTANT','AUDITOR')){$role=$roles|Where-Object code -eq $code;Call PUT "$hotel/memberships/$($member.id)/roles/$($role.id)" $owner $null|Out-Null}
 $assigned=Call GET "$hotel/memberships/$($member.id)/roles" $owner $null
 if(@($assigned).Count-ne2){throw 'Multiple roles were not retained'}
 $multiToken=(Call POST '/auth/login' $null @{email="roles-$run@hms-demo.local";password=$memberPassword}).accessToken
 $multiProfile=Call GET '/auth/me' $multiToken $null
 if($multiProfile.roles-notcontains'ACCOUNTANT'-or$multiProfile.roles-notcontains'AUDITOR'){throw 'Login profile did not combine assigned roles'}
 $checks.Add('One account retains accountant and auditor roles together')
} finally {Call PUT "$hotel/memberships/$($member.id)/status" $owner @{status='SUSPENDED'}|Out-Null}
@{run=$run;checkedAt=(Get-Date).ToUniversalTime().ToString('o');checks=$checks;status='PASS'} | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $root 'docs/testing/operational-test-results.json')
$checks








