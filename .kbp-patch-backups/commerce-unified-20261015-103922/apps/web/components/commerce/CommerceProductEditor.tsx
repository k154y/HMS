"use client";
import {useEffect,useState,FormEvent} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";
type Row=Record<string,unknown>;
type Unit={unit_name:string;units_per_package:string;retail_package_price?:string|null;wholesale_package_price?:string|null};
const input="w-full rounded-lg border border-slate-300 bg-white p-2";
export default function CommerceProductEditor({businessId,branchId,products,categories,onSaved}:{businessId:string;branchId:string;products:Row[];categories:{id:string;name:string}[];onSaved:()=>Promise<void>}){
 const {t}=useLocale();
 const [id,setId]=useState(""),[busy,setBusy]=useState(false),[error,setError]=useState(""),[units,setUnits]=useState<Unit[]>([]);
 const [packageUnit,setPackageUnit]=useState("");
 const product=products.find(p=>p.id===id);
 useEffect(()=>{
  let active=true;setUnits([]);setPackageUnit("");
  if(!id)return;
  fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"}).then(async r=>{if(!r.ok)throw new Error("Cannot load product units");return r.json() as Promise<Unit[]>}).then(x=>{if(active)setUnits(x)}).catch(e=>{if(active)setError((e as Error).message)});
  return()=>{active=false};
 },[businessId,id]);
 async function save(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(!product||busy)return;
  const d=new FormData(e.currentTarget),s=(key:string)=>String(d.get(key)??"");
  const branchPrice=s("branch_price"),effective=s("effective");
  const packName=s("package_name").trim(),factor=s("package_factor");
  const payload={
   product:{name:s("name"),description:s("description"),categoryId:s("category")||null,barcode:s("barcode")||null,
    purchaseCost:s("purchase_cost"),retailPrice:s("retail_price"),wholesalePrice:s("wholesale_price")||null,
    minimumPrice:s("minimum_price"),taxRate:s("tax_rate"),taxInclusive:d.has("tax_inclusive"),
    purchaseTaxRecoverable:d.has("purchase_tax_recoverable"),lowStockQuantity:s("low_stock_quantity"),active:d.has("active")},
   packageSetting:packName?{name:packName,factor,retailPackagePrice:s("package_retail_price")||null,wholesalePackagePrice:s("package_wholesale_price")||null}:null,
   branchPrice:branchPrice?{branchId,price:branchPrice,effectiveAt:effective?new Date(effective).toISOString():null}:null
  };
  setBusy(true);setError("");
  try{
   const r=await fetch(`/api/commerce/businesses/${businessId}/products/${id}/configuration`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
   if(r.status===401){window.location.replace(loginDestination("/commerce",true));return}
   if(!r.ok){const x=await r.json().catch(()=>({}));throw new Error(String(x.message??"Unable to save product. Verify permissions and pricing."))}
   await onSaved();
   const u=await fetch(`/api/commerce/businesses/${businessId}/products/${id}/units`,{cache:"no-store"});if(u.ok)setUnits(await u.json() as Unit[]);
  }catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 const chosen=units.find(x=>x.unit_name===packageUnit);
 return <section className="mt-5 space-y-4 rounded-xl border bg-white p-5">
  <header><h2 className="text-lg font-semibold">{t("Edit product and pricing")}</h2><p className="text-sm text-slate-600">All changes are saved together. Use the smallest physical unit for stock, such as bottle.</p></header>
  {error&&<p role="alert" className="rounded bg-amber-50 p-3 text-amber-800">{error}</p>}
  <select aria-label={t("Product")} className={input} value={id} disabled={busy} onChange={e=>{setId(e.target.value);setError("")}}><option value="">{t("Select")}</option>{products.map(p=><option key={String(p.id)} value={String(p.id)}>{String(p.name)}</option>)}</select>
  {product&&<form key={id} onSubmit={save} className="space-y-6"><fieldset disabled={busy} className="space-y-6">
   <div className="grid gap-3 md:grid-cols-3">
    {([ ["name","Name"],["description","Description"],["barcode","Barcode (optional)"],["purchase_cost","Purchase cost per base unit"],["retail_price","Retail price per base unit"],["wholesale_price","Wholesale price per base unit"],["minimum_price","Minimum price per base unit"],["tax_rate","Tax rate (%)"],["low_stock_quantity","Low stock threshold"] ] as const).map(([key,label])=><label className="grid gap-1 text-sm" key={key}>{label}<input className={input} name={key} defaultValue={String(product[key]??"")} required={!(["description","barcode","wholesale_price"] as string[]).includes(key)} type={(["name","description","barcode"] as string[]).includes(key)?"text":"number"} min="0" step="any" maxLength={key==="description"?2000:200}/></label>)}
    <label className="grid gap-1 text-sm">{t("Category")}<select name="category" className={input} defaultValue={String(product.category_id??"")}><option value="">—</option>{categories.map(c=><option value={c.id} key={c.id}>{c.name}</option>)}</select></label>
   </div>
   <div className="flex flex-wrap gap-4">{([ ["active","Active"],["tax_inclusive","Tax inclusive"],["purchase_tax_recoverable","Recoverable purchase tax"] ] as const).map(([key,label])=><label key={key} className="flex items-center gap-2"><input type="checkbox" name={key} defaultChecked={Boolean(product[key])}/>{t(label)}</label>)}</div>
   <section className="space-y-3 border-t pt-4"><h3 className="font-semibold">Packaging and selling units</h3>
    <p className="text-sm text-slate-600">Base stock unit: <strong>{String(product.base_unit)}</strong>. Example: crate = 24 bottles; sell 0.5 crate for half the configured crate price.</p>
    <label className="grid max-w-md gap-1 text-sm">Existing package (or new package)<select className={input} value={packageUnit} onChange={e=>setPackageUnit(e.target.value)}><option value="">New package / no package change</option>{units.filter(u=>u.unit_name!==product.base_unit).map(u=><option key={u.unit_name} value={u.unit_name}>{u.unit_name} ({u.units_per_package} {String(product.base_unit)})</option>)}</select></label>
    <div className="grid gap-3 md:grid-cols-4" key={packageUnit}>
     <label className="grid gap-1 text-sm">Package name<input name="package_name" maxLength={32} className={input} defaultValue={chosen?.unit_name??""} placeholder="crate"/></label>
     <label className="grid gap-1 text-sm">Base units per package<input name="package_factor" type="number" step="0.000001" min="0.000001" className={input} defaultValue={chosen?.units_per_package??""} placeholder="24"/></label>
     <label className="grid gap-1 text-sm">Retail price per package<input name="package_retail_price" type="number" min="0" step="0.0001" className={input} defaultValue={chosen?.retail_package_price??""} placeholder="Optional"/></label>
     <label className="grid gap-1 text-sm">Wholesale price per package<input name="package_wholesale_price" type="number" min="0" step="0.0001" className={input} defaultValue={chosen?.wholesale_package_price??""} placeholder="e.g. 20000"/></label>
    </div>
   </section>
   <section className="space-y-3 border-t pt-4"><h3 className="font-semibold">Optional branch price</h3><p className="text-sm text-slate-600">Overrides the base-unit price at the selected branch; leave empty to keep current branch pricing.</p>
    <div className="grid gap-3 md:grid-cols-2"><label className="grid gap-1 text-sm">Branch selling price<input name="branch_price" type="number" min="0" step="0.0001" className={input} disabled={!branchId}/></label><label className="grid gap-1 text-sm">Effective date<input name="effective" type="datetime-local" className={input} disabled={!branchId}/></label></div>
   </section>
   <button type="submit" className="rounded-lg bg-emerald-700 px-6 py-3 font-semibold text-white disabled:opacity-50">{busy?"Saving…":"Save product"}</button>
  </fieldset></form>}
 </section>
}
