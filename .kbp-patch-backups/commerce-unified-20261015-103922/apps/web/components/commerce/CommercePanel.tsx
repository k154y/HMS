"use client";

import { useEffect, useState } from "react";
import WorkspaceNavigation from "@/components/WorkspaceNavigation";
import CommerceConfiguration from "./CommerceConfiguration";
import CommerceStockOperations from "./CommerceStockOperations";
import CommerceTrading from "./CommerceTrading";
import CommerceProductEditor from "./CommerceProductEditor";
import {loginDestination} from "@/lib/navigation";
import { useLocale } from "@/components/LocaleProvider";
import { Building2, Boxes, Layers3, LayoutDashboard, MapPin, Package, Plus, RefreshCcw, Store, Warehouse } from "lucide-react";

type Business = { id: string; name: string; base_currency?: string; baseCurrency?: string; status: string; role?: string };
type Branch = { id: string; name: string; active: boolean };
type Location = { id: string; name: string; location_type?: string; type?: string; active: boolean };
type Category = { id: string; name: string; parent_id?: string | null };
type Product = { id: string; name: string; sku: string; barcode?: string; base_unit?: string; retail_price?: number; wholesale_price?: number };
type Stock = { product_id?: string; quantity?: number; on_hand?: number; [key: string]: unknown };
type Pane = "overview" | "branches" | "locations" | "categories" | "products" | "stock" | "configuration" | "trading";

async function request<T>(path: string, data?: unknown): Promise<T> {
  const response = await fetch(`/api/commerce/${path}`, {
    method: data === undefined ? "GET" : "POST", cache: "no-store",
    headers: data === undefined ? {} : { "Content-Type": "application/json" },
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  if (!response.ok) {
    if(response.status===401)window.location.replace(loginDestination("/commerce",true));
    const message=response.status===403||response.status===404?"You do not have access to this workspace. Contact your administrator.":"This workspace is temporarily unavailable. Please try again shortly.";
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

export default function CommercePanel({initialBusinesses}:{initialBusinesses:Business[]}) {
  const { t:translate } = useLocale();
  const t = {
    heading:translate("Commerce workspace"),
    business:translate("Business"),
    branch:translate("Branch"),
    location:translate("Stock location"),
    refresh:translate("Refresh"),
    overview:translate("Overview"),
    branches:translate("Branches"),
    locations:translate("Stock locations"),
    categories:translate("Categories"),
    products:translate("Products"),
    stock:translate("Stock"),
    empty:translate("No records yet"),
    disabled:translate("Commerce is unavailable or you don't have access. Ask your administrator to enable it."),
    add:translate("Create"),
    name:translate("Name"),
    currency:translate("Currency"),
    sku:translate("SKU"),
    baseUnit:translate("Base unit"),
    cost:translate("Purchase cost"),
    retail:translate("Retail price"),
    wholesale:translate("Wholesale price"),
    barcode:translate("Barcode (optional)"),
    type:translate("Location type"),
    save:translate("Save"),
    cancel:translate("Cancel"),
    loading:translate("Loading..."),
    noBranch:translate("Create a branch first"),
    noLocation:translate("Create a stock location first"),
    select:translate("Select"),
    error:translate("Unable to load data"),
    futureModules:translate("Open Sales and purchasing for invoices, receiving, payments and financial reports. Your administrator must enable the required modules."),
  };
  const [pane, setPane] = useState<Pane>("overview");
  const [businesses, setBusinesses] = useState<Business[]>(initialBusinesses);
  const [businessId, setBusinessId] = useState("");
  const [branches, setBranches] = useState<Branch[]>([]);
  const [branchId, setBranchId] = useState("");
  const [locations, setLocations] = useState<Location[]>([]);
  const [locationId, setLocationId] = useState("");
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [stock, setStock] = useState<Stock[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState<Pane | null>(null);
  const [fields, setFields] = useState<Record<string,string>>({});
  const [search,setSearch]=useState("");
  const update = (key: string, value: string) => setFields(s => ({...s,[key]:value}));


  useEffect(() => { setBranchId("");setLocationId("");setBranches([]);setLocations([]);setCategories([]);setProducts([]); if(!businessId)return;let cancelled=false;
    Promise.all([request<Branch[]>(`businesses/${businessId}/branches`),request<Category[]>(`businesses/${businessId}/categories`),request<Product[]>(`businesses/${businessId}/products`)]).then(([b,c,p])=>{if(!cancelled){setBranches(b);setCategories(c);setProducts(p);}}).catch(e=>{if(!cancelled)setMessage(e instanceof Error?translate(e.message):t.error)});return()=>{cancelled=true}; },[businessId]);
  useEffect(()=>{setLocationId("");setLocations([]);if(!businessId||!branchId)return;let cancelled=false;request<Location[]>(`businesses/${businessId}/branches/${branchId}/locations`).then(rows=>{if(!cancelled)setLocations(rows)}).catch(e=>{if(!cancelled)setMessage(e instanceof Error?translate(e.message):t.error)});return()=>{cancelled=true};},[businessId,branchId]);
  useEffect(()=>{setStock([]);if(!businessId||!locationId)return;let cancelled=false;request<Stock[]>(`businesses/${businessId}/locations/${locationId}/stock`).then(rows=>{if(!cancelled)setStock(rows)}).catch(e=>{if(!cancelled)setMessage(e instanceof Error?translate(e.message):t.error)});return()=>{cancelled=true};},[businessId,locationId]);

  const current = businesses.find(b=>b.id===businessId);
  const owner=current?.role==="OWNER";
  const catalogWrite=owner||current?.role==="MANAGER";
  async function reload() {if(!businessId)return;setBusy(true);try{
    const [b,c,p]=await Promise.all([request<Branch[]>(`businesses/${businessId}/branches`),request<Category[]>(`businesses/${businessId}/categories`),request<Product[]>(`businesses/${businessId}/products`)]);
    setBranches(b);setCategories(c);setProducts(p);
    if(branchId)setLocations(await request<Location[]>(`businesses/${businessId}/branches/${branchId}/locations`));
    if(locationId)setStock(await request<Stock[]>(`businesses/${businessId}/locations/${locationId}/stock`));setMessage("");
  }catch(e){setMessage(e instanceof Error?translate(e.message):t.error)}finally{setBusy(false)}}
  async function create(event: React.FormEvent<HTMLFormElement>) {event.preventDefault();if(!form||!businessId)return;setBusy(true);setMessage("");try{
    let path="";let body: Record<string,unknown>={};
    if(form==="branches"){path=`businesses/${businessId}/branches`;body={name:fields.name};}
    else if(form==="locations"){if(!branchId)throw new Error(t.noBranch);path=`businesses/${businessId}/branches/${branchId}/locations`;body={name:fields.name,type:fields.type||"WAREHOUSE"};}
    else if(form==="categories"){path=`businesses/${businessId}/categories`;body={name:fields.name,parentId:fields.parentId||null};}
    else if(form==="products"){path=`businesses/${businessId}/products`;body={name:fields.name,sku:fields.sku,barcode:fields.barcode||null,categoryId:fields.categoryId||null,baseUnit:fields.baseUnit,purchaseCost:fields.cost,retailPrice:fields.retail,wholesalePrice:fields.wholesale||null};}
    else return;
    if(fields.id&&["branches","locations","categories"].includes(form)) {
      path=`businesses/${businessId}/${form}/${fields.id}/update`;
      body={...body,active:fields.active!=="false",...(form==="branches"?{contact:fields.contact||null}:{})};
    }
    await request(path,body);setForm(null);setFields({});await reload();
  }catch(e){setMessage(e instanceof Error?translate(e.message):t.error)}finally{setBusy(false)}}
  const nav: {id:Pane;icon:React.ComponentType<{size?:number}>;label:string}[]=[{id:"overview",icon:LayoutDashboard,label:t.overview},{id:"branches",icon:Building2,label:t.branches},{id:"locations",icon:MapPin,label:t.locations},{id:"categories",icon:Layers3,label:t.categories},{id:"products",icon:Package,label:t.products},{id:"stock",icon:Boxes,label:t.stock}];
  const buttonClass="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-50";
  const fieldClass="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:border-emerald-600 focus:outline-none";
  if(owner)nav.push({id:"configuration",icon:Building2,label:translate("Business settings")});
  nav.push({id:"trading",icon:Store,label:translate("Sales and purchasing")});
  const editable=catalogWrite&&(pane==="categories"||(owner&&["branches","locations"].includes(pane)));
  const entries=((pane==="branches"?branches:pane==="locations"?locations:pane==="categories"?categories:pane==="products"?products:pane==="stock"?stock:[]) as Record<string,unknown>[]).filter(row=>String(row.name??row.sku??row.product_id??"").toLowerCase().includes(search.toLowerCase()));
  return <div className="min-h-screen bg-slate-50 text-slate-900 lg:flex">
    <aside className="border-b border-slate-200 bg-white p-5 lg:min-h-screen lg:w-64 lg:border-b-0 lg:border-r">
      <div className="mb-8 flex items-center gap-3"><div className="rounded-xl bg-emerald-700 p-2.5 text-white"><Store size={22}/></div><div><div className="text-lg font-bold">{ "ISDMS" }</div><div className="text-xs text-slate-500">{t.heading}</div></div></div>
      <div className="mb-6"><WorkspaceNavigation controls/></div><nav className="grid grid-cols-2 gap-1 lg:block" aria-label={translate("Commerce navigation")}>{nav.map(n=><button key={n.id} onClick={()=>{setPane(n.id);setForm(null)}} className={`mb-1 flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm ${pane===n.id?"bg-emerald-50 font-semibold text-emerald-800":"text-slate-600 hover:bg-slate-100"}`}><n.icon size={18}/>{n.label}</button>)}</nav>
    </aside>
    <main className="min-w-0 flex-1 p-5 lg:p-9"><div className="mx-auto max-w-6xl">
      <header className="mb-7 flex flex-wrap items-center justify-between gap-4"><div><h1 className="text-2xl font-semibold tracking-tight">{nav.find(n=>n.id===pane)?.label}</h1><p className="mt-1 text-sm text-slate-500">{current?.name??t.heading}</p></div><button className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm" onClick={reload} disabled={busy}><RefreshCcw size={15}/>{t.refresh}</button></header>
      {message&&<div role="alert" className="mb-5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{message}</div>}
      <section className="mb-6 grid gap-3 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-3">
        <label className="text-xs font-semibold text-slate-600">{t.business}<select aria-label={t.business} className={`${fieldClass} mt-1`} value={businessId} onChange={e=>setBusinessId(e.target.value)}><option value="">{t.select}</option>{businesses.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select></label>
        <label className="text-xs font-semibold text-slate-600">{t.branch}<select aria-label={t.branch} className={`${fieldClass} mt-1`} value={branchId} onChange={e=>setBranchId(e.target.value)} disabled={!businessId}><option value="">{t.select}</option>{branches.map(b=><option key={b.id} value={b.id}>{b.name}</option>)}</select></label>
        <label className="text-xs font-semibold text-slate-600">{t.location}<select aria-label={t.location} className={`${fieldClass} mt-1`} value={locationId} onChange={e=>setLocationId(e.target.value)} disabled={!branchId}><option value="">{t.select}</option>{locations.map(l=><option key={l.id} value={l.id}>{l.name}</option>)}</select></label>
      </section>
      {!businessId?<div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center text-slate-500">{businesses.length? t.select:t.disabled}</div>:
      <>
        {pane==="overview"&&<div className="grid gap-4 md:grid-cols-3">{[{label:t.branches,count:branches.length,icon:Building2},{label:t.categories,count:categories.length,icon:Layers3},{label:t.products,count:products.length,icon:Package}].map(x=><div className="rounded-xl border border-slate-200 bg-white p-5" key={x.label}><x.icon size={19} className="text-emerald-700"/><div className="mt-4 text-3xl font-semibold">{x.count}</div><div className="text-sm text-slate-500">{x.label}</div></div>)}<div className="md:col-span-3 rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">{t.currency}: {current?.base_currency??current?.baseCurrency??"—"}. {t.futureModules}</div></div>}
        {pane==="configuration"&&owner&&<CommerceConfiguration key={businessId} businessId={businessId} baseCurrency={current?.base_currency??current?.baseCurrency??""}/>}
        {pane==="trading"&&<CommerceTrading key={`${businessId}-${branchId}`} businessId={businessId} branchId={branchId} locationId={locationId} role={current?.role??""} baseCurrency={current?.base_currency??current?.baseCurrency??""} products={products}/>}
        {pane!=="overview"&&pane!=="configuration"&&pane!=="trading"&&<section className="overflow-hidden rounded-xl border border-slate-200 bg-white"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4"><h2 className="font-semibold">{nav.find(n=>n.id===pane)?.label}</h2><input aria-label={translate("Search")} placeholder={translate("Search")} className="rounded border p-2" value={search} onChange={e=>setSearch(e.target.value)}/>{(editable||(pane==="products"&&catalogWrite))&&<button className={buttonClass} disabled={busy||(pane==="locations"&&!branchId)} onClick={()=>{setFields({});setForm(pane)}}><span className="inline-flex items-center gap-1"><Plus size={16}/>{t.add}</span></button>}</div>
          {form===pane&&<form onSubmit={create} className="grid gap-3 border-b border-slate-100 bg-slate-50 p-4 md:grid-cols-2">
            <label className="text-sm">{t.name}<input required maxLength={200} className={fieldClass} value={fields.name??""} onChange={e=>update("name",e.target.value)}/></label>
            {pane==="locations"&&<label className="text-sm">{t.type}<select className={fieldClass} value={fields.type??"WAREHOUSE"} onChange={e=>update("type",e.target.value)}>{["WAREHOUSE","SHOP_FLOOR","STOREROOM","RETURNS","DAMAGED"].map(s=><option key={s} value={s}>{translate(s)}</option>)}</select></label>}
            {pane==="categories"&&<label className="text-sm">{translate("Parent category (optional)")}<select className={fieldClass} value={fields.parentId??""} onChange={e=>update("parentId",e.target.value)}><option value="">—</option>{categories.filter(c=>c.id!==fields.id).map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>}
            {fields.id&&<label className="flex items-center gap-2"><input type="checkbox" checked={fields.active!=="false"} onChange={e=>update("active",String(e.target.checked))}/>{translate("Active")}</label>}
            {fields.id&&pane==="branches"&&<label>{translate("Contact")}<input className={fieldClass} maxLength={300} value={fields.contact??""} onChange={e=>update("contact",e.target.value)}/></label>}
            {pane==="products"&&<>
              {([['sku',t.sku],['baseUnit',t.baseUnit],['cost',t.cost],['retail',t.retail],['wholesale',t.wholesale],['barcode',t.barcode]] as const).map(([id,label])=><label className="text-sm" key={id}>{label}<input className={fieldClass} required={id!=="barcode"&&id!=="wholesale"} type={['cost','retail','wholesale'].includes(id)?"number":"text"} min={['cost','retail','wholesale'].includes(id)?"0":undefined} step={['cost','retail','wholesale'].includes(id)?"0.0001":undefined} value={fields[id]??""} onChange={e=>update(id,e.target.value)}/></label>)}
              <label className="text-sm">{t.categories}<select className={fieldClass} value={fields.categoryId??""} onChange={e=>update("categoryId",e.target.value)}><option value="">—</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
            </>}
            <div className="flex items-end gap-2"><button type="submit" disabled={busy} className={buttonClass}>{t.save}</button><button type="button" className="rounded-lg border border-slate-300 px-4 py-2 text-sm" onClick={()=>setForm(null)}>{t.cancel}</button></div>
          </form>}
          <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr>{(pane==="products"?[t.name,t.sku,t.retail,t.wholesale]:pane==="stock"?[t.products,translate("Quantity")]:[t.name]).map(c=><th key={c} className="px-4 py-3 font-medium">{c}</th>)}</tr></thead><tbody>
            {entries.map((entry,i)=><tr key={String(entry.id??i)} className="border-t border-slate-100">{pane==="products"?<><td className="px-4 py-3">{String(entry.name??"")}</td><td className="px-4 py-3">{String(entry.sku??"")}</td><td className="px-4 py-3">{String(entry.retail_price??entry.retailPrice??"—")}</td><td className="px-4 py-3">{String(entry.wholesale_price??entry.wholesalePrice??"—")}</td></>:pane==="stock"?<><td className="px-4 py-3">{products.find(p=>p.id===entry.product_id)?.name??String(entry.product_id??"—")}</td><td className="px-4 py-3">{String(entry.quantity??entry.on_hand??"—")}</td></>:<td className="px-4 py-3">{String(entry.name??"")} {entry.active===false&&<span className="ml-2 text-slate-500">{translate("Inactive")}</span>}{editable&&<button type="button" disabled={busy} className="ml-4 text-emerald-800 underline" onClick={()=>{setFields({id:String(entry.id),name:String(entry.name??""),parentId:String(entry.parent_id??""),active:String(entry.active??true),contact:String(entry.contact??""),type:String(entry.location_type??"WAREHOUSE")});setForm(pane)}}>{translate("Edit")}</button>}</td>}</tr>)}
          </tbody></table>{(pane==="branches"?branches:pane==="locations"?locations:pane==="categories"?categories:pane==="products"?products:stock).length===0&&<p className="p-8 text-center text-sm text-slate-400">{pane==="locations"&&!branchId?t.noBranch:pane==="stock"&&!locationId?t.noLocation:t.empty}</p>}</div>
        </section>}
        {pane==="stock"&&locationId&&<CommerceStockOperations key={`${businessId}-${locationId}`} businessId={businessId} locationId={locationId} products={products} canWrite={catalogWrite||current?.role==="STOREKEEPER"} onPosted={reload}/>}
        {pane==="products"&&catalogWrite&&<CommerceProductEditor key={businessId} businessId={businessId} branchId={branchId} products={products} categories={categories} onSaved={reload}/>}
      </>}
    </div></main>
  </div>;
}

