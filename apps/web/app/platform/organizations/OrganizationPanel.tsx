"use client";
import {FormEvent,useEffect,useState} from "react";
import Link from "next/link";
import {signOut} from "next-auth/react";
import {useLocale} from "@/components/LocaleProvider";
type Product="HOTEL"|"COMMERCE";
type Organization={id:string;product:Product;name:string;status:string};
export default function OrganizationPanel(){
 const {t,locale,setLocale}=useLocale();
 const [product,setProduct]=useState<Product>("HOTEL");
 const [items,setItems]=useState<Organization[]>([]);
 const [error,setError]=useState("");
 const [success,setSuccess]=useState("");
 const [busy,setBusy]=useState(false);
 const [moduleOrg,setModuleOrg]=useState("");const [modules,setModules]=useState<string[]>([]);
 async function selectModules(id:string){setModuleOrg(id);setBusy(true);setError("");try{const r=await fetch(`/api/platform/organizations/modules?business=${id}`,{cache:"no-store"});if(!r.ok)throw new Error("Unable to load organizations.");const data=await r.json() as {module:string;enabled:boolean}[];setModules(data.filter(x=>x.enabled).map(x=>x.module));}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 async function saveModules(){setBusy(true);setError("");try{const r=await fetch("/api/platform/organizations/modules",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({business:moduleOrg,modules})});if(!r.ok)throw new Error("Unable to save module access.");setModuleOrg("");setSuccess("Module access updated. Tenant permissions remain unchanged.");}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 async function load(){
  const res=await fetch("/api/platform/organizations",{cache:"no-store"});
  const data=await res.json().catch(()=>null);
  if(!res.ok)throw new Error(data?.message??"Unable to load organizations.");
  setItems(Array.isArray(data)?data:[]);
 }
 useEffect(()=>{load().catch(e=>setError(e instanceof Error?e.message:"Unable to load organizations."))},[]);
 async function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();const form=e.currentTarget;const data=new FormData(form);
  const v=(name:string)=>String(data.get(name)??"").trim();
  const temporaryPassword=String(data.get("temporaryPassword")??"");
  setBusy(true);setError("");setSuccess("");
  try{
   const resp=await fetch("/api/platform/organizations",{
    method:"POST",headers:{"Content-Type":"application/json"},
    body:JSON.stringify({product,organizationName:v("organizationName"),fullName:v("fullName"),
     email:v("email"),temporaryPassword,hotelCode:product==="HOTEL"?v("hotelCode"):null,
     preferredLanguage:v("preferredLanguage"),currency:v("currency")})
   });
   const result=await resp.json().catch(()=>({}));
   if(!resp.ok)throw new Error(result.message??`Creation failed (${resp.status})`);
   setSuccess("Organization and owner created. Give the temporary password securely to the owner. They must change it at first login.");
   form.reset();await load();
  }catch(e){setError(e instanceof Error?e.message:"Unable to create organization.")}
  finally{setBusy(false);}
 }
 const field="mt-1 block w-full rounded-lg border border-slate-300 bg-white p-3";
 return <main className="min-h-screen bg-slate-50 p-5 text-slate-900 md:p-10"><div className="mx-auto max-w-5xl space-y-7">
  <header className="flex flex-wrap items-center justify-between gap-3">
   <div><p className="font-semibold text-blue-700">{t("Kivu Business Platform · KBP Administration")}</p>
    <h1 className="text-3xl font-bold">{t("Organizations and owners")}</h1>
    <p className="mt-1 text-sm text-slate-600">{t("Platform administration only. No access to operational customer records.")}</p></div>
   <div className="flex flex-wrap items-center gap-3">
   <button type="button" onClick={()=>signOut({callbackUrl:"/admin/login"})} className="rounded border px-3 py-2">{t("Sign out")}</button>
   <select aria-label={t("Language")} className="rounded border p-2" value={locale}
    onChange={e=>setLocale(e.target.value as "en"|"fr"|"rw")}>
    <option value="en">English</option><option value="fr">Français</option><option value="rw">Kinyarwanda</option>
   </select></div>
  </header>
  <section className="rounded-xl border bg-white p-6 shadow-sm">
   <h2 className="text-xl font-semibold">{t("Create organization and owner")}</h2>
   <p className="mt-1 text-sm text-slate-600">{t("Choose one product. Owners receive access only to their assigned organization.")}</p>
   <form onSubmit={submit} className="mt-5 grid gap-4 md:grid-cols-2">
    <label className="text-sm font-medium">{t("Product")}<select className={field} value={product} onChange={e=>setProduct(e.target.value as Product)}>
     <option value="HOTEL">{"KBP Hotel (HMS)"}</option><option value="COMMERCE">{"KBP Commerce (ISDMS)"}</option>
    </select></label>
    <label className="text-sm font-medium">{t("Organization name")}<input className={field} name="organizationName" required maxLength={200}/></label>
    {product==="HOTEL"&&<label className="text-sm font-medium">{t("Hotel code")}<input className={field} name="hotelCode" required maxLength={50}/></label>}
    <label className="text-sm font-medium">{t("Owner's full name")}<input className={field} name="fullName" required maxLength={200}/></label>
    <label className="text-sm font-medium">{t("Owner email")}<input className={field} name="email" type="email" required maxLength={255} autoComplete="off"/></label>
    <label className="text-sm font-medium">{t("Temporary password (15–128 characters)")}<input className={field} name="temporaryPassword"
       type="password" minLength={15} maxLength={128} required autoComplete="new-password"/></label>
    <label className="text-sm font-medium">{t("Base currency")}<select className={field} name="currency" defaultValue="RWF">
     <option value="RWF">{"RWF"}</option><option value="USD">{"USD"}</option><option value="EUR">{"EUR"}</option>
    </select></label>
    <label className="text-sm font-medium">{t("Preferred language")}<select className={field} name="preferredLanguage" defaultValue="en">
     <option value="en">English</option><option value="fr">Français</option><option value="rw">Kinyarwanda</option>
    </select></label>
    <div className="md:col-span-2"><p className="mb-3 text-sm text-amber-900">{t("Provide the temporary password privately. Never send passwords in an unencrypted group chat.")}</p>
     {error&&<p role="alert" className="mb-3 rounded bg-red-50 p-3 text-red-700">{error}</p>}
     {success&&<p role="status" className="mb-3 rounded bg-green-50 p-3 text-green-800">{t(success)}</p>}
     <button disabled={busy} className="rounded-lg bg-blue-700 px-5 py-3 font-semibold text-white disabled:opacity-50">
      {t(busy?"Creating…":"Create owner and organization")}
     </button></div>
   </form>
  </section>
  <section className="rounded-xl border bg-white p-6"><h2 className="mb-3 text-xl font-semibold">{t("Registered organizations")}</h2>
   {moduleOrg&&<div className="mb-4 rounded border bg-slate-50 p-4"><h3 className="font-semibold">{t("Commerce modules")}</h3><p>{t("Core and inventory remain enabled. Retail requires accounting.")}</p>{["CORE","INVENTORY","ACCOUNTING","RETAIL"].map(module=><label key={module} className="mr-4 inline-flex gap-2 py-3"><input type="checkbox" disabled={busy||module==="CORE"||module==="INVENTORY"} checked={modules.includes(module)} onChange={e=>setModules(s=>e.target.checked?Array.from(new Set([...s,module,...(module==="RETAIL"?["ACCOUNTING"]:[])])):s.filter(m=>m!==module&&(module!=="ACCOUNTING"||m!=="RETAIL")))}/>{module}</label>)}<button disabled={busy} onClick={saveModules} className="rounded bg-blue-700 px-4 py-2 text-white">{t("Save")}</button></div>}
   <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b">
    <th className="p-3">{t("Name")}</th><th className="p-3">{t("Product")}</th><th className="p-3">{t("Status")}</th></tr></thead>
    <tbody>{items.map(x=><tr key={`${x.product}-${x.id}`} className="border-b">
     <td className="p-3">{x.name}</td><td className="p-3">{x.product}</td><td className="p-3">{x.status}{x.product==="COMMERCE"&&<button disabled={busy} className="ml-3 text-blue-700 underline" onClick={()=>selectModules(x.id)}>{t("Manage modules")}</button>}</td>
    </tr>)}</tbody></table>{!items.length&&<p className="p-4 text-slate-600">{t("No organizations found.")}</p>}</div>
  </section>
 </div></main>;
}
