"use client";
import {FormEvent,useEffect,useState} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";

type Row=Record<string,unknown>;
export default function CommerceConfiguration({businessId,baseCurrency}:{businessId:string;baseCurrency:string}) {
 const {t}=useLocale();
 const [tab,setTab]=useState("currencies");
 const [rows,setRows]=useState<Row[]>([]),[branches,setBranches]=useState<Row[]>([]);
 const [message,setMessage]=useState(""),[busy,setBusy]=useState(false),[editing,setEditing]=useState<Row|null>(null);
 const [assigned,setAssigned]=useState<string[]>([]);
 async function api(path:string,data?:unknown) {
  const res=await fetch(`/api/commerce/businesses/${businessId}/${path}`,{method:data===undefined?"GET":"POST",cache:"no-store",headers:data===undefined?{}:{"Content-Type":"application/json"},body:data===undefined?undefined:JSON.stringify(data)});
  if(res.status===401) window.location.replace(loginDestination("/commerce",true));
  if(!res.ok) throw new Error(res.status===409?"The change conflicts with existing records. Review the current configuration.":res.status===400?"Please check the information you entered and try again.":"You don't have permission to perform this action.");
  return res.json();
 }
 useEffect(()=>{let cancelled=false;setRows([]);setEditing(null);setMessage("");setAssigned([]);
  Promise.all([api(tab),api("branches")]).then(([r,b])=>{if(!cancelled){setRows(r);setBranches(b)}}).catch(e=>{if(!cancelled)setMessage(e.message)});return()=>{cancelled=true};
 },[businessId,tab]);
 async function submit(e:FormEvent<HTMLFormElement>) {
  e.preventDefault();const form=e.currentTarget;const fd=new FormData(form);const value=(key:string)=>String(fd.get(key)??"");
  setBusy(true);setMessage("");
  try {
   let path=tab;let data:unknown;
   if(tab==="currencies") data={currency:value("currency").trim().toUpperCase(),active:fd.get("active")==="on"};
   else if(tab==="exchange-rates") data={currency:value("currency").trim().toUpperCase(),rate:value("rate"),effectiveAt:new Date(value("effectiveAt")).toISOString(),source:value("source")};
   else if(tab==="employees") {
    data=editing?{role:value("role"),active:fd.get("active")==="on",branches:assigned}:{email:value("email"),name:value("name"),temporaryPassword:value("password"),role:value("role"),branches:assigned};
    if(editing) path+=`/${editing.user_id}/update`;
   }
   await api(path,data);form.reset();setEditing(null);setAssigned([]);setRows(await api(tab));setMessage("Configuration saved.");
  }catch(e){setMessage(e instanceof Error?e.message:"Unable to load data")}finally{setBusy(false)}
 }
 const field="mt-1 block w-full rounded border border-slate-300 bg-white p-2";
 const tabs=[{id:"currencies",label:t("Currencies")},{id:"exchange-rates",label:t("Exchange rate history")},{id:"employees",label:t("Staff")},{id:"configuration-audit",label:t("Audit history")}];
 const columns=tab==="currencies"?["currency","decimal_places","active"]:tab==="exchange-rates"?["currency","rate_to_base","effective_at","source"]:tab==="employees"?["full_name","email","role","status"]:["action","detail","created_at"];
 const labels:Record<string,string>={currency:t("Currency"),decimal_places:t("Decimal places"),active:t("Active"),rate_to_base:t("Exchange rate"),effective_at:t("Effective date"),source:t("Source"),full_name:t("Name"),email:t("Email"),role:t("Role"),status:t("Status"),action:t("Action"),detail:t("Details"),created_at:t("Date")};
 return <section className="space-y-4 rounded-xl border bg-white p-5">
  <nav className="flex flex-wrap gap-2">{tabs.map(x=><button type="button" disabled={busy} key={x.id} onClick={()=>setTab(x.id)} className={`rounded px-3 py-2 ${tab===x.id?"bg-emerald-700 text-white":"bg-slate-100"}`}>{x.label}</button>)}</nav>
  <p className="text-sm text-slate-600">{t("Base currency")}: {baseCurrency}. {t("The base accounting currency is fixed after provisioning.")}</p>
  {message&&<p role="status" className="rounded bg-amber-50 p-3">{t(message)}</p>}
  {tab!=="configuration-audit"&&<form key={`${tab}-${editing?.user_id??"new"}`} onSubmit={submit} className="grid gap-3 rounded border p-4 md:grid-cols-2">
   {(tab==="currencies"||tab==="exchange-rates")&&<label>{t("Currency")}<input className={field} name="currency" required pattern="[A-Za-z]{3}" maxLength={3} placeholder="USD"/></label>}
   {tab==="currencies"&&<label className="flex items-center gap-2"><input name="active" type="checkbox" defaultChecked/>{t("Active")}</label>}
   {tab==="exchange-rates"&&<><label>{t("Exchange rate")}<input className={field} name="rate" type="number" min="0.0000000001" step="0.0000000001" required/></label>
    <label>{t("Effective date")}<input className={field} name="effectiveAt" type="datetime-local" required/></label><label>{t("Source")}<input className={field} name="source" maxLength={200} required/></label>
    <p className="text-sm text-slate-600 md:col-span-2">{t("Enter the base-currency value of one transaction-currency unit. Existing rate records cannot be changed.")}</p></>}
   {tab==="employees"&&<>
    {!editing&&<><label>{t("Name")}<input name="name" className={field} required maxLength={200}/></label><label>{t("Email")}<input name="email" type="email" className={field} required maxLength={255}/></label>
    <label>{t("Temporary password (15–128 characters)")}<input name="password" type="password" autoComplete="new-password" className={field} required minLength={15} maxLength={128}/></label></>}
    {editing&&<p className="font-semibold">{String(editing.full_name)}</p>}
    <label>{t("Role")}<select className={field} name="role" defaultValue={String(editing?.role??"CASHIER")}>{["MANAGER","CASHIER","STOREKEEPER","ACCOUNTANT","VIEWER"].map(r=><option key={r} value={r}>{t(r)}</option>)}</select></label>
    {editing&&<label className="flex items-center gap-2"><input type="checkbox" name="active" defaultChecked={editing.status==="ACTIVE"}/>{t("Active")}</label>}
    <fieldset className="md:col-span-2"><legend>{t("Allowed branches")}</legend><div className="flex flex-wrap gap-4">{branches.filter(b=>b.active).map(b=><label key={String(b.id)} className="flex gap-2"><input type="checkbox" checked={assigned.includes(String(b.id))} onChange={e=>setAssigned(a=>e.target.checked?[...a,String(b.id)]:a.filter(id=>id!==b.id))}/>{String(b.name)}</label>)}</div></fieldset>
   </>}
   <div className="flex gap-2"><button disabled={busy||(tab==="employees"&&!assigned.length)} className="rounded bg-emerald-700 px-4 py-2 text-white disabled:opacity-50">{t("Save")}</button>{editing&&<button type="button" onClick={()=>{setEditing(null);setAssigned([])}}>{t("Cancel")}</button>}</div>
  </form>}
  <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{columns.map(k=><th key={k} className="p-3">{labels[k]}</th>)}{tab==="employees"&&<th>{t("Actions")}</th>}</tr></thead><tbody>{rows.map((row,i)=><tr key={String(row.id??row.user_id??row.currency??i)} className="border-t">{columns.map(k=><td key={k} className="max-w-xl break-words p-3">{typeof row[k]==="boolean"?t(row[k]?"Active":"Inactive"):String(row[k]??"")}</td>)}{tab==="employees"&&<td>{row.role!=="OWNER"&&<button className="text-emerald-800 underline" onClick={()=>{setEditing(row);setAssigned(JSON.parse(String(row.branch_ids??"[]")))}}>{t("Edit")}</button>}</td>}</tr>)}</tbody></table>{!rows.length&&<p className="p-4">{t("No records yet")}</p>}</div>
 </section>;
}
