"use client";
import {useState} from "react";
import {Building2,UserRound,Mail,Phone,Search,ReceiptText,WalletCards,Banknote} from "lucide-react";
import {useLocale} from "@/components/LocaleProvider";
import {inputStyle} from "./ui";
import {CustomerFolioHistory} from "./CustomerFolioHistory";
import {FolioActivity,folioMoney,folioInitials,filterActivity} from "./folio-presentation";
type Props={folio:{id:string;customerId:string;currency:string;status:string;balance:number};customer:{id:string;name:string;email?:string|null;phone?:string|null;kind?:string|null}|null;entries:FolioActivity[];totals:{charges:number;payments:number}};
export default function FolioWorkspace({folio,customer,entries,totals}:Props){
 const {t}=useLocale(),[view,setView]=useState("transactions"),[search,setSearch]=useState(""),[kind,setKind]=useState("ALL");
 const balance=Number(folio.balance),Icon=customer?.kind==="COMPANY"?Building2:UserRound;
 const filtered=filterActivity(entries,search,kind);
 const badge=(kind:string)=>kind==="PAYMENT"?"bg-emerald-50 text-emerald-700":kind==="REFUND"||kind==="REVERSAL"?"bg-amber-50 text-amber-800":"bg-blue-50 text-blue-700";
 return <div className="min-w-0 space-y-5">
  <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
   <div className="h-1 bg-blue-600"/><div className="p-5 sm:p-6">
    <div className="flex flex-wrap items-start justify-between gap-5"><div className="flex min-w-0 items-start gap-4"><span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-blue-50 text-lg font-semibold text-blue-700">{folioInitials(customer?.name??"")}</span><div className="min-w-0"><p className="flex items-center gap-1.5 text-xs font-medium text-slate-500"><Icon size={13} aria-hidden="true"/>{t("Billed to")}{customer?.kind&&<span>· {t(customer.kind)}</span>}</p><h2 className="mt-1 break-words text-xl font-semibold text-slate-950">{customer?.name??t("Customer")}</h2><div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs text-slate-500">{customer?.email&&<span className="flex items-center gap-1 break-all"><Mail size={13} aria-hidden="true"/>{customer.email}</span>}{customer?.phone&&<span className="flex items-center gap-1"><Phone size={13} aria-hidden="true"/>{customer.phone}</span>}</div></div></div>
     <span className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">{t(folio.status)}{totals.payments>0&&balance>0&&<span> · {t("PARTIAL")}</span>}</span>
    </div>
    <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4"><span className="break-all text-xs text-slate-400">{t("Folio reference")}: {folio.id}</span><span className="text-xs font-medium text-slate-500">{t("Currency")}: {folio.currency}</span></div>
    {balance>0&&<p className="mt-4 rounded-xl border border-amber-100 bg-amber-50 p-3 text-xs leading-relaxed text-amber-900">{t("The folio has an unpaid balance. Settle or move the approved balance to credit before closing it.")}</p>}
   </div>
  </section>
  <section className="grid gap-3 sm:grid-cols-3">{[
   {label:"Total charges",value:totals.charges,icon:ReceiptText},
   {label:"Paid amount",value:totals.payments,icon:Banknote},
   {label:balance<0?"Credit balance":"Balance due",value:Math.abs(balance),icon:WalletCards}
  ].map(({label,value,icon:MetricIcon})=><article key={label} className={`rounded-2xl border p-4 ${label==="Balance due"&&balance>0?"border-amber-100 bg-amber-50/70":"border-slate-200 bg-white"}`}><p className="flex items-center gap-2 text-xs text-slate-500"><MetricIcon size={15} aria-hidden="true"/>{t(label)}</p><p className="mt-3 break-words text-xl font-semibold tabular-nums text-slate-900">{folioMoney(value)} <span className="text-xs font-medium text-slate-500">{folio.currency}</span></p></article>)}</section>
  <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
   <div className="flex flex-wrap gap-1 border-b border-slate-100 p-2" aria-label={t("Folio views")}>{[["transactions","Transactions"],["history","Related folios"]].map(([key,label])=><button type="button" key={key} aria-pressed={view===key} onClick={()=>setView(key)} className={`rounded-xl px-4 py-3 text-sm font-medium focus-visible:outline-2 focus-visible:outline-blue-600 ${view===key?"bg-blue-50 text-blue-700":"text-slate-500 hover:bg-slate-50"}`}>{t(label)}</button>)}</div>
   {view==="history"?<div className="p-4"><CustomerFolioHistory record={customer??{id:folio.customerId}} currentId={folio.id}/></div>:<>
    <div className="space-y-4 p-5"><div><h3 className="font-semibold text-slate-900">{t("Folio ledger")}</h3><p className="mt-1 text-xs text-slate-500">{t("All charges, payments, refunds and reversals.")}</p></div>
     <div className="flex flex-wrap gap-3"><label className="relative min-w-0 flex-1"><span className="sr-only">{t("Search transactions")}</span><Search size={16} className="absolute left-3 top-3 text-slate-400" aria-hidden="true"/><input className={inputStyle+" pl-10"} placeholder={t("Search transactions")} value={search} onChange={e=>setSearch(e.target.value)}/></label><label><span className="sr-only">{t("Transaction type")}</span><select className={inputStyle} value={kind} onChange={e=>setKind(e.target.value)}><option value="ALL">{t("All types")}</option>{Array.from(new Set(entries.map(e=>e.kind))).map(k=><option key={k} value={k}>{t(k)}</option>)}</select></label></div>
    </div>
    <div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-sm"><thead className="border-y border-slate-100 bg-slate-50 text-xs text-slate-500"><tr>{["Date & time","Description","Type","Amount"].map(label=><th key={label} scope="col" className={`px-5 py-3 font-medium ${label==="Amount"?"text-right":""}`}>{t(label)}</th>)}</tr></thead>
     <tbody className="divide-y divide-slate-100">{filtered.map(entry=><tr key={entry.id} className="align-top hover:bg-slate-50/70">
      <td className="whitespace-nowrap px-5 py-4 text-xs text-slate-500">{entry.createdAt?new Date(entry.createdAt).toLocaleString():"—"}</td>
      <td className="max-w-sm px-5 py-4"><p className="break-words font-medium text-slate-800">{entry.memo}</p><details className="mt-2 text-xs text-slate-500"><summary className="cursor-pointer text-blue-700">{t("Transaction details")}</summary><dl className="mt-2 space-y-2 rounded-lg bg-slate-50 p-3">{[["Entry reference",entry.id],["Source reference",entry.sourceId],["Staff reference",entry.actorId]].filter(([,value])=>value).map(([label,value])=><div key={label}><dt>{t(label!)}</dt><dd className="mt-1 break-all font-mono">{value}</dd></div>)}</dl></details></td>
      <td className="px-5 py-4"><span className={`inline-block whitespace-nowrap rounded-md px-2 py-1 text-[11px] font-medium ${badge(entry.kind)}`}>{t(entry.kind)}</span></td>
      <td className={`whitespace-nowrap px-5 py-4 text-right font-semibold tabular-nums ${Number(entry.amount)<0?"text-emerald-700":"text-slate-900"}`}>{folioMoney(entry.amount)} <span className="text-xs font-normal">{folio.currency}</span></td>
     </tr>)}</tbody></table></div>
     {!filtered.length&&<p className="p-10 text-center text-sm text-slate-500">{t(entries.length?"No matching transactions.":"No folio activity yet.")}</p>}
   </>}
  </section>
 </div>;
}
