"use client";

import {useEffect,useState,FormEvent} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";

type Row=Record<string,unknown>;
type Unit={unit_name:string;units_per_package:string;purchase_package_price?:string|null;retail_package_price?:string|null;wholesale_package_price?:string|null};
type Props={businessId:string;branchId:string;products:Row[];categories:{id:string;name:string}[];onSaved:()=>Promise<void>};
const field="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm";
const money=(v:number)=>Number.isFinite(v)?v.toLocaleString(undefined,{maximumFractionDigits:4}):"—";
const decimal=(v:unknown)=>String(v??"");
function validAmount(value:string){return value.trim()!==""&&Number.isFinite(Number(value))&&Number(value)>=0;}
export default function CommerceProductEditor({businessId,branchId,products,categories,onSaved}:Props){
 const {t}=useLocale();
 const [id,setId]=useState("");const [busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState("");
 const [base,setBase]=useState("piece"),[units,setUnits]=useState<Unit[]>([]),[editingUnit,setEditingUnit]=useState("");
 const [packageName,setPackageName]=useState(""),[factor,setFactor]=useState(""),[purchase,setPurchase]=useState(""),[wholesale,setWholesale]=useState(""),[retail,setRetail]=useState("");
 const [minPrice,setMinPrice]=useState("0"),[tax,setTax]=useState("0"),[alert,setAlert]=useState("0");
 type ExtraUnit={name:string;factor:string;purchase:string;wholesale:string;retail:string};
 const [additionalUnits,setAdditionalUnits]=useState<ExtraUnit[]>([]);
 const [branchPrice,setBranchPrice]=useState(""),[effective,setEffective]=useState("");
 const product=products.find(x=>String(x.id)===id);const isNew=!product;
 const stockUnit=String(product?.base_unit??base);
 useEffect(()=>{let active=true;setUnits([]);setEditingUnit("");setPackageName("");setFactor("");
  setBase(String(product?.base_unit??"piece"));
  setRetail(decimal(product?.retail_price??""));setWholesale(decimal(product?.wholesale_price??""));
  setPurchase(decimal(product?.purchase_cost??""));setMinPrice(decimal(product?.minimum_price??0));setTax(decimal(product?.tax_rate??0));setAlert(decimal(product?.low_stock_quantity??0));
  setBranchPrice("");setEffective("");setAdditionalUnits([]);
  if(!id)return;
  fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"}).then(async r=>{
   if(!r.ok)throw new Error("Unable to load product units");return r.json() as Promise<Unit[]>;
  }).then(data=>{if(!active)return;setUnits(data);const first=data.find(x=>x.unit_name!==String(product?.base_unit));if(first){setEditingUnit(first.unit_name);setPackageName(first.unit_name);setFactor(first.units_per_package);setPurchase(decimal(first.purchase_package_price??""));setWholesale(decimal(first.wholesale_package_price??""));}}).catch(e=>{if(active)setError((e as Error).message)});
  return()=>{active=false};
 },[businessId,id]);
 const useUnit=(name:string)=>{setEditingUnit(name);const u=units.find(x=>x.unit_name===name);setPackageName(u?.unit_name??"");setFactor(decimal(u?.units_per_package));setPurchase(decimal(u?.purchase_package_price??(name?"":"")));setWholesale(decimal(u?.wholesale_package_price??""));};
 const hasPackage=packageName.trim()!=="";const n=Number(factor);
 const derived=hasPackage&&n>0&&validAmount(purchase)?Number(purchase)/n:null;
 async function save(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(busy)return;
  const data=new FormData(e.currentTarget),read=(name:string)=>String(data.get(name)??"").trim();
  if(hasPackage&&(!Number.isFinite(n)||n<=0)){setError("Enter how many stock units one package contains.");return;}
  if(!hasPackage&&factor.trim()!==""){setError("Select a purchase package name or clear the package quantity.");return;}
  if(!validAmount(purchase)){setError("Enter the buying price per selected purchase unit.");return;}
  if(!validAmount(retail)){setError(`Enter the retail price for one ${stockUnit}.`);return;}
  if(branchPrice&&!effective){setError("Choose an effective date for the optional branch price.");return;}
  if(hasPackage&&packageName.trim().toLowerCase()===stockUnit.toLowerCase()){setError("The purchase package must be different from the stock base unit.");return;}
  const additional=additionalUnits.filter(u=>u.name.trim());
  if(additional.some(u=>!validAmount(u.factor)||Number(u.factor)<=0||!validAmount(u.purchase))){setError("Every additional package requires its size and purchase price.");return;}
  const usedNames=[...(hasPackage?[packageName.trim().toLowerCase()]:[]),...additional.map(u=>u.name.trim().toLowerCase())];
  if(new Set(usedNames).size!==usedNames.length||usedNames.includes(stockUnit.toLowerCase())){setError("Each package needs a unique name different from the stock unit.");return;}
  const basePurchase=hasPackage?(Number(purchase)/n).toFixed(4):purchase;
  const baseWholesale=hasPackage&&validAmount(wholesale)?(Number(wholesale)/n).toFixed(4):(!hasPackage&&wholesale!==""?wholesale:null);
  const detail={name:read("name"),description:read("description"),categoryId:read("category")||null,barcode:read("barcode")||null,
   purchaseCost:basePurchase,retailPrice:retail,wholesalePrice:baseWholesale,minimumPrice:minPrice||"0",taxRate:tax||"0",lowStockQuantity:alert||"0",
   discountEnabled:data.has("discount_enabled"),cashierDiscountLimit:read("cashier_limit")||"0",
   returnWindowDays:Number(read("return_days")||"7"),fractionalUnits:data.has("fractional_units"),
   taxInclusive:data.has("tax_inclusive"),purchaseTaxRecoverable:data.has("purchase_tax_recoverable"),active:data.has("active")};
  const primary=hasPackage?{name:packageName.trim(),factor,purchasePackagePrice:purchase,
   wholesalePackagePrice:wholesale||null,retailPackagePrice:read("package_retail")||null}:null;
  const packages=[...(primary?[primary]:[]),...additional.map(u=>({name:u.name.trim(),factor:u.factor,
   purchasePackagePrice:u.purchase,wholesalePackagePrice:u.wholesale||null,retailPackagePrice:u.retail||null}))];
  const config={product:detail,packageSettings:packages,
   branchPrice:branchPrice?{branchId,price:branchPrice,effectiveAt:new Date(effective).toISOString()}:null};
  const path=isNew?`/api/commerce/businesses/${businessId}/products/full`:`/api/commerce/businesses/${businessId}/products/${id}/configuration`;
  const payload=isNew?{sku:read("sku"),baseUnit:stockUnit,...config}:config;
  setBusy(true);setError("");setNotice("");try{
   const response=await fetch(path,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
   if(response.status===401){window.location.replace(loginDestination("/commerce",true));return;}
   const result=await response.json().catch(()=>({}));if(!response.ok)throw new Error(String(result.message??`Save failed (HTTP ${response.status}).`));
   await onSaved();if(isNew)setId(String(result.id));setNotice(isNew?"Product created. Its stock remains zero until goods are received.":"Product saved successfully.");
  }catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 return <section className="space-y-5 rounded-xl border bg-white p-5">
  <header className="flex flex-wrap justify-between gap-3"><div><h2 className="text-xl font-semibold">{isNew?t("Create product"):t("Edit product")}</h2><p className="text-sm text-slate-600">One form for both actions. You enter real buying and selling prices; KBP handles conversions.</p></div>
   <label className="grid gap-1 text-sm">{t("Choose product to edit")}<select className={field} value={id} disabled={busy} onChange={e=>{setId(e.target.value);setError("");setNotice("")}}><option value="">+ Create new product</option>{products.map(p=><option key={String(p.id)} value={String(p.id)}>{String(p.name)} ({String(p.sku)})</option>)}</select></label></header>
  {error&&<p role="alert" className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">{error}</p>}{notice&&<p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}
  <form key={id||"new"} onSubmit={save} className="space-y-5"><fieldset disabled={busy} className="space-y-5">
   <div className="space-y-3"><h3 className="border-b pb-2 font-semibold">Product information</h3><div className="grid gap-3 md:grid-cols-3">
    <label className="grid gap-1 text-sm">SKU<input required name="sku" maxLength={80} className={field} defaultValue={String(product?.sku??"")} readOnly={!isNew}/></label>
    <label className="grid gap-1 text-sm">Product name<input required name="name" maxLength={200} className={field} defaultValue={String(product?.name??"")}/></label>
    <label className="grid gap-1 text-sm">Barcode (optional)<input name="barcode" maxLength={120} className={field} defaultValue={String(product?.barcode??"")}/></label>
    <label className="grid gap-1 text-sm">Category<select name="category" className={field} defaultValue={String(product?.category_id??"")}><option value="">—</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
    <label className="grid gap-1 text-sm">Smallest stock unit<select name="base_unit" className={field} value={stockUnit} disabled={!isNew} onChange={e=>setBase(e.target.value)}>{[...new Set(["bottle","kg","piece","liter","gram","meter",stockUnit])].map(u=><option key={u} value={u}>{u}</option>)}</select><span className="text-xs text-slate-500">Stock unit cannot change after creation.</span></label>
    <label className="grid gap-1 text-sm">Description<input name="description" className={field} defaultValue={String(product?.description??"")}/></label>
   </div>{!isNew&&["CRATE","BAG","BOX"].includes(stockUnit.toUpperCase())&&<p className="rounded bg-amber-50 p-3 text-sm text-amber-900">This existing product tracks stock in {stockUnit}s. Do not enter the number of bottles/kg inside a package as the stock quantity. Preserve historical stock; create a new base-unit product after reconciliation if you need to sell individual contents.</p>}</div>
   <div className="space-y-3"><h3 className="border-b pb-2 font-semibold">How you buy it</h3>
    {!isNew&&<label className="grid gap-1 text-sm">Choose an existing package to edit<select className={field} value={editingUnit} onChange={e=>useUnit(e.target.value)}><option value="">+ Use base unit / create new package</option>{units.filter(u=>u.unit_name!==stockUnit).map(u=><option key={u.unit_name} value={u.unit_name}>{u.unit_name} ({u.units_per_package} {stockUnit})</option>)}</select></label>}
    <div className="grid gap-3 md:grid-cols-3"><label className="grid gap-1 text-sm">Purchase package (optional)<input className={field} value={packageName} onChange={e=>{setPackageName(e.target.value);if(!e.target.value.trim())setFactor("")}} placeholder="crate, bag, box, roll…"/></label>
     {hasPackage&&<label className="grid gap-1 text-sm">Number of {stockUnit} per {packageName}<input type="number" min="0.000001" step="any" required className={field} value={factor} onChange={e=>setFactor(e.target.value)}/></label>}
     <label className="grid gap-1 text-sm">Buying price per {hasPackage?packageName:stockUnit}<input type="number" min="0" step="any" required className={field} value={purchase} onChange={e=>setPurchase(e.target.value)} placeholder="Example: 10000"/></label>
    </div>
    {hasPackage&&derived!==null&&<p className="rounded-lg bg-slate-50 p-3 text-sm">Calculated cost per {stockUnit}: <strong>{money(derived)}</strong>. This is automatic; you do not enter it again.</p>}
    {additionalUnits.map((unit,index)=><div key={index} className="grid gap-3 rounded border bg-slate-50 p-3 md:grid-cols-5">
     <label className="grid gap-1 text-sm">Additional package name<input className={field} value={unit.name} placeholder="box, carton, pack" onChange={e=>setAdditionalUnits(old=>old.map((x,i)=>i===index?{...x,name:e.target.value}:x))}/></label>
     {([ ["factor","Base units per package"],["purchase","Buying price per package"],["wholesale","Wholesale price per package"],["retail","Retail price per package"] ] as const).map(([key,title])=><label key={key} className="grid gap-1 text-sm">{title}<input className={field} type="number" min="0" step="any" value={unit[key]} onChange={e=>setAdditionalUnits(old=>old.map((x,i)=>i===index?{...x,[key]:e.target.value}:x))}/></label>)}
     <button type="button" className="rounded border px-3 py-2 text-sm md:col-span-5" onClick={()=>setAdditionalUnits(old=>old.filter((_,i)=>index!==i))}>Remove this package</button>
    </div>)}
    <button type="button" className="rounded-lg border px-3 py-2 text-sm" onClick={()=>setAdditionalUnits(old=>[...old,{name:"",factor:"",purchase:"",wholesale:"",retail:""}])}>+ Add another package size</button>
   </div>
   <div className="space-y-3"><h3 className="border-b pb-2 font-semibold">How you sell it</h3><div className="grid gap-3 md:grid-cols-3">
    <label className="grid gap-1 text-sm">Retail selling price per {stockUnit}<input type="number" min="0" step="any" required className={field} value={retail} onChange={e=>setRetail(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">Wholesale price per {hasPackage?packageName:stockUnit}<input type="number" min="0" step="any" className={field} value={wholesale} onChange={e=>setWholesale(e.target.value)}/></label>
    {hasPackage&&<label className="grid gap-1 text-sm">Retail price per {packageName} (optional)<input type="number" min="0" step="any" name="package_retail" className={field} defaultValue={units.find(u=>u.unit_name===editingUnit)?.retail_package_price??""}/></label>}
    <label className="grid gap-1 text-sm">Minimum selling price per {stockUnit}<input type="number" min="0" step="any" className={field} value={minPrice} onChange={e=>setMinPrice(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">Tax rate (%)<input type="number" min="0" max="100" step="any" className={field} value={tax} onChange={e=>setTax(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">Low-stock alert (in {stockUnit})<input type="number" min="0" step="any" className={field} value={alert} onChange={e=>setAlert(e.target.value)}/></label>
   </div><div className="flex flex-wrap gap-4 text-sm">{([ ["active","Active",true], ["tax_inclusive","Tax inclusive",false], ["purchase_tax_recoverable","Recoverable purchase tax",false] ] as const).map(([name,label,initial])=><label key={name} className="flex items-center gap-2"><input type="checkbox" name={name} defaultChecked={isNew?initial:Boolean(product?.[name])}/>{label}</label>)}</div></div>
   <details className="rounded-lg border p-3"><summary className="cursor-pointer font-semibold">Discount and return rules (optional)</summary><div className="mt-3 grid gap-3 md:grid-cols-3">
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="discount_enabled" defaultChecked={isNew?true:Boolean(product?.discount_enabled)}/>Allow authorized discounts</label>
    <label className="grid gap-1 text-sm">Maximum cashier discount (%)<input name="cashier_limit" className={field} type="number" min="0" max="100" step="any" defaultValue={String(product?.cashier_discount_limit??0)}/></label>
    <label className="grid gap-1 text-sm">Customer return period (days)<input name="return_days" className={field} type="number" min="0" max="3650" defaultValue={String(product?.return_window_days??7)}/></label>
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="fractional_units" defaultChecked={isNew?true:product?.fractional_units!==false}/>Allow quantities that convert to fractional base units</label>
    <p className="md:col-span-3 text-xs text-slate-500">Manager discounts still require a reason; amounts below the minimum selling price are rejected. An item such as a phone should not allow fractional units.</p>
   </div></details>
   <details className="rounded-lg border p-3"><summary className="cursor-pointer font-semibold">Branch-specific price (optional)</summary><div className="mt-3 grid gap-3 md:grid-cols-2"><label className="grid gap-1 text-sm">Selling price per {stockUnit}<input type="number" min="0" step="any" className={field} disabled={!branchId} value={branchPrice} onChange={e=>setBranchPrice(e.target.value)}/></label><label className="grid gap-1 text-sm">Effective from<input type="datetime-local" className={field} disabled={!branchId} value={effective} onChange={e=>setEffective(e.target.value)}/></label></div></details>
   <button type="submit" className="rounded-lg bg-emerald-700 px-8 py-3 font-semibold text-white disabled:opacity-50">{busy?"Saving…":"Save product"}</button>
  </fieldset></form>
 </section>;
}
