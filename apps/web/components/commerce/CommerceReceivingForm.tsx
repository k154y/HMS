"use client";

import {useState} from "react";

type Props={name:string;unit:string;ordered:number;received:number;claimed:number;disabled:boolean;onConfirm:(body:Record<string,unknown>)=>Promise<void>};
const field="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm";
export default function CommerceReceivingForm({name,unit,ordered,received,claimed,disabled,onConfirm}:Props){
 const remaining=Math.max(0,Number((ordered-received-claimed).toFixed(6)));
 const [accepted,setAccepted]=useState("");
 const [damaged,setDamaged]=useState("0"),[missing,setMissing]=useState("0");
 const [note,setNote]=useState(""),[resolution,setResolution]=useState("PENDING"),[review,setReview]=useState(false);
 const total=Number((Number(accepted)+Number(damaged)+Number(missing)).toFixed(6));
 const discrepancy=Number(damaged)+Number(missing)>0;
 const valid=[accepted,damaged,missing].every(v=>v.trim()!==""&&Number.isFinite(Number(v))&&Number(v)>=0)&&total>0&&total<=remaining&&(!discrepancy||note.trim()!=="");
 return <section className="space-y-4 rounded-xl border border-emerald-200 bg-emerald-50/40 p-4 md:col-span-2">
  <div><h3 className="font-semibold">Receive {name}</h3><p className="mt-1 text-sm text-slate-600">Ordered: {ordered} {unit} · Accepted: {received} · Claimed: {claimed} · Still to check: <strong>{remaining} {unit}</strong></p></div>
  {remaining===0?<p role="status" className="text-sm font-medium text-emerald-800">All quantities have been received or recorded as supplier claims.</p>:review?<div className="space-y-3">
   <h4 className="font-semibold">Confirm this delivery</h4><p className="text-sm">Add <strong>{accepted} {unit}</strong> to sellable stock.</p>
   {discrepancy&&<p className="text-sm">Damaged: {damaged} · Missing: {missing} · {note}</p>}
   <p className="text-sm text-slate-600">{Number((remaining-total).toFixed(6))} {unit} will remain to check. Confirm only after checking the actual goods.</p>
   <div className="flex flex-wrap gap-3"><button disabled={disabled||!valid} className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50" onClick={()=>onConfirm({quantity:accepted,damagedQuantity:damaged,missingQuantity:missing,note:note||null,resolution})}>Confirm received goods</button><button disabled={disabled} className="rounded-lg border bg-white px-4 py-2 text-sm" onClick={()=>setReview(false)}>Back to quantities</button></div>
  </div>:<form className="space-y-4" onSubmit={e=>{e.preventDefault();if(valid)setReview(true)}}><fieldset disabled={disabled} className="space-y-4">
   <label className="grid gap-1 text-sm">Accepted quantity ({unit})<input className={field} required type="number" min="0" max={remaining} step="0.000001" value={accepted} onChange={e=>setAccepted(e.target.value)}/></label>
   <button type="button" className="text-sm font-medium text-emerald-800 underline" onClick={()=>{setAccepted(String(remaining));setDamaged("0");setMissing("0")}}>All remaining goods arrived in good condition</button>
   <details className="rounded-lg border bg-white p-3"><summary className="cursor-pointer text-sm font-medium">Damaged or missing goods</summary><div className="mt-3 grid gap-3 sm:grid-cols-2">
    <label className="grid gap-1 text-sm">Damaged quantity<input className={field} required type="number" min="0" max={remaining} step="0.000001" value={damaged} onChange={e=>setDamaged(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">Missing quantity<input className={field} required type="number" min="0" max={remaining} step="0.000001" value={missing} onChange={e=>setMissing(e.target.value)}/></label>
    <label className="grid gap-1 text-sm">Next action<select className={field} value={resolution} onChange={e=>setResolution(e.target.value)}><option value="PENDING">Hold / inspect</option><option value="REPLACEMENT">Request replacement</option><option value="SUPPLIER_CREDIT">Request supplier credit</option></select></label>
    <label className="grid gap-1 text-sm">Delivery note / reason<input className={field} maxLength={500} value={note} onChange={e=>setNote(e.target.value)}/></label>
    <p className="text-xs text-slate-600 sm:col-span-2">Damaged and missing goods are recorded as claims. They do not increase stock or automatically issue a refund.</p>
   </div></details>
   {total>remaining&&<p role="alert" className="text-sm text-red-700">The total exceeds the quantity still to check.</p>}
   {discrepancy&&!note.trim()&&<p className="text-sm text-amber-800">Enter a reason for damaged or missing goods.</p>}
   <button disabled={!valid} className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">Review received goods</button>
  </fieldset></form>}
 </section>;
}
