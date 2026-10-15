"use client";

import {useEffect,useState,FormEvent} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";

type Row=Record<string,unknown>;
type Unit={unit_name:string;units_per_package:string;purchase_package_price?:string|null;retail_package_price?:string|null;wholesale_package_price?:string|null};
type Props={businessId:string;branchId:string;products:Row[];categories:{id:string;name:string}[];initialProductId?:string;onCancel:()=>void;onSaved:()=>Promise<void>};
const field="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none focus:ring-2 focus:ring-emerald-100";
const stockChoices=[
 ["piece","Piece"],["bottle","Bottle"],["kg","Kilogram (kg)"],["gram","Gram (g)"],
 ["liter","Liter (L)"],["milliliter","Milliliter (mL)"],["meter","Meter (m)"],
 ["centimeter","Centimeter (cm)"],["egg","Egg"],["tablet","Tablet"],["pair","Pair"],
 ["unit","Unit"],["can","Can"],["sheet","Sheet"],["other","Other / custom unit"]
] as const;
const packageChoices=[
 ["crate","Crate"],["bag","Bag"],["box","Box"],["carton","Carton"],
 ["pack","Pack"],["tray","Tray"],["roll","Roll"],["container","Container"],
 ["bundle","Bundle"],["case","Case"],["pallet","Pallet"],["sack","Sack"],
 ["dozen","Dozen"],["jerrycan","Jerrycan"],["drum","Drum"],["tin","Tin"],
 ["bottle","Bottle"],["tube","Tube"],["other","Other / custom package"]
] as const;
const isStockChoice=(unit:string)=>stockChoices.some(([id])=>id===unit);
const isPackageChoice=(unit:string)=>packageChoices.some(([id])=>id===unit);
const section="rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5";
const money=(v:number)=>Number.isFinite(v)?v.toLocaleString(undefined,{maximumFractionDigits:4}):"—";
const decimal=(v:unknown)=>String(v??"");
function validAmount(value:string){return value.trim()!==""&&Number.isFinite(Number(value))&&Number(value)>=0;}
export default function CommerceProductEditor({businessId,branchId,products,categories,initialProductId="",onCancel,onSaved}:Props){
 const {t}=useLocale();
 const [id,setId]=useState(initialProductId);const [busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState("");
 const [base,setBase]=useState("piece"),[units,setUnits]=useState<Unit[]>([]),[editingUnit,setEditingUnit]=useState("");
 const [customStock,setCustomStock]=useState(false),[customPackage,setCustomPackage]=useState(false);
 const [packageName,setPackageName]=useState(""),[factor,setFactor]=useState(""),[purchase,setPurchase]=useState(""),[wholesale,setWholesale]=useState(""),[retail,setRetail]=useState("");
 const [minPrice,setMinPrice]=useState("0"),[tax,setTax]=useState("0"),[alert,setAlert]=useState("0");
 type ExtraUnit={name:string;factor:string;purchase:string;wholesale:string;retail:string};
 const [additionalUnits,setAdditionalUnits]=useState<ExtraUnit[]>([]);
 const [branchPrice,setBranchPrice]=useState(""),[effective,setEffective]=useState("");
 const product=products.find(x=>String(x.id)===id);const isNew=!product;
 const stockUnit=String(product?.base_unit??base);
 useEffect(()=>{let active=true;setUnits([]);setEditingUnit("");setPackageName("");setFactor("");
  setBase(String(product?.base_unit??"piece"));
  setCustomStock(!isStockChoice(String(product?.base_unit??"piece")));setCustomPackage(false);
  setRetail(decimal(product?.retail_price??""));setWholesale(decimal(product?.wholesale_price??""));
  setPurchase(decimal(product?.purchase_cost??""));setMinPrice(decimal(product?.minimum_price??0));setTax(decimal(product?.tax_rate??0));setAlert(decimal(product?.low_stock_quantity??0));
  setBranchPrice("");setEffective("");setAdditionalUnits([]);
  if(!id)return;
  fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"}).then(async r=>{
   if(!r.ok)throw new Error("Unable to load product units");return r.json() as Promise<Unit[]>;
  }).then(data=>{if(!active)return;setUnits(data);const first=data.find(x=>x.unit_name!==String(product?.base_unit));if(first){setEditingUnit(first.unit_name);setPackageName(first.unit_name);setCustomPackage(!isPackageChoice(first.unit_name.toLowerCase()));setFactor(first.units_per_package);setPurchase(decimal(first.purchase_package_price??""));setWholesale(decimal(first.wholesale_package_price??""));}}).catch(e=>{if(active)setError((e as Error).message)});
  return()=>{active=false};
 },[businessId,id]);
 const useUnit=(name:string)=>{setEditingUnit(name);const u=units.find(x=>x.unit_name===name);
  setPackageName(u?.unit_name??"");setCustomPackage(Boolean(u&&!isPackageChoice(u.unit_name.toLowerCase())));
  setFactor(decimal(u?.units_per_package));setPurchase(decimal(u?.purchase_package_price??(!name?product?.purchase_cost:"")));
  setWholesale(decimal(u?.wholesale_package_price??(!name?product?.wholesale_price:"")));
 };
 const hasPackage=packageName.trim()!=="";const n=Number(factor);
 const derived=hasPackage&&n>0&&validAmount(purchase)?Number(purchase)/n:null;
 async function save(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(busy)return;
  const data=new FormData(e.currentTarget),read=(name:string)=>String(data.get(name)??"").trim();
  if(!stockUnit.trim()||stockUnit==="other"){setError("Select a stock unit or enter a custom stock unit name.");return;}
  if(customPackage&&!hasPackage){setError("Enter the name of your custom purchase package.");return;}
  if(hasPackage&&(!Number.isFinite(n)||n<=0)){setError("Enter how many stock units one package contains.");return;}
  if(!hasPackage&&factor.trim()!==""){setError("Select a purchase package name or clear the package quantity.");return;}
  if(!validAmount(purchase)){setError("Enter the buying price per selected purchase unit.");return;}
  if(!validAmount(retail)){setError(`Enter the retail price for one ${stockUnit}.`);return;}
  if(branchPrice&&!effective){setError("Choose an effective date for the optional branch price.");return;}
  if(hasPackage&&packageName.trim().toLowerCase()===stockUnit.toLowerCase()){setError("The purchase package must be different from the stock base unit.");return;}
  const additional=additionalUnits.filter(u=>u.name.trim());
  if(additional.some(u=>!validAmount(u.factor)||Number(u.factor)<=0||(u.purchase!==""&&!validAmount(u.purchase)))){setError("Every additional package needs a positive size; prices are optional.");return;}
  const usedNames=[...(hasPackage?[packageName.trim().toLowerCase()]:[]),...additional.map(u=>u.name.trim().toLowerCase())];
  if(new Set(usedNames).size!==usedNames.length||usedNames.includes(stockUnit.toLowerCase())){setError("Each package needs a unique name different from the stock unit.");return;}
  // Submitted values are in the business's everyday units; the base-unit cost
  // is calculated for the existing inventory ledger. Posted invoices preserve
  // their own actual package prices and the source unit conversion.
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
   purchasePackagePrice:u.purchase||null,wholesalePackagePrice:u.wholesale||null,retailPackagePrice:u.retail||null}))];
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
 return <section className="space-y-5 rounded-2xl border border-slate-200 bg-slate-50 p-4 shadow-sm sm:p-6">
  <header className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 pb-4"><div className="space-y-1"><h2 className="text-xl font-semibold text-slate-900">{isNew?t("Create product"):t("Edit product")}</h2><p className="text-sm text-slate-600">{t("Enter your normal purchase and selling prices once. KBP converts quantities and prices automatically.")}</p></div>
   <button type="button" disabled={busy} onClick={onCancel} className="rounded-lg border bg-white px-4 py-2 text-sm">{t("Cancel")}</button></header>
  {error&&<p role="alert" className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">{error}</p>}{notice&&<p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}
  <form key={id||"new"} onSubmit={save} className="space-y-4"><fieldset disabled={busy} className="space-y-4">
   <div className={section+" space-y-4"}><div><h3 className="font-semibold text-slate-900">{t("1. Product information")}</h3><p className="mt-1 text-xs text-slate-500">{t("Choose how this product is counted in stock. You can purchase it in larger packages.")}</p></div><div className="grid gap-3 md:grid-cols-3">
    <label className="grid gap-1 text-sm">{t("SKU")}<input required name="sku" maxLength={80} className={field} defaultValue={String(product?.sku??"")} readOnly={!isNew}/></label>
    <label className="grid gap-1 text-sm">{t("Product name")}<input required name="name" maxLength={200} className={field} defaultValue={String(product?.name??"")}/></label>
    <label className="grid gap-1 text-sm">{t("Barcode (optional)")}<input name="barcode" maxLength={120} className={field} defaultValue={String(product?.barcode??"")}/></label>
    <label className="grid gap-1 text-sm">{t("Category")}<select name="category" className={field} defaultValue={String(product?.category_id??"")}><option value="">—</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
    <label className="grid gap-1 text-sm">{t("Smallest tracked unit")}<select aria-label="Smallest tracked unit" name="base_unit" className={field} value={customStock?"other":stockUnit} disabled={!isNew} onChange={e=>{setCustomStock(e.target.value==="other");setBase(e.target.value==="other"?"":e.target.value)}}>{stockChoices.map(([u,label])=><option key={u} value={u}>{label}</option>)}</select><span className="text-xs text-slate-500">{t("Stock is stored in this unit; it cannot change after creation.")}</span></label>
    {customStock&&<label className="grid gap-1 text-sm">{t("Custom stock unit")}<input className={field} required disabled={!isNew} maxLength={32} placeholder="e.g. sheet" value={base} onChange={e=>setBase(e.target.value)}/></label>}
    <label className="grid gap-1 text-sm">{t("Description")}<input name="description" className={field} defaultValue={String(product?.description??"")}/></label>
   </div>{!isNew&&["CRATE","BAG","BOX"].includes(stockUnit.toUpperCase())&&<p className="rounded bg-amber-50 p-3 text-sm text-amber-900">{t("This existing product tracks stock in")} {stockUnit}{t("s. Do not enter the number of bottles/kg inside a package as the stock quantity. Preserve historical stock; create a new base-unit product after reconciliation if you need to sell individual contents.")}</p>}</div>
   <div className={section+" space-y-4"}><div><h3 className="font-semibold text-slate-900">{t("2. How you purchase it")}</h3><p className="mt-1 text-xs text-slate-500">{t("Enter the amount paid for the actual purchase unit; no division required.")}</p></div>
    {!isNew&&<label className="grid gap-1 text-sm">{t("Choose an existing package to edit")}<select className={field} value={editingUnit} onChange={e=>useUnit(e.target.value)}><option value="">{t("+ Use base unit / create new package")}</option>{units.filter(u=>u.unit_name!==stockUnit).map(u=><option key={u.unit_name} value={u.unit_name}>{u.unit_name} ({u.units_per_package} {stockUnit})</option>)}</select></label>}
    <div className="grid gap-3 md:grid-cols-3"><label className="grid gap-1 text-sm">{t("Purchase unit")}<select aria-label="Purchase unit" className={field} value={hasPackage?(customPackage?"other":packageName.toLowerCase()):""} onChange={e=>{
      const choice=e.target.value;
      if(choice==="other"){setCustomPackage(true);setEditingUnit("");setPackageName("");setFactor("");setPurchase("");setWholesale("");}
      else if(!choice){useUnit("");}
      else {const existing=units.find(u=>u.unit_name.toLowerCase()===choice.toLowerCase());
       if(existing)useUnit(existing.unit_name);
       else {setEditingUnit("");setCustomPackage(false);setPackageName(choice);setFactor("");setPurchase("");setWholesale("");}
      }
     }}><option value="">{`Individual ${stockUnit} (no package)`}</option>{packageChoices.filter(([v])=>v!=="other"&&v!==stockUnit.toLowerCase()).map(([v,l])=><option key={v} value={v}>{l}</option>)}<option value="other">{t("Other / custom package")}</option></select></label>
     {customPackage&&<label className="grid gap-1 text-sm">{t("Custom package name")}<input required className={field} maxLength={32} value={packageName} onChange={e=>setPackageName(e.target.value)} placeholder="e.g. sack, bundle, case"/></label>}
     {hasPackage&&<label className="grid gap-1 text-sm">{t("How many")} {stockUnit} {t("in one")} {packageName}?<input type="number" min="0.000001" step="any" required className={field} value={factor} onChange={e=>setFactor(e.target.value)}/></label>}
     <label className="grid gap-1 text-sm">{t("Registered purchase cost per")} {hasPackage?packageName:stockUnit}<input type="number" min="0" step="any" required className={field} value={purchase} onChange={e=>setPurchase(e.target.value)} placeholder="Example: 10000"/></label>
    </div>
    {hasPackage&&derived!==null&&<p className="rounded-lg bg-slate-50 p-3 text-sm">{t("Automatic cost per")} {stockUnit}: <strong>{money(derived)}</strong> {t("(from the")} {packageName} {t("purchase price). You do not need to calculate or enter this.")}</p>}
    {additionalUnits.map((unit,index)=><div key={index} className="grid gap-3 rounded border bg-slate-50 p-3 md:grid-cols-5">
     <label className="grid gap-1 text-sm">{t("Additional unit name")}<input className={field} list="commerce-package-suggestions" value={unit.name} placeholder="Choose or type a custom unit" onChange={e=>setAdditionalUnits(old=>old.map((x,i)=>i===index?{...x,name:e.target.value}:x))}/><datalist id="commerce-package-suggestions">{packageChoices.filter(([v])=>v!=="other"&&v!==stockUnit.toLowerCase()).map(([v,l])=><option value={v} key={v} label={l}/>)}</datalist></label>
     {([ ["factor","Base units per package"],["purchase","Buying price per package"],["wholesale","Wholesale price per package"],["retail","Retail price per package"] ] as const).map(([key,title])=><label key={key} className="grid gap-1 text-sm">{title}<input className={field} type="number" min="0" step="any" value={unit[key]} onChange={e=>setAdditionalUnits(old=>old.map((x,i)=>i===index?{...x,[key]:e.target.value}:x))}/></label>)}
     <button type="button" className="rounded border px-3 py-2 text-sm md:col-span-5" onClick={()=>setAdditionalUnits(old=>old.filter((_,i)=>index!==i))}>{t("Remove this package")}</button>
    </div>)}
    <button type="button" className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm hover:bg-slate-50" onClick={()=>setAdditionalUnits(old=>[...old,{name:"",factor:"",purchase:"",wholesale:"",retail:""}])}>{t("+ Add another buying or selling unit")}</button>
   </div>
   <div className={section+" space-y-4"}><div><h3 className="font-semibold text-slate-900">{t("3. How you sell it")}</h3><p className="mt-1 text-xs text-slate-500">{t("Retail can be per individual stock unit, while wholesale can be per package.")}</p></div><div className="grid gap-3 md:grid-cols-3">
    <label className="grid gap-1 text-sm">{t("Retail price for 1")} {stockUnit}<input type="number" min="0" step="any" required className={field} value={retail} onChange={e=>setRetail(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">{t("Wholesale price for 1")} {hasPackage?packageName:stockUnit}<input type="number" min="0" step="any" className={field} value={wholesale} onChange={e=>setWholesale(e.target.value)}/></label>
    {hasPackage&&<label className="grid gap-1 text-sm">{t("Retail price per")} {packageName} {t("(optional)")}<input type="number" min="0" step="any" name="package_retail" key={editingUnit||packageName} className={field} defaultValue={units.find(u=>u.unit_name===editingUnit)?.retail_package_price??""}/></label>}
    <label className="grid gap-1 text-sm">{t("Minimum selling price per")} {stockUnit}<input type="number" min="0" step="any" className={field} value={minPrice} onChange={e=>setMinPrice(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">{t("Tax rate (%)")}<input type="number" min="0" max="100" step="any" className={field} value={tax} onChange={e=>setTax(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">{t("Low-stock alert (in")} {stockUnit})<input type="number" min="0" step="any" className={field} value={alert} onChange={e=>setAlert(e.target.value)}/></label>
   </div><div className="flex flex-wrap gap-4 text-sm">{([ ["active","Active",true], ["tax_inclusive","Tax inclusive",false], ["purchase_tax_recoverable","Recoverable purchase tax",false] ] as const).map(([name,label,initial])=><label key={name} className="flex items-center gap-2"><input type="checkbox" name={name} defaultChecked={isNew?initial:Boolean(product?.[name])}/>{label}</label>)}</div></div>
   <details className="rounded-lg border p-3"><summary className="cursor-pointer font-semibold">{t("Discount and return rules (optional)")}</summary><div className="mt-3 grid gap-3 md:grid-cols-3">
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="discount_enabled" defaultChecked={isNew?true:Boolean(product?.discount_enabled)}/>{t("Allow authorized discounts")}</label>
    <label className="grid gap-1 text-sm">{t("Maximum cashier discount (%)")}<input name="cashier_limit" className={field} type="number" min="0" max="100" step="any" defaultValue={String(product?.cashier_discount_limit??0)}/></label>
    <label className="grid gap-1 text-sm">{t("Customer return period (days)")}<input name="return_days" className={field} type="number" min="0" max="3650" defaultValue={String(product?.return_window_days??7)}/></label>
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" name="fractional_units" defaultChecked={isNew?["kg","gram","liter","milliliter","meter","centimeter"].includes(stockUnit.toLowerCase()):product?.fractional_units!==false}/>{t("Allow fractional stock units (e.g., 0.5 kg; not 0.5 phone)")}</label>
    <p className="md:col-span-3 text-xs text-slate-500">{t("Manager discounts still require a reason; amounts below the minimum selling price are rejected. An item such as a phone should not allow fractional units.")}</p>
   </div></details>
   <details className="rounded-lg border p-3"><summary className="cursor-pointer font-semibold">{t("Branch-specific price (optional)")}</summary><div className="mt-3 grid gap-3 md:grid-cols-2"><label className="grid gap-1 text-sm">{t("Selling price per")} {stockUnit}<input type="number" min="0" step="any" className={field} disabled={!branchId} value={branchPrice} onChange={e=>setBranchPrice(e.target.value)}/></label><label className="grid gap-1 text-sm">{t("Effective from")}<input type="datetime-local" className={field} disabled={!branchId} value={effective} onChange={e=>setEffective(e.target.value)}/></label></div></details>
   <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3"><p className="text-xs text-emerald-900">{t("One Save updates the product and its units together. Stock is added only through goods receiving.")}</p><button type="submit" className="rounded-lg bg-emerald-700 px-8 py-3 font-semibold text-white hover:bg-emerald-800 disabled:opacity-50">{busy?"Saving…":"Save product"}</button></div>
  </fieldset></form>
 </section>;
}
