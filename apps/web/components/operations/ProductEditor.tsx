"use client";
import {useState} from "react";
import {useSession} from "next-auth/react";
import {Row} from "./MasterData";
import {productFields} from "./fields";
import {Field,inputStyle,buttonStyle} from "./ui";
import {api} from "@/lib/hms-api";
import {useLocale} from "@/components/LocaleProvider";
export function ProductEditor({product,onSaved}:{product:Row;onSaved?:()=>Promise<void>}){
 const {t}=useLocale();const {data:session}=useSession();const permissions=(session?.user as {permissions?:string[]})?.permissions??[];
 const [editing,setEditing]=useState(false);const [values,setValues]=useState<Row>({...product});const [busy,setBusy]=useState(false);const [message,setMessage]=useState("");
 if(!permissions.includes("PRODUCT_MANAGE"))return null;
 async function save(e:React.FormEvent){e.preventDefault();setBusy(true);setMessage("");try{await api(`products/${product.id}`,"PUT",values);await onSaved?.();setEditing(false);setMessage("Saved")}catch(e){setMessage((e as Error).message)}finally{setBusy(false)}}
 return <section className="rounded-2xl border border-slate-200 bg-white p-6 space-y-4"><div className="flex items-center justify-between"><h2 className="font-semibold">{t("Product details")}</h2><button className={buttonStyle} onClick={()=>setEditing(!editing)}>{t(editing?"Cancel":"Edit")}</button></div>{message&&<p role="status">{t(message)}</p>}{editing&&<form onSubmit={save} className="space-y-4"><p className="text-sm text-slate-500">{t("Stock corrections are recorded as movements to preserve history.")}</p><div className="grid gap-4 md:grid-cols-3">{productFields.map(f=><Field key={f.key} label={f.label}>{f.options?<select className={inputStyle} value={String(values[f.key])} onChange={e=>setValues({...values,[f.key]:e.target.value})}>{f.options.map(o=><option key={o} value={o}>{t(o)}</option>)}</select>:f.type==="checkbox"?<input type="checkbox" checked={Boolean(values[f.key])} onChange={e=>setValues({...values,[f.key]:e.target.checked})}/>:<input className={inputStyle} required type={f.type??"text"} min={f.type==="number"?f.min??0:undefined} step={f.type==="number"?"0.0001":undefined} value={String(values[f.key]??"")} onChange={e=>setValues({...values,[f.key]:f.type==="number"?Number(e.target.value):e.target.value})}/>}</Field>)}</div><button className={buttonStyle} disabled={busy}>{t(busy?"Saving":"Save")}</button></form>}</section>
}

