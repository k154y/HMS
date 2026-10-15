"use client";
import {useEffect,useState,FormEvent} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";
type Row=Record<string,unknown>;
type Unit={unit_name:string;units_per_package:string;retail_package_price?:string|null;wholesale_package_price?:string|null};
const input="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm focus:border-emerald-600 focus:outline-none";
const numberFields=["purchase_cost","retail_price","wholesale_price","minimum_price","tax_rate","low_stock_quantity"];
const requiredFields=["name","purchase_cost","retail_price","minimum_price","tax_rate","low_stock_quantity"];
export default function CommerceProductEditor({businessId,branchId,products,categories,onSaved}:{businessId:string;branchId:string;products:Row[];categories:{id:string;name:string}[];onSaved:()=>Promise<void>}){
 const {t}=useLocale();const [newBase,setNewBase]=useState("bottle"),[id,setId]=useState(""),[busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState(""),[units,setUnits]=useState<Unit[]>([]),[selectedUnit,setSelectedUnit]=useState("");
 const product=products.find(p=>p.id===id);const isNew=!product;const base=String(product?.base_unit??newBase);
 useEffect(()=>{let active=true;setUnits([]);setSelectedUnit("");if(!id)return;
  fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"}).then(async r=>{if(!r.ok)throw new Error("Unable to load selling units");return r.json() as Promise<Unit[]>}).then(u=>{if(active)setUnits(u)}).catch(e=>{if(active)setError((e as Error).message)});
  return()=>{active=false};},[businessId,id]);
 const selected=units.find(u=>u.unit_name===selectedUnit);
 async function save(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(busy)return;const d=new FormData(e.currentTarget),s=(k:string)=>String(d.get(k)??"").trim();
  const packName=s("package_name"),factor=s("package_factor"),branchPrice=s("branch_price"),effective=s("effective");
  if(packName&&!factor){setError("Enter how many base units are in the selling unit.");return;}
  if(!packName&&(factor||s("package_retail_price")||s("package_wholesale_price"))){setError("Enter the selling unit name (for example crate or bag).");return;}
  if(branchPrice&&!effective){setError("Choose the effective date for the optional branch price.");return;}
  const detail={name:s("name"),description:s("description"),categoryId:s("category")||null,barcode:s("barcode")||null,purchaseCost:s("purchase_cost"),retailPrice:s("retail_price"),wholesalePrice:s("wholesale_price")||null,minimumPrice:s("minimum_price"),taxRate:s("tax_rate"),taxInclusive:d.has("tax_inclusive"),purchaseTaxRecoverable:d.has("purchase_tax_recoverable"),lowStockQuantity:s("low_stock_quantity"),active:d.has("active")};
  const configuration={product:detail,packageSetting:packName?{name:packName,factor,retailPackagePrice:s("package_retail_price")||null,wholesalePackagePrice:s("package_wholesale_price")||null}:null,branchPrice:branchPrice?{branchId,price:branchPrice,effectiveAt:new Date(effective).toISOString()}:null};
  const path=isNew?`/api/commerce/businesses/${businessId}/products/full`:`/api/commerce/businesses/${businessId}/products/${id}/configuration`;
  const payload=isNew?{sku:s("sku"),baseUnit:s("base_unit"),...configuration}:configuration;
  setBusy(true);setError("");setNotice("");try{
   const r=await fetch(path,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
   if(r.status===401){window.location.replace(loginDestination("/commerce",true));return;}
   const result=await r.json().catch(()=>({}));if(!r.ok)throw new Error(String(result.message??`Save failed (HTTP ${r.status}). Check role and product prices.`));
   await onSaved();if(isNew)setId(String(result.id));setNotice(isNew?"Product created successfully.":"Product updated successfully.");
   if(!isNew){const u=await fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"});if(u.ok)setUnits(await u.json() as Unit[]);}
  }catch(e){setError((e as Error).message)}finally{setBusy(false);}
 }
 const defaultValue=(key:string)=>String(product?.[key]??(numberFields.includes(key)?"0":""));
 return <section className="mt-5 space-y-5 rounded-xl border border-slate-200 bg-white p-5">
  <header className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-xl font-semibold">{t(isNew?"Create product":"Edit product")}</h2><p className="mt-1 text-sm text-slate-600">Same form for new and existing products. One Save updates all fields together.</p></div><label className="text-sm">{t("Choose product to edit")}<select aria-label="Choose product to edit" className={input} value={id} disabled={busy} onChange={e=>{setId(e.target.value);setError("");setNotice("")}}><option value="">+ Create new product</option>{products.map(p=><option key={String(p.id)} value={String(p.id)}>{String(p.name)} ({String(p.sku)})</option>)}</select></label></header>
  {error&&<p role="alert" className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">{error}</p>}{notice&&<p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}
  <form key={id||"create"} onSubmit={save} className="space-y-6"><fieldset disabled={busy} className="space-y-6">
   <section className="space-y-3"><h3 className="border-b pb-2 font-semibold">Product information</h3><div className="grid gap-3 md:grid-cols-3">
    <label className="grid gap-1 text-sm">{t("SKU")}<input name="sku" className={input} required maxLength={80} defaultValue={String(product?.sku??"")} readOnly={!isNew} title={!isNew?"SKU cannot be changed after creation":undefined}/></label>
    {([ ["name","Product name"],["barcode","Barcode (optional)"],["description","Description"] ] as const).map(([key,label])=><label key={key} className="grid gap-1 text-sm">{t(label)}<input name={key} className={input} required={requiredFields.includes(key)} maxLength={key==="description"?2000:key==="barcode"?120:200} defaultValue={defaultValue(key)}/></label>)}
    <label className="grid gap-1 text-sm">{t("Category")}<select name="category" className={input} defaultValue={String(product?.category_id??"")}><option value="">—</option>{categories.map(c=><option value={c.id} key={c.id}>{c.name}</option>)}</select></label>
    <label className="grid gap-1 text-sm">Stock base unit<select name="base_unit" className={input} value={base} onChange={e=>setNewBase(e.target.value)} disabled={!isNew}>{[...new Set(["bottle","kg","piece","liter","gram","meter","crate","bag",base])].map(u=><option key={u}>{u}</option>)}</select>{!isNew&&<span className="text-xs text-slate-500">Fixed to preserve existing stock records.</span>}</label>
   </div>{!isNew&&["CRATE","BAG"].includes(base.toUpperCase())&&<p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">This product was created with {base} as the stock base unit. Do not enter 24 bottles as 24 crates. To track individual bottles/kg accurately, create a new product with <strong>bottle</strong> or <strong>kg</strong> as its base unit. Existing stock and sales must be reconciled before replacing the old product.</p>}</section>
   <section className="space-y-3"><h3 className="border-b pb-2 font-semibold">Pricing and tax</h3><p className="text-sm text-slate-600">Enter the price of <strong>one {base}</strong>, including the price when sold individually. Bulk unit prices are set below.</p><div className="grid gap-3 md:grid-cols-3">
    {([ ["purchase_cost","Purchase cost per base unit"],["retail_price","Retail price per base unit"],["wholesale_price","Wholesale price per base unit"],["minimum_price","Minimum selling price per base unit"],["tax_rate","Tax rate (%)"],["low_stock_quantity","Low stock alert quantity"] ] as const).map(([key,label])=><label className="grid gap-1 text-sm" key={key}>{t(label)}<input className={input} name={key} type="number" min="0" step="any" required={requiredFields.includes(key)} defaultValue={key==="wholesale_price"&&product?.[key]==null?"":defaultValue(key)}/></label>)}
   </div><div className="flex flex-wrap gap-4">{([ ["active","Active"],["tax_inclusive","Tax inclusive"],["purchase_tax_recoverable","Recoverable purchase tax"] ] as const).map(([key,label])=><label className="flex items-center gap-2 text-sm" key={key}><input name={key} type="checkbox" defaultChecked={isNew?key==="active":Boolean(product?.[key])}/>{t(label)}</label>)}</div></section>
   <section className="space-y-3"><h3 className="border-b pb-2 font-semibold">Bulk and selling units (optional)</h3><p className="text-sm text-slate-600">Example: stock by bottle, and sell a <strong>crate containing 24 bottles</strong>; or stock by kg and sell a <strong>bag containing 25 kg</strong>. A half-crate uses 0.5 of the crate price and deducts 12 bottles.</p>
    {!isNew&&<label className="grid max-w-md gap-1 text-sm">Edit existing selling unit<select className={input} value={selectedUnit} onChange={e=>setSelectedUnit(e.target.value)}><option value="">+ New unit / no change</option>{units.filter(u=>u.unit_name!==base).map(u=><option key={u.unit_name} value={u.unit_name}>{u.unit_name} ({u.units_per_package} {base})</option>)}</select></label>}
    <div className="grid gap-3 md:grid-cols-4" key={selectedUnit}>
     <label className="grid gap-1 text-sm">Selling unit name<input className={input} name="package_name" maxLength={32} defaultValue={selected?.unit_name??""} placeholder="crate / bag"/></label>
     <label className="grid gap-1 text-sm">Number of {base} per unit<input className={input} name="package_factor" type="number" min="0.000001" step="any" defaultValue={selected?.units_per_package??""} placeholder="24 or 25"/></label>
     <label className="grid gap-1 text-sm">Retail price per crate/bag<input className={input} name="package_retail_price" type="number" min="0" step="any" defaultValue={selected?.retail_package_price??""} placeholder="Optional"/></label>
     <label className="grid gap-1 text-sm">Wholesale price per crate/bag<input className={input} name="package_wholesale_price" type="number" min="0" step="any" defaultValue={selected?.wholesale_package_price??""} placeholder="Optional"/></label>
    </div></section>
   <section className="space-y-3"><h3 className="border-b pb-2 font-semibold">Branch price override (optional)</h3><div className="grid gap-3 md:grid-cols-2"><label className="grid gap-1 text-sm">Branch selling price per {base}<input className={input} name="branch_price" type="number" min="0" step="any" disabled={!branchId} placeholder="Keep regular price"/></label><label className="grid gap-1 text-sm">Effective date<input className={input} name="effective" type="datetime-local" disabled={!branchId}/></label></div></section>
   <button className="rounded-lg bg-emerald-700 px-8 py-3 font-semibold text-white disabled:opacity-50" type="submit">{busy?"Saving…":isNew?"Save product":"Save changes"}</button>
  </fieldset></form>
 </section>;
}
