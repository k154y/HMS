"use client";
import {useEffect,useRef,useState} from "react";
import Link from "next/link";
import {X} from "lucide-react";
import {api} from "@/lib/hms-api";
import {useLocale} from "@/components/LocaleProvider";

type Detail={id:string;reference:string;customerId:string;folioId:string;checkIn:string;checkOut:string;
  adults:number;children:number;status:string;roomIds:string[];notes:string|null};
type Summary={currency:string;reservationTotal:number;advancePaid:number;remaining:number;paymentStatus:string};

export function ReservationDetails({id,names,rooms,onClose}:{id:string;names:Record<string,string>;
  rooms:{id:string;code:string}[];onClose:()=>void}) {
  const {t}=useLocale();
  const dialog=useRef<HTMLDialogElement>(null);
  const [detail,setDetail]=useState<Detail|null>(null);
  const [summary,setSummary]=useState<Summary|null>(null);
  const [error,setError]=useState("");
  const [summaryError,setSummaryError]=useState("");
  const [loading,setLoading]=useState(true);
  useEffect(()=>{
    const element=dialog.current!;
    const opener=document.activeElement as HTMLElement|null;
    element.showModal();
    return ()=>{element.close();opener?.focus();};
  },[]);
  useEffect(()=>{
    let current=true;
    setDetail(null);setSummary(null);setError("");setSummaryError("");setLoading(true);
    Promise.allSettled([api<Detail>(`reservations/${id}`),api<Summary>(`reservations/${id}/payment-summary`)]).then(([reservation,payment])=>{
      if(!current)return;
      if(reservation.status==="fulfilled")setDetail(reservation.value);
      else setError(reservation.reason instanceof Error?reservation.reason.message:"Unable to load reservation details.");
      if(payment.status==="fulfilled")setSummary(payment.value);
      else setSummaryError(payment.reason instanceof Error?payment.reason.message:"Unable to load reservation payment information.");
      setLoading(false);
    });
    return ()=>{current=false;};
  },[id]);
  const amount=(value:number)=>`${Number(value).toLocaleString(undefined,{maximumFractionDigits:4})} ${summary?.currency??""}`;
  return <dialog ref={dialog} aria-labelledby="reservation-detail-title" onCancel={event=>{event.preventDefault();onClose();}}
    className="fixed inset-y-0 left-auto right-0 m-0 h-dvh max-h-none w-full max-w-xl overflow-y-auto border-0 bg-white p-7 text-slate-900 shadow-2xl backdrop:bg-slate-950/30 backdrop:backdrop-blur-sm">
    <header className="mb-6 flex items-start justify-between gap-4">
      <div><h2 id="reservation-detail-title" className="text-2xl font-semibold">{t("Reservation details")}</h2>
        {detail&&<p className="mt-2 text-sm text-slate-500">{detail.reference}</p>}</div>
      <button type="button" autoFocus aria-label={t("Close")} onClick={onClose} className="rounded-full bg-slate-100 p-2 focus-visible:outline-2 focus-visible:outline-blue-600"><X size={20}/></button>
    </header>
    {loading&&<p role="status">{t("Loading reservation details...")}</p>}
    {error&&<p role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{t(error)}</p>}
    {detail&&<div className="space-y-6">
      <dl className="grid grid-cols-2 gap-5">{[
        ["Customer",names[detail.customerId]??detail.customerId],
        ["Status",t(detail.status)],
        ["Check-in",detail.checkIn],["Check-out",detail.checkOut],
        ["Adults",String(detail.adults)],["Children",String(detail.children)],
        ["Assigned rooms",detail.roomIds.map(roomId=>rooms.find(room=>room.id===roomId)?.code??roomId).join(", ")],
      ].map(([label,value])=><div key={label}><dt className="text-sm text-slate-500">{t(label)}</dt><dd className="mt-1 break-words font-medium">{value}</dd></div>)}</dl>
      {detail.notes&&<div><h3 className="text-sm text-slate-500">{t("Notes")}</h3><p className="mt-1 whitespace-pre-wrap">{detail.notes}</p></div>}
      <section className="space-y-3 rounded-xl border bg-slate-50 p-4"><h3 className="font-semibold">{t("Payment")}</h3>
        {summary&&<dl className="space-y-3">{[["Reservation total",amount(summary.reservationTotal)],["Advance received",amount(summary.advancePaid)],
          ["Remaining",amount(summary.remaining)],["Payment status",t(summary.paymentStatus)]].map(([label,value])=>
            <div key={label} className="flex justify-between gap-4"><dt>{t(label)}</dt><dd className="font-medium tabular-nums">{value}</dd></div>)}</dl>}
        {summaryError&&<p role="alert" className="text-sm text-red-700">{t(summaryError)}</p>}
      </section>
      {detail.folioId&&<Link href={`/folios/${detail.folioId}`} className="inline-block font-medium text-blue-700 underline">{t("Open folio")}</Link>}
    </div>}
  </dialog>;
}
