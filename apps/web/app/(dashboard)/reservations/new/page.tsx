"use client";
import {useEffect,useState,useRef} from "react";
import Link from "next/link";
import {ArrowLeft,RefreshCw,CalendarDays,BedDouble,Users,CheckCircle2,ClipboardList} from "lucide-react";
import {useSession} from "next-auth/react";
import {effectiveReservationRate,validReservationRate} from "@/components/operations/reservation-rate";
import {api,all} from "@/lib/hms-api";
import {Panel,Field,inputStyle,buttonStyle} from "@/components/operations/ui";
import {useLocale} from "@/components/LocaleProvider";
type Room={id:string;code:string;roomTypeId:string;adults:number;children:number;bedType:string;bedDimensions:string;nightlyRate:string|number};type Customer={id:string;name:string;kind:string;active:boolean};
export default function NewReservation(){const {t}=useLocale();const [checkIn,setIn]=useState("");const [checkOut,setOut]=useState("");const [rooms,setRooms]=useState<Room[]>([]);const [customers,setCustomers]=useState<Customer[]>([]);const [customer,setCustomer]=useState("");const [selected,setSelected]=useState<Record<string,{adults:number;children:number;rate:string}>>({});const [notes,setNotes]=useState("");const [error,setError]=useState("");const [busy,setBusy]=useState(false);const [preview,setPreview]=useState(false);const [result,setResult]=useState<{reference:string;folioId:string}|null>(null);const [checked,setChecked]=useState("");
 const {data:session}=useSession();
 const canSetRate=((session?.user as {permissions?:string[]})?.permissions??[]).includes("ROOM_RATE_MANAGE");
 const [currency,setCurrency]=useState("");
 const previewButton=useRef<HTMLButtonElement>(null),previewPanel=useRef<HTMLDialogElement>(null);
 const confirmation=useRef<HTMLElement>(null),posting=useRef(false);
 useEffect(()=>{api<{currencyCode:string}>("settings").then(h=>setCurrency(h.currencyCode)).catch(()=>setCurrency(""))},[]);
 useEffect(()=>{
  if(!preview||!previewPanel.current)return;
  const dialog=previewPanel.current,trigger=previewButton.current,overflow=document.body.style.overflow;
  document.body.style.overflow="hidden";dialog.showModal();dialog.focus();
  return()=>{dialog.close();document.body.style.overflow=overflow;if(trigger?.isConnected)trigger.focus({preventScroll:true})};
 },[preview]);
 function closePreview(){if(!posting.current)setPreview(false)}
 useEffect(()=>{const q=new URLSearchParams(window.location.search);setIn(q.get("checkIn")??"");setOut(q.get("checkOut")??"");all<Customer>("customers").then(c=>setCustomers(c.filter(x=>x.active))).catch(e=>setError(e.message))},[]);
 function dates(a:string,b:string){setIn(a);setOut(b);setRooms([]);setSelected({});setPreview(false);setChecked("");setResult(null)}
 async function availability(){setBusy(true);setError("");try{const list=await all<Room>(`reservations/availability?checkIn=${checkIn}&checkOut=${checkOut}&adults=1&children=0`);setRooms(list);setSelected({});setChecked(`${checkIn}/${checkOut}`)}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 async function reserve(){if(posting.current||Object.values(selected).some(r=>!validReservationRate(r.rate)))return;posting.current=true;setBusy(true);setError("");try{setResult(await api("reservations","POST",{bookingCustomerId:customer,checkIn,checkOut,adults:Object.values(selected).reduce((n,r)=>n+r.adults,0),children:Object.values(selected).reduce((n,r)=>n+r.children,0),rooms:Object.entries(selected).map(([roomId,counts])=>({roomId,adults:counts.adults,children:counts.children,...(canSetRate&&counts.rate!==""?{rate:counts.rate}:{}),guestIds:[]})),notes}));setSelected({});setRooms([]);setChecked("");setPreview(false)}catch(e){setError((e as Error).message)}finally{posting.current=false;setBusy(false)}}
 useEffect(()=>{if(result){confirmation.current?.focus({preventScroll:true});confirmation.current?.scrollIntoView({block:"nearest"})}},[result]);

 const ready=checked===`${checkIn}/${checkOut}`;
 const selectedCount=Object.keys(selected).length;
 const canPreview=ready&&!!customer&&selectedCount>0&&!Object.entries(selected).some(([id,x])=>{
  const r=rooms.find(room=>room.id===id);
  return !r||!validReservationRate(x.rate)||x.adults<1||x.adults>r.adults||x.children<0||x.children>r.children||!Number.isInteger(x.adults)||!Number.isInteger(x.children);
 });
 async function refresh(){
  setBusy(true);setError("");setPreview(false);
  try {
   const [people,available]=await Promise.all([all<Customer>("customers"),checkIn&&checkOut>checkIn?all<Room>(`reservations/availability?checkIn=${checkIn}&checkOut=${checkOut}&adults=1&children=0`):Promise.resolve(null)]);
   setCustomers(people.filter(c=>c.active));setSelected({});setResult(null);
   setRooms(available??[]);setChecked(available?`${checkIn}/${checkOut}`:"");
  }catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 const secondary="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-blue-600 disabled:opacity-50";
 const card="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6";
 return <Panel title="New reservation" error={error}>
  <nav aria-label={t("Reservation actions")} className="flex flex-wrap items-center justify-between gap-3">
   <Link href="/reservations" className={secondary}><ArrowLeft size={17} aria-hidden="true"/>{t("Back to reservations")}</Link>
   <button type="button" className={secondary} disabled={busy} onClick={refresh}><RefreshCw size={16} aria-hidden="true" className={busy?"animate-spin":""}/>{t("Refresh")}</button>
  </nav>
  {result?<section ref={confirmation} role="status" tabIndex={-1} className="space-y-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-6">
   <CheckCircle2 className="text-emerald-600" size={30} aria-hidden="true"/>
   <h2 className="text-xl font-semibold text-emerald-950">{t("Reservation confirmed")}</h2><p className="font-mono text-emerald-800">{result.reference}</p>
   <div className="flex flex-wrap gap-3"><Link className={buttonStyle} href="/reservations">{t("Back to reservations")}</Link><Link className={secondary} href={`/folios/${result.folioId}`}>{t("Open customer folio")}</Link></div>
  </section>:<div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_300px]">
   <div className="min-w-0 space-y-6">
    <section className={card}>
     <header className="mb-5 flex items-center gap-3"><span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 font-semibold text-blue-700">1</span><div><h2 className="font-semibold text-slate-900">{t("Stay dates")}</h2><p className="mt-1 text-sm text-slate-500">{t("Choose dates first")}</p></div></header>
     <div className="grid items-end gap-4 sm:grid-cols-2">
      <Field label="Check-in date"><input className={inputStyle} type="date" value={checkIn} disabled={busy} onChange={e=>dates(e.target.value,checkOut)}/></Field>
      <Field label="Check-out date"><input className={inputStyle} type="date" min={checkIn} value={checkOut} disabled={busy} onChange={e=>dates(checkIn,e.target.value)}/></Field>
     </div>
     <div className="mt-5 flex flex-wrap items-center justify-between gap-3"><p className="text-xs text-slate-500">{t("Check availability before choosing rooms.")}</p><button className={buttonStyle+" inline-flex items-center gap-2"} disabled={busy||!checkIn||checkOut<=checkIn} onClick={availability}><CalendarDays size={16} aria-hidden="true"/>{t("Check availability")}</button></div>
    </section>
    {ready?<>
     <section className={card}>
      <header className="mb-5 flex items-center gap-3"><span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 font-semibold text-blue-700">2</span><h2 className="font-semibold text-slate-900">{t("Customer details")}</h2></header>
      <Field label="Person or company"><select disabled={busy} className={inputStyle} value={customer} onChange={e=>{setCustomer(e.target.value);setPreview(false)}}><option value="">{t("Select customer")}</option>{customers.map(c=><option key={c.id} value={c.id}>{c.name} · {t(c.kind)}</option>)}</select></Field>
      <Link href="/customers" className="mt-3 inline-block text-sm font-medium text-blue-700 underline underline-offset-4">{t("Create customer or company")}</Link>
     </section>
     <section className={card}>
      <header className="mb-5 flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-3"><span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 font-semibold text-blue-700">3</span><h2 className="font-semibold text-slate-900">{t("Choose rooms")}</h2></div><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">{t("Selected rooms")}: {selectedCount}</span></header>
      <div className="grid gap-4 lg:grid-cols-2">{rooms.map(r=><article key={r.id} className={`min-w-0 rounded-xl border p-4 transition ${selected[r.id]?"border-blue-400 bg-blue-50/40 ring-1 ring-blue-200":"border-slate-200 bg-white"}`}>
       <label className="flex cursor-pointer items-center justify-between gap-3"><span className="flex items-center gap-2 text-lg font-semibold text-slate-900"><BedDouble size={20} className="text-slate-400" aria-hidden="true"/>{t("Room")} {r.code}</span><input disabled={busy} className="h-5 w-5 accent-blue-600" type="checkbox" checked={!!selected[r.id]} onChange={e=>{const next={...selected};if(e.target.checked)next[r.id]={adults:1,children:0,rate:""};else delete next[r.id];setSelected(next);setPreview(false)}}/></label>
       <p className="mt-2 text-sm text-slate-500">{r.bedType} · {r.bedDimensions}</p>
       <p className="mt-2 flex items-center gap-2 text-xs text-slate-500"><Users size={14} aria-hidden="true"/>{t("Capacity")}: {r.adults} {t("Adults")}, {r.children} {t("Children")}</p>
       <div className="mt-4 border-t border-slate-200 pt-3"><p className="text-xs text-slate-500">{t("Configured rate")}</p><p className="mt-1 font-semibold tabular-nums text-slate-900">{String(r.nightlyRate)} <span className="text-xs font-normal text-slate-500">{currency||t("Hotel currency")}/{t("night")}</span></p></div>
       {selected[r.id]&&<fieldset disabled={busy} className="mt-4 space-y-4 border-t border-blue-100 pt-4">
        <div className="grid grid-cols-2 gap-3">{(["adults","children"] as const).map(k=><Field label={k==="adults"?"Adults":"Children"} key={k}><input className={inputStyle} type="number" min={k==="adults"?1:0} max={r[k]} value={selected[r.id][k]} onChange={e=>{setSelected({...selected,[r.id]:{...selected[r.id],[k]:Number(e.target.value)}});setPreview(false)}}/></Field>)}</div>
        {canSetRate&&<Field label="Custom room rate (optional)"><input className={inputStyle} inputMode="decimal" value={selected[r.id].rate} aria-invalid={!validReservationRate(selected[r.id].rate)} onChange={e=>{setSelected({...selected,[r.id]:{...selected[r.id],rate:e.target.value}});setPreview(false)}}/><span className="block text-xs font-normal text-slate-500">{t("Leave empty to use the configured rate.")}</span>{!validReservationRate(selected[r.id].rate)&&<span role="alert" className="block text-xs text-red-700">{t("Enter a nonnegative amount with up to 15 digits and 4 decimal places.")}</span>}</Field>}
       </fieldset>}
      </article>)}</div>
      {!rooms.length&&<div className="rounded-xl border border-dashed p-8 text-center text-slate-500"><BedDouble className="mx-auto mb-3" size={28} aria-hidden="true"/>{t("No rooms available for these dates")}</div>}
     </section>
     <section className={card}><Field label="Notes"><textarea disabled={busy} rows={3} className={inputStyle} value={notes} onChange={e=>{setNotes(e.target.value);setPreview(false)}}/></Field></section>
     <section className="space-y-4">
      {preview&&<dialog ref={previewPanel} id="reservation-preview" role="dialog" aria-modal="true" aria-labelledby="reservation-preview-title" tabIndex={-1} onCancel={e=>{e.preventDefault();closePreview()}} className="fixed inset-0 m-auto max-h-[85dvh] w-[calc(100%-2rem)] max-w-2xl overflow-y-auto overscroll-contain rounded-2xl border border-blue-200 bg-blue-50 p-5 text-slate-900 shadow-2xl backdrop:bg-slate-950/50 space-y-4 focus-visible:outline-2 focus-visible:outline-blue-600">
       <div><h2 id="reservation-preview-title" className="font-semibold text-blue-950">{t("Preview reservation")}</h2><p className="mt-1 text-sm text-blue-800">{customers.find(c=>c.id===customer)?.name} · {checkIn} → {checkOut}</p></div>
       {Object.entries(selected).map(([id,c])=><div key={id} className="rounded-xl bg-white p-4"><p className="font-semibold">{t("Room")} {rooms.find(r=>r.id===id)?.code}</p><p className="mt-1 text-sm text-slate-500">{c.adults} {t("Adults")}, {c.children} {t("Children")}</p><p className="mt-2 text-sm">{t("Effective nightly rate")}: <strong>{effectiveReservationRate(canSetRate?c.rate:"",rooms.find(r=>r.id===id)!.nightlyRate)} {currency||t("Hotel currency")}/{t("night")}</strong></p></div>)}
       {notes&&<p className="whitespace-pre-wrap break-words text-sm">{t("Notes")}: {notes}</p>}
       {error&&<p role="alert" className="text-red-700">{error}</p>}
       <div className="flex flex-wrap gap-3"><button className={buttonStyle} disabled={busy} onClick={reserve}>{t("Confirm reservation")}</button><button type="button" className={secondary} disabled={busy} onClick={closePreview}>{t("Close")}</button></div>
      </dialog>}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5"><Link href="/reservations" className="text-sm font-medium text-slate-600 hover:text-blue-700">{t("Cancel")}</Link><button ref={previewButton} aria-expanded={preview} aria-controls="reservation-preview" aria-haspopup="dialog" className={buttonStyle+" inline-flex items-center gap-2"} disabled={busy||!canPreview} onClick={()=>setPreview(true)}><ClipboardList size={16} aria-hidden="true"/>{t("Preview reservation")}</button></div>
     </section>
    </>:<section className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500"><BedDouble size={30} className="mx-auto mb-3 text-slate-400" aria-hidden="true"/>{t("Available rooms will appear after checking your dates.")}</section>}
   </div>
   <aside className={card+" space-y-5 xl:sticky xl:top-6"}>
    <h2 className="flex items-center gap-2 font-semibold"><ClipboardList size={18} className="text-blue-600" aria-hidden="true"/>{t("Reservation summary")}</h2>
    <dl className="space-y-4 text-sm">{[["Check-in date",checkIn||"—"],["Check-out date",checkOut||"—"],["Person or company",customers.find(c=>c.id===customer)?.name||"—"],["Selected rooms",String(selectedCount)]].map(([label,value])=><div key={label}><dt className="text-xs text-slate-500">{t(label)}</dt><dd className="mt-1 break-words font-medium text-slate-900">{value}</dd></div>)}</dl>
    <p className="border-t pt-4 text-xs leading-relaxed text-slate-500">{t("Review the details before confirming.")}</p>
   </aside>
  </div>}
 </Panel>;
}
