"use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {all} from "@/lib/hms-api";
import {Row} from "./MasterData";
import {useLocale} from "@/components/LocaleProvider";
export function AccountHistory({record,vendor=false}:{record:Row;vendor?:boolean}){const {t}=useLocale();const [rows,setRows]=useState<Row[]>([]);const [error,setError]=useState("");useEffect(()=>{all<Row>(vendor?"purchase-orders":"folios").then(data=>setRows(data.filter(r=>String(r[vendor?"vendor_id":"customer_id"])===record.id))).catch(e=>setError(e.message))},[record.id,vendor]);return <section className="rounded-xl border bg-white p-5 space-y-4"><h2 className="text-xl font-semibold">{String(record.name)}</h2>{error&&<p role="alert">{t(error)}</p>}<p>{t("Outstanding balance")}: {rows.filter(r=>!vendor||["APPROVED","RECEIVED"].includes(String(r.status))).reduce((s,r)=>s+(vendor?Number(r.total)-Number(r.paid):Number(r.balance)),0).toFixed(2)}</p>{rows.map(r=><div key={r.id} className="border-t pt-3 flex justify-between gap-4"><span>{String(r.reference??r.created_at)} · {t(String(r.status))}</span><span>{vendor?(Number(r.total)-Number(r.paid)).toFixed(2):String(r.balance)}</span><Link className="text-blue-700 underline" href={vendor?"/purchasing":`/folios/${r.id}`}>{t("Details")}</Link></div>)}</section>}
