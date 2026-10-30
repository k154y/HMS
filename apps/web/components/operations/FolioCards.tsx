"use client";
import Link from "next/link";
import {ArrowUpRight,ReceiptText} from "lucide-react";
import {useLocale} from "@/components/LocaleProvider";
import {FolioListItem,folioMoney} from "./folio-presentation";
export function FolioCards({rows,currentId}:{rows:FolioListItem[];currentId?:string}){
 const {t}=useLocale();
 return <div className="grid gap-3 sm:grid-cols-2">{rows.map(f=><Link key={f.id} href={`/folios/${f.id}`} aria-current={f.id===currentId?"page":undefined} className={`group rounded-xl border p-4 transition focus-visible:outline-2 focus-visible:outline-blue-600 ${f.id===currentId?"border-blue-300 bg-blue-50":"border-slate-200 bg-white hover:border-blue-300 hover:bg-slate-50"}`}>
  <div className="flex items-center justify-between gap-3"><span className="flex items-center gap-2 text-sm font-semibold text-slate-800"><ReceiptText size={16} aria-hidden="true"/>{t("Folio")} {f.id.slice(0,8)}</span><ArrowUpRight size={16} className="text-slate-400" aria-hidden="true"/></div>
  <div className="mt-3 flex items-center justify-between gap-3"><span className="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600">{t(f.status)}</span><span className={`font-semibold tabular-nums ${Number(f.balance)>0?"text-amber-800":Number(f.balance)<0?"text-blue-700":"text-emerald-700"}`}>{folioMoney(f.balance)} {f.currency}</span></div>
  {f.created_at&&<p className="mt-3 text-xs text-slate-500">{new Date(f.created_at).toLocaleDateString()}</p>}
 </Link>)}</div>;
}
