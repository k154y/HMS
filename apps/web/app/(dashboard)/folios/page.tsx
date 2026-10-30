"use client";
import {useEffect,useState,useCallback} from "react";
import {Search,RefreshCw,Users,ReceiptText,ChevronDown,WalletCards} from "lucide-react";
import {all} from "@/lib/hms-api";
import {Panel,inputStyle} from "@/components/operations/ui";
import {FolioCards} from "@/components/operations/FolioCards";
import {FolioListItem,folioMoney,folioInitials,groupFolios,currencyBalances} from "@/components/operations/folio-presentation";
import {useLocale} from "@/components/LocaleProvider";
export default function Folios(){
 const {t}=useLocale();
 const [rows,setRows]=useState<FolioListItem[]>([]),[error,setError]=useState(""),[search,setSearch]=useState(""),[expanded,setExpanded]=useState<string|null>(null),[loading,setLoading]=useState(true),[filter,setFilter]=useState("ALL");
 const load=useCallback(async()=>{setLoading(true);setError("");try{setRows(await all<FolioListItem>("folios"))}catch(e){setError(e instanceof Error?e.message:"Unable to load folio.")}finally{setLoading(false)}},[]);
 useEffect(()=>{void load()},[load]);
 const accounts=groupFolios(rows);
 const visible=accounts.filter(a=>(a.name+" "+a.bills.map(b=>b.id).join(" ")).toLowerCase().includes(search.toLowerCase())&&(filter==="ALL"||a.bills.some(b=>filter==="DUE"?Number(b.balance)>0:b.status==="OPEN")));
 const balances=currencyBalances(rows.filter(r=>Number(r.balance)>0));
 return <Panel title="Guest Folios" error={error}>
  <div className="flex flex-wrap items-start justify-between gap-4"><p className="max-w-2xl text-sm leading-relaxed text-slate-500">{t("Review recent guest and company accounts, then open a folio to manage billing.")}</p><button type="button" disabled={loading} onClick={load} className="inline-flex items-center gap-2 rounded-xl border bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"><RefreshCw size={16} aria-hidden="true" className={loading?"animate-spin":""}/>{t("Refresh")}</button></div>
  <div className="grid gap-4 sm:grid-cols-3">
   <article className="rounded-2xl border border-slate-200 bg-white p-5"><Users size={20} className="text-blue-600" aria-hidden="true"/><p className="mt-3 text-xs text-slate-500">{t("Customers")}</p><p className="mt-1 text-2xl font-semibold">{accounts.length}</p></article>
   <article className="rounded-2xl border border-slate-200 bg-white p-5"><ReceiptText size={20} className="text-blue-600" aria-hidden="true"/><p className="mt-3 text-xs text-slate-500">{t("Recent folios")}</p><p className="mt-1 text-2xl font-semibold">{rows.length}</p></article>
   <article className="rounded-2xl border border-amber-100 bg-amber-50/60 p-5"><WalletCards size={20} className="text-amber-700" aria-hidden="true"/><p className="mt-3 text-xs text-amber-800">{t("Outstanding balance")}</p><div className="mt-1 space-y-1 text-lg font-semibold tabular-nums text-amber-950">{Object.entries(balances).map(([currency,value])=><p key={currency}>{folioMoney(value)} {currency}</p>)}{!Object.keys(balances).length&&<p>—</p>}</div></article>
  </div>
  <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
   <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 p-5">
    <label className="relative min-w-0 flex-1 sm:max-w-md"><Search size={17} className="pointer-events-none absolute left-3 top-3 text-slate-400" aria-hidden="true"/><span className="sr-only">{t("Search accounts")}</span><input className={inputStyle+" pl-10"} placeholder={t("Search accounts")} value={search} onChange={e=>setSearch(e.target.value)}/></label>
    <div className="flex flex-wrap gap-1 rounded-xl bg-slate-100 p-1" aria-label={t("Filter accounts")}>{[["ALL","All accounts"],["DUE","Balance due"],["OPEN","Open folios"]].map(([key,label])=><button key={key} type="button" aria-pressed={filter===key} onClick={()=>setFilter(key)} className={`rounded-lg px-3 py-2 text-xs font-medium ${filter===key?"bg-white text-blue-700 shadow-sm":"text-slate-500 hover:text-slate-900"}`}>{t(label)}</button>)}</div>
   </div>
   <p className="px-5 py-3 text-xs text-slate-500">{t("Showing recent folios available for this branch. Summaries cover these records only.")}</p>
   {loading&&<p role="status" className="p-5 text-sm text-slate-500">{t("Loading")}…</p>}
   <div className="divide-y divide-slate-100">{visible.map(a=><section key={a.id}>
    <button type="button" className="flex w-full flex-wrap items-center gap-4 px-5 py-5 text-left hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-blue-600" aria-expanded={expanded===a.id} aria-controls={`account-${a.id}`} onClick={()=>setExpanded(expanded===a.id?null:a.id)}>
     <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-sm font-semibold text-blue-700">{folioInitials(a.name)}</span>
     <span className="min-w-0 flex-1"><span className="block break-words font-semibold text-slate-900">{a.name}</span><span className="mt-1 block text-xs text-slate-500">{t("Recent folios")}: {a.bills.length}</span></span>
     <span className="text-right"><span className="block text-xs text-slate-500">{t("Balance")}</span>{Object.entries(currencyBalances(a.bills)).map(([currency,value])=><span key={currency} className="mt-1 block font-semibold tabular-nums text-slate-800">{folioMoney(value)} {currency}</span>)}</span>
     <ChevronDown size={17} aria-hidden="true" className={`text-slate-400 transition ${expanded===a.id?"rotate-180":""}`}/>
    </button>
    {expanded===a.id&&<div id={`account-${a.id}`} className="border-t border-slate-100 bg-slate-50/70 p-5"><FolioCards rows={a.bills}/></div>}
   </section>)}</div>
   {!loading&&!visible.length&&<div className="p-12 text-center text-sm text-slate-500"><Search size={28} className="mx-auto mb-3 text-slate-300" aria-hidden="true"/>{t(search||filter!=="ALL"?"No matching accounts.":"No folio activity yet.")}</div>}
  </section>
 </Panel>;
}
