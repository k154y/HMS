"use client";
import {useEffect,useState,FormEvent} from "react";
import CommerceInvoice from "./CommerceInvoice";
import CommerceReceivingForm from "./CommerceReceivingForm";
import {useSession} from "next-auth/react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";

type Row=Record<string,unknown>;
type Product={id:string;name:string;sku:string;barcode?:string;base_unit?:string;active?:boolean;retail_price?:number|string;wholesale_price?:number|string;purchase_cost?:number|string};
type ProductUnit={unit_name:string;units_per_package:string;purchase_package_price?:string|null;retail_package_price?:string|null;wholesale_package_price?:string|null};
type Item={productId:string;unit:string;quantity:string;unitPrice:string;discount:string;discountMode:"AMOUNT"|"PERCENT";reason:string};
type Pending={path:string;body:Record<string,unknown>};
type Props={businessId:string;branchId:string;locationId:string;role:string;roles?:string[];baseCurrency:string;products:Product[]};
const field="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm";
const button="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";
const secondary="rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:opacity-50";
const str=(row:Row,key:string)=>String(row[key]??"");
const rows=(value:unknown)=>Array.isArray(value)?value as Row[]:[];
export default function CommerceTrading({businessId,branchId,locationId,role,roles=[],baseCurrency,products}:Props){
 const {t}=useLocale();const {data:session}=useSession();const granted=new Set([role,...roles]);const financial=["OWNER","MANAGER","ACCOUNTANT"].some(r=>granted.has(r)),manager=["OWNER","MANAGER"].some(r=>granted.has(r)),cashier=granted.has("CASHIER"),storekeeper=granted.has("STOREKEEPER")&&!financial&&!cashier;
 const [tab,setTab]=useState(["OWNER","MANAGER","CASHIER"].some(r=>granted.has(r))?"Point of sale":storekeeper?"Purchase history / receiving":"Sales history"),[state,setState]=useState<Row|null>(null),[parties,setParties]=useState<Row[]>([]),[accounts,setAccounts]=useState<Row[]>([]),[currencies,setCurrencies]=useState<Row[]>([]),[documents,setDocuments]=useState<Row[]>([]),[shifts,setShifts]=useState<Row[]>([]);
 const [entitlements,setEntitlements]=useState<Row|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState(""),[pending,setPending]=useState<Pending|null>(null),[detail,setDetail]=useState<Row|null>(null),[report,setReport]=useState<Row|null>(null);
 const [expandedLine,setExpandedLine]=useState<number|null>(null),[productUnits,setProductUnits]=useState<Record<string,ProductUnit[]>>({}),[stockBalances,setStockBalances]=useState<Row[]>([]),[items,setItems]=useState<Item[]>([]),[kind,setKind]=useState("SALE"),[currency,setCurrency]=useState(baseCurrency),[party,setParty]=useState(""),[reference,setReference]=useState(""),[due,setDue]=useState(""),[supplyDate,setSupplyDate]=useState(new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,10)),[wholesale,setWholesale]=useState(false),[search,setSearch]=useState(""),[preview,setPreview]=useState<Row|null>(null);
 const [paymentMethod,setPaymentMethod]=useState("CASH"),[openingAccountName,setOpeningAccountName]=useState("Main cash register"),[openingAccountCurrency,setOpeningAccountCurrency]=useState(baseCurrency),
 [tenders,setTenders]=useState<{accountId:string;amount:string;shiftId:string;reference:string}[]>([]),[paper,setPaper]=useState("80"),[from,setFrom]=useState(new Date().toISOString().slice(0,10)),[to,setTo]=useState(new Date().toISOString().slice(0,10));
 const [createForm,setCreateForm]=useState<"party"|"account"|null>(null);
 useEffect(()=>setCreateForm(null),[tab,businessId]);
 const storage=`commerce-pending-${session?.user?.id??"unknown"}-${businessId}`;
 async function api(path:string,body?:unknown):Promise<Row|Row[]>{
  const response=await fetch(`/api/commerce/businesses/${businessId}/${path}`,{method:body===undefined?"GET":"POST",headers:body===undefined?{}:{"Content-Type":"application/json"},body:body===undefined?undefined:JSON.stringify(body),cache:"no-store"});
  if(response.status===401)window.location.replace(loginDestination("/commerce",true));
  if(!response.ok){const failure=await response.json().catch(()=>({}));const e=new Error(String(failure.message??(response.status===403||response.status===404?"Access denied: check assigned roles, branch and modules.":response.status===409?"Transaction conflict; check accounting setup, stock or payment state.":"Unable to complete the transaction."))) as Error&{status:number;code?:string};e.status=response.status;e.code=failure.code;throw e;}
  return response.json();
 }
 useEffect(()=>{
  let active=true;
  Promise.all(products.map(async product=>{
   const res=await fetch(`/api/commerce/businesses/${businessId}/products/${product.id}/units`,{cache:"no-store"});
   if(!res.ok)return [product.id,[{unit_name:product.base_unit??"unit",units_per_package:"1"}]] as const;
   return [product.id,await res.json() as ProductUnit[]] as const;
  })).then(pairs=>{if(active)setProductUnits(Object.fromEntries(pairs))}).catch(()=>{});
  return()=>{active=false};
 },[businessId,products]);
 async function load(){
  const modules=await api("operations/status") as Row;setEntitlements(modules);
  if(modules.ACCOUNTING!==true||modules.RETAIL!==true||modules.INVENTORY!==true){setState(null);return;}
  const s=await api("accounting");setState(s as Row);
  const [p,c]=await Promise.all([api("parties"),api("currencies")]);setParties(rows(p));setCurrencies(rows(c));
  if(!storekeeper)setAccounts(rows(await api("payment-accounts")));
  if(branchId){setDocuments(rows(await api(`branches/${branchId}/documents`)));if(!storekeeper)setShifts(rows(await api(`branches/${branchId}/shifts`)));}
  if(locationId)setStockBalances(rows(await api(`locations/${locationId}/stock`)));
 }
 useEffect(()=>{setDetail(null);setItems([]);setPreview(null);setTenders([]);setError("");setDocuments([]);setShifts([]);let active=true;
  try{const saved=sessionStorage.getItem(storage);setPending(saved?JSON.parse(saved):null);}catch{setPending(null);}
  load().catch(e=>{if(active)setError(e.message)});return()=>{active=false};
 },[businessId,branchId,locationId,storage]);
 useEffect(()=>{setPreview(null)},[items,currency,party,kind,wholesale,locationId]);
 async function execute(p:Pending){
  setBusy(true);setError("");try{
   const data=await api(p.path,p.body) as Row;if(p.path==="parties"||p.path==="payment-accounts")setCreateForm(null);sessionStorage.removeItem(storage);setPending(null);await load();
   if(p.path==="documents"){setItems([]);setTenders([]);setPreview(null);setDetail(await api(`documents/${data.id}`) as Row);setSearch("");setTab(p.body.kind==="PURCHASE"?"Purchase history / receiving":"Sales history");}
   else if(detail?.id)setDetail(await api(`documents/${detail.id}`) as Row); if(p.path.endsWith("/print"))window.print();
  }catch(e){setError(e instanceof Error?e.message:"Unable to complete the transaction. Check the entered amounts and required fields.");
   if([400,403,404,422].includes((e as Error&{status?:number}).status??0)||((e as Error&{status?:number}).status===409&&(e as Error&{code?:string}).code!=="FINANCIAL_REQUEST_CONFLICT")){sessionStorage.removeItem(storage);setPending(null);}
  }finally{setBusy(false);}
 }
 async function post(path:string,body:Record<string,unknown>){if(pending||busy)return;const p={path,body:{...body,requestId:crypto.randomUUID()}};sessionStorage.setItem(storage,JSON.stringify(p));setPending(p);await execute(p);}
 useEffect(()=>{if(detail&&(tab==="Sales history"||tab==="Purchase history / receiving")){
  const node=document.getElementById("commerce-document-details");
  node?.scrollIntoView({behavior:"auto",block:"center"});
 }},[detail?.id,tab]);
 const myOpenShift=shifts.find(s=>!s.closed_at&&String(s.cashier_id)===session?.user?.id);
 const cashAccounts=accounts.filter(a=>a.method==="CASH"&&a.active!==false);
 const suitableAccount=accounts.find(a=>a.method===paymentMethod&&a.active!==false&&a.currency===currency);
 const needsShift=kind==="SALE"&&paymentMethod==="CASH";
 const autoTender=()=>{
  if(kind!=="SALE"||paymentMethod==="CREDIT"||!preview)return [];
  const account=paymentMethod==="CASH"?cashAccounts.find(a=>a.id===myOpenShift?.payment_account_id&&a.currency===currency):suitableAccount;
  if(!account)return [];
  return [{accountId:String(account.id),amount:String(preview.total),shiftId:paymentMethod==="CASH"?String(myOpenShift?.id??""):null,reference:null}];
 };
 const draft=(forPosting=false)=>({kind,locationId,partyId:party||null,currency,reference:reference||null,dueDate:due||null,supplyDate:kind==="PURCHASE"?supplyDate:null,wholesale,
  lines:items.map(i=>({productId:i.productId,unit:i.unit||null,quantity:i.quantity,unitPrice:i.unitPrice||null,
   discount:i.discountMode==="AMOUNT"&&i.discount!==""?i.discount:null,
   discountPercent:i.discountMode==="PERCENT"&&i.discount!==""?i.discount:null,reason:i.reason||null})),
  payments:tenders.length?tenders.map(p=>({...p,shiftId:p.shiftId||null})):forPosting?autoTender():[]});
 const unitsFor=(id:string)=>productUnits[id]??[{unit_name:products.find(p=>p.id===id)?.base_unit??"piece",units_per_package:"1"}];
 function defaultUnit(product:Product){const units=unitsFor(product.id),prefer=kind==="PURCHASE"?
   units.find(u=>u.purchase_package_price!=null)||units.find(u=>Number(u.units_per_package)>1):wholesale?
   units.find(u=>u.wholesale_package_price!=null):undefined;return prefer?.unit_name??product.base_unit??"piece";}
 const addProduct=(product:Product)=>setItems(old=>[...old,{productId:product.id,unit:defaultUnit(product),quantity:"1",unitPrice:"",discount:"",discountMode:"AMOUNT",reason:""}]);
 const formatPrice=(p:Product,unit:ProductUnit)=>{
  const amount=kind==="PURCHASE"?unit.purchase_package_price??(Number(p.purchase_cost??0)*Number(unit.units_per_package)).toFixed(4):
  wholesale?unit.wholesale_package_price??(Number(p.wholesale_price??p.retail_price??0)*Number(unit.units_per_package)).toFixed(4):
  unit.retail_package_price??(Number(p.retail_price??0)*Number(unit.units_per_package)).toFixed(4);
  return String(amount);
 };
 async function calculate(){setBusy(true);setError("");try{setPreview(await api("documents/preview",draft()) as Row)}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 const updateItem=(index:number,key:keyof Item,value:string)=>setItems(s=>s.map((x,i)=>i===index?{...x,[key]:value}:x));
 async function complete(){
  if(kind==="SALE"&&paymentMethod==="CASH"&&!myOpenShift){setError("Open a cashier shift before accepting cash. Go to Cashier shifts.");return;}
  if(kind==="SALE"&&paymentMethod!=="CREDIT"&&!tenders.length&&!autoTender().length){setError("Create or select an active payment account for this currency before checkout.");return;}
  if(kind==="SALE"&&paymentMethod==="CREDIT"&&!party){setError("Credit sales require an approved customer. Select a customer before checkout.");return;}
  await post("documents",draft(true));
 }
 const formData=(e:FormEvent<HTMLFormElement>)=>{e.preventDefault();return new FormData(e.currentTarget)};
 const value=(data:FormData,key:string)=>String(data.get(key)??"");
 const label=(name:string,node:React.ReactNode)=><label className="grid gap-1 text-sm">{t(name)}{node}</label>;
 function options(data:Row[],key="name"){return <><option value="">{t("Select")}</option>{data.map(x=><option key={str(x,"id")} value={str(x,"id")}>{str(x,key)} {str(x,"currency")}</option>)}</>}
 const tabs=[...(manager||cashier?["Point of sale"]:[]),...(financial?["New purchase"]:[]),"Sales history",...(financial||granted.has("STOREKEEPER")?["Purchase history / receiving"]:[]),...(financial?["Customers and suppliers"]:[]),...(role==="OWNER"?["Payment accounts"]:[]),...(!storekeeper?["Cashier shifts"]:[]),...(financial?["Financial reports","Operating expenses"]:[])];
 const disabled=busy||!!pending||!session?.user?.id;
 const reviewIssue=!branchId?"Select a branch in the workspace selectors above before reviewing an invoice."
  :!locationId?"Select a stock location above. Purchases need a receiving location; sales need a selling location."
  :!items.length?"Add at least one registered product from the catalog."
  :items.some(x=>!Number.isFinite(Number(x.quantity))||Number(x.quantity)<=0)?"Enter a positive quantity for every product."
  :pending?"A previous transaction is waiting for confirmation. Retry or resolve it first."
  :!session?.user?.id?"Your session is not ready. Refresh or sign in again."
  :busy?"Wait until the current request finishes.":"";
 return <div className="space-y-5">
  {error&&<div role="alert" className="rounded-lg bg-amber-50 p-4 text-amber-900">{t(error)}</div>}
  {entitlements&&(!entitlements.RETAIL||!entitlements.ACCOUNTING||!entitlements.INVENTORY)&&<section className="rounded-xl border border-amber-200 bg-amber-50 p-5"><h2 className="font-semibold">Commerce setup required</h2><p className="my-2 text-sm">Purchasing and selling need Inventory, Retail and Accounting modules. These are core Commerce operations.</p>{role==="OWNER"?<button className={button} disabled={busy} onClick={async()=>{setBusy(true);setError("");try{await api("operations/enable",{});await load()}catch(e){setError((e as Error).message)}finally{setBusy(false)}}}>Enable purchasing and sales</button>:<p className="text-sm">Ask the business owner to enable Commerce operations.</p>}</section>}
  {pending&&<div className="rounded-lg border border-amber-300 p-4"><p>{t("A transaction is awaiting confirmation. Retry the same request to avoid duplicate posting.")}</p><button className={button} disabled={busy} onClick={()=>execute(pending)}>{t("Retry transaction")}</button></div>}
  {state&&!state.active&&<section className="rounded-xl border bg-white p-5"><h2 className="font-semibold">{t("Opening inventory approval")}</h2><p className="my-3">{t("Review current quantities and purchase costs before approving opening inventory valuation. The base currency is locked after approval.")}</p><p className="mb-3">{t("Inventory valuation")}: {str(state,"openingValuation")} {baseCurrency}</p>{role==="OWNER"&&<button disabled={disabled} className={button} onClick={()=>post("accounting/activate",{})}>{t("Approve opening inventory valuation")}</button>}</section>}
  <nav className="flex flex-wrap gap-2">{tabs.map(name=><button key={name} disabled={busy} className={tab===name?button:secondary} onClick={()=>{setTab(name);setDetail(null);setSearch("");if(name==="Point of sale"||name==="New purchase"){setKind(name==="New purchase"?"PURCHASE":"SALE");setItems([]);setParty("");setTenders([]);setPreview(null);setPaymentMethod("CASH")}}}>{t(name)}</button>)}</nav>
  {entitlements?.ACCOUNTING===true&&entitlements?.RETAIL===true&&(tab==="Point of sale"||tab==="New purchase")&&<section className="space-y-4 rounded-xl border bg-white p-5">
   {!locationId&&<div role="status" className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900"><strong>Before continuing:</strong> choose the branch and the stock location in the selectors at the top of this page. You can then review and post your {kind==="PURCHASE"?"purchase invoice":"sale"}.</div>}
   <div className="flex flex-wrap items-center gap-2 rounded-lg bg-slate-50 p-3 text-sm"><span className="rounded-full bg-emerald-700 px-2.5 py-1 font-semibold text-white">1</span><span className={!preview?"font-semibold":""}>Choose products and quantities</span><span className="text-slate-400">→</span><span className="rounded-full border border-slate-300 px-2.5 py-1">2</span><span className={preview?"font-semibold text-emerald-800":""}>Review total and payment</span><span className="text-slate-400">→</span><span className="rounded-full border border-slate-300 px-2.5 py-1">3</span><span>{kind==="PURCHASE"?"Post purchase invoice":"Complete sale"}</span><span className="text-slate-400">→</span><span className="rounded-full border border-slate-300 px-2.5 py-1">4</span><span>{kind==="PURCHASE"?"Check and receive goods":"Print receipt"}</span></div>
   <div className="grid gap-3 md:grid-cols-3">{label("Currency",<select className={field} value={currency} disabled={disabled} onChange={e=>setCurrency(e.target.value)}>{currencies.filter(x=>x.active!==false).map(x=><option key={str(x,"currency")}>{str(x,"currency")}</option>)}</select>)}
    {label(kind==="SALE"?"Customer":"Supplier",<select className={field} value={party} disabled={disabled} onChange={e=>setParty(e.target.value)}>{options(parties.filter(x=>x.active!==false&&x.kind===(kind==="SALE"?"CUSTOMER":"SUPPLIER")))}</select>)}
    {label("Reference",<input className={field} maxLength={200} placeholder={kind==="PURCHASE"?"Supplier invoice reference":"Sale reference (optional)"} value={reference} disabled={disabled} onChange={e=>setReference(e.target.value)}/>)}
    {kind==="PURCHASE"&&label("Purchase / supply date",<input type="date" className={field} value={supplyDate} disabled={disabled} onChange={e=>setSupplyDate(e.target.value)}/>)}
    {label("Payment due date (optional)",<input type="date" className={field} value={due} disabled={disabled} onChange={e=>setDue(e.target.value)}/>)}
    {kind==="SALE"&&label("Wholesale price",<input type="checkbox" checked={wholesale} disabled={disabled} onChange={e=>setWholesale(e.target.checked)}/>)}
   </div>
   <div className="grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(310px,360px)]">
    <div className="min-w-0 space-y-3">
     <div className="space-y-2 rounded-lg border border-slate-200 bg-slate-50 p-3">
      <h3 className="font-semibold">{kind==="PURCHASE"?"Products to purchase":"Products in this sale"} <span className="ml-2 text-sm font-normal text-slate-500">({items.length} lines)</span></h3>
      {label("Search catalog by name, SKU or scan barcode",<input className={field} value={search} disabled={disabled} placeholder="Find and add a registered product…" onChange={e=>setSearch(e.target.value)} onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();const exact=products.find(p=>p.active!==false&&(p.barcode===search||p.sku===search));if(exact){addProduct(exact);setSearch("")}}}}/>)}
      <select className={field} value="" disabled={disabled} aria-label={t("Select registered product")} onChange={e=>{const p=products.find(x=>x.id===e.target.value);if(p){addProduct(p);setSearch("")}}}><option value="">+ {t("Add product from catalog")}</option>{products.filter(p=>p.active!==false&&`${p.name} ${p.sku} ${p.barcode??""}`.toLowerCase().includes(search.toLowerCase())).map(p=><option key={p.id} value={p.id}>{p.name} · {p.sku}</option>)}</select>
      {!products.some(p=>p.active!==false)&&<p className="text-sm text-amber-800">No active products. Create a product in Products first.</p>}
     </div>
     <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
      <div className="max-h-[440px] overflow-auto">
       <table className="min-w-[620px] w-full text-left text-sm">
        <thead className="sticky top-0 z-10 bg-slate-100"><tr><th className="p-3">Product / selling unit</th><th className="p-3">Qty</th><th className="p-3">Catalog price ({baseCurrency})</th><th className="p-3 text-right">Actions</th></tr></thead>
        <tbody>
         {items.map((item,index)=>{const p=products.find(x=>x.id===item.productId),u=unitsFor(item.productId).find(x=>x.unit_name===item.unit),available=stockBalances.find(x=>String(x.product_id)===item.productId);
          return <tr key={`${item.productId}-${index}`} className="border-t align-top">
           <td className="p-3"><strong>{p?.name??"Product"}</strong><p className="text-xs text-slate-500">{p?.sku??""}</p><p className="text-xs text-slate-500">{item.unit}{kind==="SALE"?` · Available: ${String(available?.quantity??available?.on_hand??"0")} ${p?.base_unit??"units"}`:""}</p></td>
           <td className="p-3"><input type="number" min="0.000001" step="0.000001" aria-label={`Quantity for ${p?.name??"product"}`} className="w-24 rounded border border-slate-300 px-2 py-1" value={item.quantity} disabled={disabled} onChange={e=>updateItem(index,"quantity",e.target.value)}/></td>
           <td className="p-3 whitespace-nowrap">{p&&u?`${formatPrice(p,u)} ${baseCurrency}`:"Loading…"}</td>
           <td className="p-3"><div className="flex justify-end gap-2"><button type="button" disabled={disabled} className={secondary} aria-label={`Edit ${p?.name??"item"}`} onClick={()=>setExpandedLine(expandedLine===index?null:index)}>{expandedLine===index?"Close":"Edit"}</button><button type="button" className={secondary} disabled={disabled} aria-label={`Remove ${p?.name??"item"}`} onClick={()=>{setItems(old=>old.filter((_,i)=>i!==index));setExpandedLine(null)}}>×</button></div></td>
          </tr>})}
         {!items.length&&<tr><td colSpan={4} className="p-8 text-center text-slate-500">Add a product above to begin.</td></tr>}
        </tbody>
       </table>
      </div>
     </div>
     {expandedLine!==null&&items[expandedLine]&&(()=>{const item=items[expandedLine],index=expandedLine,product=products.find(p=>p.id===item.productId);
      return <div className="space-y-3 rounded-lg border border-emerald-200 bg-emerald-50 p-4"><div className="flex justify-between"><h3 className="font-semibold">Edit {product?.name??"item"}</h3><button className={secondary} type="button" onClick={()=>setExpandedLine(null)}>Close</button></div>
       <div className="grid gap-3 sm:grid-cols-2">
        {label(kind==="PURCHASE"?"Purchase unit":"Selling unit",<select className={field} value={item.unit} disabled={disabled} onChange={e=>updateItem(index,"unit",e.target.value)}>{unitsFor(item.productId).map(u=><option key={u.unit_name} value={u.unit_name}>{u.unit_name} ({u.units_per_package} {product?.base_unit??"units"})</option>)}</select>)}
        {label("Quantity",<input className={field} type="number" min="0.000001" step="0.000001" value={item.quantity} disabled={disabled} onChange={e=>updateItem(index,"quantity",e.target.value)}/>)}
        {(manager||kind==="PURCHASE")&&label(kind==="PURCHASE"?"Actual supplier price override (optional)":"Authorized price override (optional)",<input className={field} type="number" min="0" step="any" placeholder="Use registered price" value={item.unitPrice} disabled={disabled} onChange={e=>updateItem(index,"unitPrice",e.target.value)}/>)}
        {(manager||cashier||kind==="PURCHASE")&&<>
         {label("Discount type",<select className={field} disabled={disabled} value={item.discountMode} onChange={e=>setItems(old=>old.map((x,i)=>i===index?{...x,discountMode:e.target.value as Item["discountMode"],discount:""}:x))}><option value="AMOUNT">Fixed amount ({currency})</option><option value="PERCENT">Percentage (%)</option></select>)}
         {label("Discount (optional)",<input className={field} type="number" min="0" max={item.discountMode==="PERCENT"?100:undefined} step="any" placeholder="0" value={item.discount} disabled={disabled} onChange={e=>updateItem(index,"discount",e.target.value)}/>)}
         {label(kind==="PURCHASE"?"Supplier note (optional)":"Discount / override reason",<input className={field} maxLength={500} value={item.reason} disabled={disabled} onChange={e=>updateItem(index,"reason",e.target.value)}/>)}
        </>}
       </div></div>})()}
     <p className="text-xs text-slate-500">Only one item expands at a time. The product list scrolls independently, including purchases with 20 or more lines.</p>
    </div>
    <aside className="min-w-0 space-y-3 xl:sticky xl:top-4">
     <div className="space-y-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="font-semibold">{kind==="PURCHASE"?"Purchase summary":"Sale summary"}</h3>
      <div className="flex justify-between text-sm"><span>Product lines</span><strong>{items.length}</strong></div>
      <div className="flex justify-between text-sm"><span>Quantity total (mixed units)</span><span>By item</span></div>
      <p className="text-xs text-slate-500">Final discounts, taxes and exchange rates are calculated by the backend when you review.</p>
      {preview&&<div className="rounded-lg bg-slate-50 p-3"><p className="text-xs text-slate-600">Reviewed total</p><p className="text-xl font-bold">{str(preview,"total")} {currency}</p></div>}
      <button type="button" className={`${button} w-full`} disabled={Boolean(reviewIssue)} title={reviewIssue||undefined} onClick={calculate}>{busy?"Calculating…":kind==="PURCHASE"?"Review purchase invoice":"Review sale and total"}</button>
      {reviewIssue&&<p role="status" className="text-sm text-amber-800">{reviewIssue}</p>}
     </div>
   {preview&&<div className="space-y-3 rounded-lg border border-emerald-200 p-4"><table className="w-full text-sm"><thead><tr>{["Product","Quantity","Unit price","Discount","Tax","Total"].map(x=><th className="p-2 text-left" key={x}>{t(x)}</th>)}</tr></thead><tbody>{rows(preview.lines).map((line,i)=><tr key={i}>{["name","quantity","unitPrice","discount","tax","total"].map(key=><td className="p-2" key={key}>{str(line,key)}</td>)}</tr>)}</tbody></table><p className="font-bold">{t("Total")}: {str(preview,"total")} {currency}</p>
    {kind==="SALE"&&<div className="space-y-3 rounded-lg border p-3"><h3 className="font-semibold">Payment and checkout</h3>
     <div className="grid gap-3 md:grid-cols-2">{label("Payment method",<select className={field} value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)} disabled={disabled}><option value="CASH">Cash</option><option value="MOBILE_MONEY">Mobile Money</option><option value="BANK">Bank transfer</option><option value="CARD">Card</option><option value="CREDIT">Approved customer credit</option></select>)}
      <div className="text-sm"><p className="font-medium">Amount to collect</p><p className="text-lg font-bold">{str(preview,"total")} {currency}</p><p className="text-xs text-slate-600">Automatically selected from reviewed cart; taxes and discounts included.</p></div></div>
     {needsShift&&!myOpenShift&&<p className="rounded bg-amber-50 p-3 text-sm text-amber-900">You have no open cashier shift. Go to <button type="button" className="font-bold underline" onClick={()=>setTab("Cashier shifts")}>Cashier shifts</button> to open one before accepting cash.</p>}
     {paymentMethod!=="CREDIT"&&!autoTender().length&&!tenders.length&&<p className="rounded bg-amber-50 p-3 text-sm">No active matching {paymentMethod.toLowerCase().replaceAll("_"," ")} account for {currency}. The owner can create one in Payment accounts.</p>}
     {paymentMethod==="CREDIT"&&<p className="text-sm">The customer must have an approved credit limit. A credit sale leaves an amount receivable.</p>}
    </div>}
    {kind==="PURCHASE"&&<p className="text-sm">Invoice cost and supplier discount are calculated above. Receive goods separately after posting. Payment can be recorded later or added below.</p>}
    <details className="rounded-lg border p-3"><summary className="cursor-pointer text-sm font-medium">Split payments / advanced payment entry</summary>
     {tenders.map((pay,i)=><div className="mt-2 grid gap-2 md:grid-cols-4" key={i}>{label("Payment account",<select className={field} value={pay.accountId} disabled={disabled} onChange={e=>setTenders(old=>old.map((x,j)=>j===i?{...x,accountId:e.target.value}:x))}>{options(accounts)}</select>)}{label("Amount",<input className={field} type="number" min="0" step="any" value={pay.amount} disabled={disabled} onChange={e=>setTenders(old=>old.map((x,j)=>j===i?{...x,amount:e.target.value}:x))}/>)}{label("Cashier shift",<select className={field} value={pay.shiftId} disabled={disabled} onChange={e=>setTenders(old=>old.map((x,j)=>j===i?{...x,shiftId:e.target.value}:x))}>{options(shifts.filter(x=>!x.closed_at&&x.cashier_id===session?.user?.id),"account_name")}</select>)}<button type="button" className={secondary} disabled={disabled} onClick={()=>setTenders(old=>old.filter((_,j)=>i!==j))}>Remove</button></div>)}
     <button type="button" className={`${secondary} mt-2`} disabled={disabled||tenders.length>=10} onClick={()=>setTenders(old=>[...old,{accountId:"",amount:"",shiftId:"",reference:""}])}>Add payment</button></details>
    <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4"><p className="mb-2 text-sm text-slate-700">{kind==="PURCHASE"?"Posting records the supplier invoice. Goods are added to inventory only after you confirm receipt.":"Completing the sale records the transaction and stock deduction once. You can print the receipt after it is saved."}</p><button type="button" className={button} disabled={disabled||!state?.active||!locationId||(kind==="SALE"&&paymentMethod==="CASH"&&!myOpenShift)} onClick={complete}>{kind==="PURCHASE"?"Confirm and post purchase invoice":"Complete sale · open receipt"}</button></div>
   </div>}
    </aside>
   </div>
  </section>}
  {tab==="Customers and suppliers"&&financial&&<div className="flex items-center justify-between"><h2 className="text-lg font-semibold">Customers and suppliers</h2><button className={button} disabled={disabled} onClick={()=>setCreateForm(createForm==="party"?null:"party")}>{createForm==="party"?"Cancel":"Create customer / supplier"}</button></div>}
  {tab==="Customers and suppliers"&&financial&&createForm==="party"&&<form className="grid gap-3 rounded-xl border bg-white p-5 md:grid-cols-2" onSubmit={e=>{const d=formData(e);post("parties",{kind:value(d,"kind"),name:value(d,"name"),contact:value(d,"contact"),creditLimit:value(d,"creditLimit")})}}><fieldset disabled={disabled} className="contents">{label("Type",<select name="kind" className={field}><option value="CUSTOMER">{t("Customer")}</option><option value="SUPPLIER">{t("Supplier")}</option></select>)}{label("Name",<input required maxLength={200} name="name" className={field}/>)}{label("Contact",<input maxLength={300} name="contact" className={field}/>)}{label("Credit limit in base currency",<input required defaultValue="0" min="0" step="any" type="number" name="creditLimit" className={field}/>)}<button className={button}>{t("Create")}</button></fieldset></form>}
  {tab==="Customers and suppliers"&&financial&&<section className="space-y-3 rounded-xl border bg-white p-5"><h3 className="font-semibold">{t("Customers and suppliers")}</h3>{parties.map(p=><details key={str(p,"id")} className="rounded border p-3"><summary className="cursor-pointer">{str(p,"name")} <span className="text-sm text-slate-500">{str(p,"kind")} · {str(p,"contact")} · Edit</span></summary><form className="mt-3 grid gap-3 md:grid-cols-2" onSubmit={e=>{const d=formData(e);post(`parties/${p.id}/update`,{name:value(d,"name"),contact:value(d,"contact"),creditLimit:value(d,"limit"),active:d.has("active")})}}><fieldset className="contents" disabled={disabled}>{label("Name",<input required name="name" maxLength={200} defaultValue={str(p,"name")} className={field}/>)}{label("Contact",<input name="contact" maxLength={300} defaultValue={str(p,"contact")} className={field}/>)}{label("Credit limit in base currency",<input required name="limit" type="number" min="0" step="any" defaultValue={str(p,"credit_limit_base")} className={field}/>)}{label("Active",<input name="active" type="checkbox" defaultChecked={p.active!==false}/>)}<button className={button}>{t("Save")}</button></fieldset></form></details>)}</section>}
  {tab==="Payment accounts"&&role==="OWNER"&&<section className="space-y-4 rounded-xl border bg-white p-5"><div className="flex items-center justify-between"><h2 className="font-semibold">Payment accounts</h2><button className={button} disabled={disabled} onClick={()=>setCreateForm(createForm==="account"?null:"account")}>{createForm==="account"?"Cancel":"Create payment account"}</button></div><div className="grid gap-3 md:grid-cols-2">{accounts.map(a=><article className="rounded-lg border p-4" key={str(a,"id")}><h3 className="font-semibold">{str(a,"name")}</h3><p className="text-sm text-slate-600">{str(a,"method")} · {str(a,"currency")}</p><p className="text-sm">{str(a,"reference")}</p></article>)}</div>{!accounts.length&&<p className="text-sm text-slate-500">No payment accounts yet. Click Create payment account to add one.</p>}</section>}
  {tab==="Payment accounts"&&role==="OWNER"&&createForm==="account"&&<form className="grid gap-3 rounded-xl border bg-white p-5 md:grid-cols-2" onSubmit={e=>{const d=formData(e);post("payment-accounts",{name:value(d,"name"),method:value(d,"method"),currency:value(d,"currency"),reference:value(d,"reference")})}}><fieldset className="contents" disabled={disabled}>{label("Name",<input required name="name" maxLength={200} className={field}/>)}{label("Payment method",<select name="method" className={field}>{["CASH","BANK","MOBILE_MONEY","CARD"].map(x=><option key={x}>{x}</option>)}</select>)}{label("Currency",<select name="currency" className={field}>{currencies.filter(x=>x.active!==false).map(c=><option key={str(c,"currency")}>{str(c,"currency")}</option>)}</select>)}{label("Account number, mobile number or merchant code",<input name="reference" maxLength={200} className={field}/>)}<button className={button}>{t("Create")}</button></fieldset></form>}
  {(tab==="Sales history"||tab==="Purchase history / receiving")&&<section className="space-y-4 rounded-xl border bg-white p-5"><div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-700">{tab==="Sales history"?"Sales history. Open a sale to print its receipt, record a payment or manage a return.":"Goods receiving. Open a purchase, check the delivered quantities and confirm accepted goods. Only accepted goods are added to stock."}</div><input className={field} aria-label={t("Search")} placeholder={t("Search")} value={search} onChange={e=>setSearch(e.target.value)}/><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{["Reference","Type","Date",tab==="Sales history"?"Customer":"Supplier","Total","Outstanding"].map(x=><th className="p-2" key={x}>{t(x)}</th>)}</tr></thead><tbody>{documents.filter(d=>d.kind===(tab==="Sales history"?"SALE":"PURCHASE")).filter(d=>Object.values(d).join(" ").toLowerCase().includes(search.toLowerCase())).map(d=><tr className="border-t" key={str(d,"id")}><td className="p-2"><button className="text-emerald-800 underline" onClick={()=>api(`documents/${d.id}`).then(d=>setDetail(d as Row)).catch(e=>setError(e.message))}>{str(d,"document_number")} <span className="ml-2 text-xs">{d.kind==="PURCHASE"?"Check / receive goods":"View receipt"}</span></button></td>{["kind","created_at","party_name","total","outstanding"].map(key=><td className="p-2" key={key}>{str(d,key)} {key==="total"?str(d,"currency"):""}</td>)}</tr>)}</tbody></table></div>
   {detail&&<><div id="commerce-document-details" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-4"><p className="text-sm font-medium text-emerald-800">{detail.kind==="SALE"?"Sale saved. Print the receipt or reprint it later from Sales history.":"Purchase invoice saved. Check the quantities below, review the delivery and confirm received goods."}</p>{!storekeeper&&<div className="flex flex-wrap items-center gap-2"><select className={field} style={{width:"auto"}} value={paper} aria-label={t("Receipt format")} onChange={e=>setPaper(e.target.value)}><option value="58">58 mm</option><option value="80">80 mm</option><option value="A4">A4 / Save PDF</option></select><button className={button} disabled={disabled} onClick={()=>post(`documents/${detail.id}/print`,{})}>{t(detail.kind==="SALE"?"Print receipt":"Print invoice")}</button></div>}</div><CommerceInvoice detail={detail}/>
    {(financial||cashier)&&<details className="rounded-lg border p-4"><summary className="cursor-pointer font-medium">Payments and refunds</summary><form className="mt-3 grid gap-3 md:grid-cols-3" onSubmit={e=>{const d=formData(e);post("settlements",{documentId:detail.id,payment:{accountId:value(d,"account"),amount:value(d,"amount"),shiftId:value(d,"shift")||null,reference:value(d,"reference")},refund:value(d,"refund")==="on"})}}><fieldset disabled={disabled} className="contents">{label("Payment account",<select required name="account" className={field}>{options(accounts)}</select>)}{label("Amount",<input required type="number" min="0" step="any" name="amount" className={field}/>)}{label("Cashier shift",<select name="shift" className={field}>{options(shifts.filter(s=>!s.closed_at&&s.cashier_id===session?.user?.id),"account_name")}</select>)}{label("Reference",<input name="reference" maxLength={200} className={field}/>)}{financial&&label("Refund",<input name="refund" type="checkbox"/>)}<button className={button}>{t("Record payment")}</button></fieldset></form></details>}
    {rows(detail.lines).map(l=><div className="rounded border p-4" key={str(l,"id")}><p className="mb-3">{str(l,"description")} · {t("Received")}: {str(l,"received")} · {t("Returned")}: {str(l,"returned")}</p><div className="grid gap-3 md:grid-cols-2">
     {detail.kind==="PURCHASE"&&(manager||granted.has("STOREKEEPER"))&&<CommerceReceivingForm key={`${l.id}-${l.received}-${rows(detail.receivingExceptions).length}`} name={str(l,"description")} unit={str(l,"unit")} ordered={Number(l.quantity)} received={Number(l.received??0)} claimed={rows(detail.receivingExceptions).filter(x=>x.line_id===l.id).reduce((sum,x)=>sum+Number(x.damaged_quantity??0)+Number(x.missing_quantity??0),0)} disabled={disabled} onConfirm={body=>post("receiving",{documentId:detail.id,lineId:l.id,...body})}/>}
     {manager&&<details className="rounded-lg border p-3"><summary className="cursor-pointer text-sm font-medium">Record a return</summary><form className="mt-3 grid gap-2" onSubmit={e=>{const d=formData(e);post("returns",{documentId:detail.id,lineId:l.id,quantity:value(d,"quantity"),saleable:value(d,"saleable")==="on",reason:value(d,"reason")})}}><fieldset disabled={disabled} className="contents">{label("Return quantity",<input required name="quantity" type="number" min="0.000001" step="0.000001" className={field}/>)}{label("Reason",<input required name="reason" maxLength={500} className={field}/>)}{detail.kind==="SALE"&&label("Return to saleable stock",<input name="saleable" type="checkbox" defaultChecked/>)}<button className={secondary}>{t("Record return")}</button></fieldset></form></details>}</div></div>)}
    {detail.kind==="PURCHASE"&&rows(detail.receivingExceptions).map(x=><p key={str(x,"id")} className="rounded bg-amber-50 p-2 text-sm">Supplier claim: {str(x,"description")} · Damaged {str(x,"damaged_quantity")} {str(x,"unit")} · Missing {str(x,"missing_quantity")} {str(x,"unit")} · {str(x,"resolution")} · {str(x,"note")}</p>)}
    {rows(detail.returns).map(r=><p key={str(r,"id")}>{t("Returned")}: {str(r,"quantity")} · {str(r,"reason")} · {str(r,"created_at")}</p>)}
   </>}
  </section>}
  {tab==="Cashier shifts"&&<section className="space-y-4 rounded-xl border bg-white p-5">
   <h2 className="text-lg font-semibold">Cashier shifts</h2>
   {!state?.active&&<p className="rounded bg-amber-50 p-3 text-sm">The owner must enable Accounting and approve opening inventory valuation before shifts can open.</p>}
   {!branchId&&<p className="rounded bg-amber-50 p-3 text-sm">Select a branch before opening a shift.</p>}
   {!cashAccounts.length&&<div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm"><p>No active cash account exists for this business. Opening a cashier shift requires a cash payment account.</p>
    {role==="OWNER"?<form className="mt-3 grid gap-3 md:grid-cols-3" onSubmit={e=>{e.preventDefault();post("payment-accounts",{name:openingAccountName,method:"CASH",currency:openingAccountCurrency,reference:null})}}>
     {label("Cash account name",<input required className={field} value={openingAccountName} maxLength={200} onChange={e=>setOpeningAccountName(e.target.value)}/>)}
     {label("Currency",<select className={field} value={openingAccountCurrency} onChange={e=>setOpeningAccountCurrency(e.target.value)}>{currencies.filter(c=>c.active!==false).map(c=><option key={str(c,"currency")} value={str(c,"currency")}>{str(c,"currency")}</option>)}</select>)}
     <button className={button} disabled={disabled||!state?.active}>Create cash account</button></form>:
     <p className="mt-2 font-medium">Ask the owner to create a cash account under Payment accounts.</p>}
    </div>}
   {myOpenShift&&<p className="rounded bg-emerald-50 p-3 text-sm font-medium text-emerald-900">You already have an open shift on {str(myOpenShift,"account_name")}. Close it before opening another.</p>}
   {(manager||cashier)&&<form className="grid gap-3 md:grid-cols-3" onSubmit={e=>{const d=formData(e);post("shifts",{branchId,accountId:value(d,"account"),openingCash:value(d,"amount")})}}><fieldset disabled={disabled||!branchId||!state?.active||!cashAccounts.length||!!myOpenShift} className="contents">
    {label("Cash account",<select required name="account" className={field}>{options(cashAccounts)}</select>)}
    {label("Opening drawer cash (zero is allowed)",<input required defaultValue="0" name="amount" type="number" min="0" step="any" className={field}/>)}
    <button className={button}>{t("Open shift")}</button></fieldset></form>}
   {shifts.map(s=><article className="space-y-3 rounded border p-4" key={str(s,"id")}><p>{str(s,"account_name")} · {str(s,"opened_at")} · {str(s,"cashier_id")}</p><p>{t("Expected cash")}: {str(s,"expected")} {str(s,"currency")} · {t("Variance")}: {str(s,"variance")}</p>
    {!s.closed_at&&s.cashier_id===session?.user?.id&&(manager||cashier)&&<form className="flex gap-3" onSubmit={e=>{const d=formData(e);post(`shifts/${s.id}/close`,{countedCash:value(d,"amount")})}}><input required aria-label={t("Counted cash")} name="amount" type="number" min="0" step="any" className={field} disabled={disabled}/><button className={button} disabled={disabled}>{t("Close shift")}</button></form>}
    {!s.closed_at&&manager&&<form className="grid gap-3 md:grid-cols-3" onSubmit={e=>{const d=formData(e);post(`shifts/${s.id}/cash-moves`,{amount:value(d,"amount"),reason:value(d,"reason")})}}><fieldset className="contents" disabled={disabled}>{label("Cash deposit or withdrawal",<input required name="amount" type="number" step="any" className={field}/>)}{label("Reason",<input required name="reason" maxLength={500} className={field}/>)}<button className={secondary}>{t("Record cash movement")}</button></fieldset></form>}
    {!!s.closed_at&&manager&&!s.approved_at&&s.cashier_id!==session?.user?.id&&<button disabled={disabled} className={secondary} onClick={()=>post(`shifts/${s.id}/approve`,{})}>{t("Approve shift")}</button>}{!!s.approved_at&&<p>{t("Approved")}</p>}
   </article>)}
  </section>}
  {tab==="Operating expenses"&&financial&&<form className="grid gap-3 rounded-xl border bg-white p-5 md:grid-cols-2" onSubmit={e=>{const d=formData(e);post("expenses",{branchId,accountId:value(d,"account"),amount:value(d,"amount"),reason:value(d,"reason")})}}><fieldset className="contents" disabled={disabled||!branchId}>{label("Payment account",<select required name="account" className={field}>{options(accounts.filter(a=>a.method!=="CASH"))}</select>)}{label("Amount",<input required name="amount" type="number" min="0" step="any" className={field}/>)}{label("Reason",<input required name="reason" maxLength={500} className={field}/>)}<button className={button}>{t("Record expense")}</button></fieldset></form>}
  {tab==="Financial reports"&&financial&&<section className="space-y-4 rounded-xl border bg-white p-5"><div className="flex gap-3">{label("From",<input type="date" className={field} value={from} onChange={e=>setFrom(e.target.value)}/>)}{label("To",<input type="date" className={field} value={to} onChange={e=>setTo(e.target.value)}/>)}<button className={button} onClick={()=>api(`reports?from=${from}&to=${to}${branchId?`&branch=${branchId}`:""}`).then(r=>setReport(r as Row)).catch(e=>setError(e.message))}>{t("Generate report")}</button></div><p>{t("Journal amounts use base currency. Inventory shows current balances; journal totals use the selected dates.")}</p>
   {report&&<section className="space-y-3"><h3 className="text-lg font-semibold">Payment account balances</h3><p className="text-sm text-slate-600">Cumulative recorded balance through {str(report,"to")} for the selected branch scope. Includes receipts, supplier payments, refunds and expenses. Unrecorded opening funds and cash drawer floats are excluded. Each account is shown in its own currency.</p><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{rows(report.paymentAccounts).map(a=><article key={str(a,"id")} className="rounded-xl border bg-slate-50 p-4"><p className="text-xs font-semibold uppercase text-emerald-700">{a.method==="MOBILE_MONEY"?"MoMo":str(a,"method")}</p><h4 className="mt-1 font-semibold">{str(a,"name")}</h4><p className="mt-3 text-2xl font-semibold">{str(a,"balance")} <span className="text-sm">{str(a,"currency")}</span></p>{a.active===false&&<p className="text-xs text-slate-500">Inactive account</p>}</article>)}</div>{!rows(report.paymentAccounts).length&&<p>No payment accounts found.</p>}</section>}
   {report&&["summary","accounts","inventory","sales","collections","balances","products","returns"].map(section=><div key={section} className="overflow-x-auto"><h3 className="my-3 font-bold">{t(section==="accounts"?"Account balances":section==="inventory"?"Inventory valuation":section==="sales"?"Sales and purchases":section==="summary"?"Gross profit":section==="balances"?"Customer and supplier balances":section==="products"?"Product sales before returns":section==="returns"?"Returns":"Collections")}</h3><table className="w-full text-left text-xs"><thead><tr>{Object.keys(rows(report[section])[0]??{}).map(key=><th className="p-2" key={key}>{t(key.replaceAll("_"," "))}</th>)}</tr></thead><tbody>{rows(report[section]).map((row,index)=><tr className="border-t" key={index}>{Object.entries(row).map(([key,v])=><td className="p-2" key={key}>{String(v??"")}</td>)}</tr>)}</tbody></table></div>)}
  </section>}
  <style>{`@media print {body * {visibility:hidden} #commerce-receipt, #commerce-receipt * {visibility:visible} #commerce-receipt .invoice-grid {display:block} #commerce-receipt table {font-size:inherit;table-layout:fixed;width:100%} #commerce-receipt th,#commerce-receipt td {padding:2px;overflow-wrap:anywhere} #commerce-receipt {position:absolute;left:0;top:0;width:${paper==="A4"?"190mm":`${paper}mm`};padding:2mm;border:0;font-size:${paper==="58"?"9px":"11px"}} @page {margin:5mm;}`}</style>
 </div>;
}



