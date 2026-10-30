"use client";
import {useEffect,useState} from "react";
import {Building2,UserRound,Mail,Phone} from "lucide-react";
import {all} from "@/lib/hms-api";
import {useLocale} from "@/components/LocaleProvider";
import {FolioCards} from "./FolioCards";
import {FolioListItem,currencyBalances,folioMoney} from "./folio-presentation";
export function CustomerFolioHistory({record,currentId}:{record:{id:string;name?:unknown;kind?:unknown;email?:unknown;phone?:unknown};currentId?:string}){
 const {t}=useLocale(),[rows,setRows]=useState<FolioListItem[]>([]),[error,setError]=useState(""),[loading,setLoading]=useState(true);
 useEffect(()=>{let active=true;setLoading(true);setRows([]);setError("");all<FolioListItem>("folios").then(data=>{if(active)setRows(data.filter(r=>r.customer_id===record.id))}).catch(e=>{if(active)setError(e.message)}).finally(()=>{if(active)setLoading(false)});return()=>{active=false}},[record.id]);
 const Icon=record.kind==="COMPANY"?Building2:UserRound;
 return <section className="space-y-5 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
  <header className="flex items-start gap-3"><span className="rounded-xl bg-blue-50 p-3 text-blue-700"><Icon size={22} aria-hidden="true"/></span><div className="min-w-0"><p className="text-xs text-slate-500">{t("Billed to")}</p><h2 className="mt-1 break-words text-lg font-semibold text-slate-900">{String(record.name??"")}</h2><div className="mt-2 flex flex-wrap gap-3 text-xs text-slate-500">{record.email?<span className="inline-flex items-center gap-1 break-all"><Mail size={13} aria-hidden="true"/>{String(record.email)}</span>:null}{record.phone?<span className="inline-flex items-center gap-1"><Phone size={13} aria-hidden="true"/>{String(record.phone)}</span>:null}</div></div></header>
  {error&&<p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{t(error)}</p>}
  {loading?<p role="status" className="text-sm text-slate-500">{t("Loading")}…</p>:!error&&<>
   <div className="flex flex-wrap gap-3">{Object.entries(currencyBalances(rows)).map(([currency,value])=><div key={currency} className="min-w-40 rounded-xl bg-slate-50 px-4 py-3"><p className="text-xs text-slate-500">{t("Balance")} · {currency}</p><p className="mt-1 font-semibold tabular-nums">{folioMoney(value)}</p></div>)}</div>
   <div><h3 className="text-sm font-semibold">{t("Recent billing history")}</h3><p className="mt-1 text-xs leading-relaxed text-slate-500">{t("Showing recent folios available for this branch. Summaries cover these records only.")}</p></div>
   <FolioCards rows={rows} currentId={currentId}/>
   {!rows.length&&<p className="rounded-xl border border-dashed p-6 text-center text-sm text-slate-500">{t("No folio activity yet.")}</p>}
  </>}
 </section>;
}
