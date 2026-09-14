"use client";
import { useEffect,useState,useCallback } from "react";
import { useSession } from "next-auth/react";
import { api } from "@/lib/hms-api";
import { useLocale } from "@/components/LocaleProvider";
import { Panel,Empty,buttonStyle,inputStyle } from "@/components/operations/ui";
type Item={id:string;order_id:string;product_name:string;quantity:number;preparation_status:string;customer_name:string};
export default function Kitchen(){const {t}=useLocale();const {data:session}=useSession();const role=(session?.user as {role?:string})?.role;const [destination,setDestination]=useState("KITCHEN");const [items,setItems]=useState<Item[]>([]);const [error,setError]=useState("");const [busy,setBusy]=useState(false);
 useEffect(()=>{if(role==="BARTENDER")setDestination("BAR")},[role]);
 const load=useCallback(()=>api<Item[]>(`orders/queue?destination=${destination}`).then(setItems).catch(e=>setError(e.message)),[destination]);
 useEffect(()=>{load();const timer=setInterval(load,15000);return()=>clearInterval(timer)},[load]);
 async function advance(item:Item){setBusy(true);setError("");try{await api(`orders/${item.order_id}/items/${item.id}/status`,"POST",{status:item.preparation_status==="SENT"?"PREPARING":"READY"});await load()}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 return <Panel title="Preparation queue" error={error}><select className={`${inputStyle} max-w-xs`} value={destination} onChange={e=>setDestination(e.target.value)} disabled={role==="KITCHEN_STAFF"||role==="BARTENDER"}>{["KITCHEN","BAR","SERVICE"].map(d=><option key={d} value={d}>{t(d)===d?d:t(d)}</option>)}</select><div className="grid gap-4 md:grid-cols-3">{items.map(item=><article key={item.id} className="space-y-3 rounded-xl border bg-white p-5"><p className="text-lg font-semibold">{item.quantity} × {item.product_name}</p><p>{item.customer_name}</p><p>{t(item.preparation_status)}</p>{item.preparation_status!=="READY"&&<button className={buttonStyle} disabled={busy} onClick={()=>advance(item)}>{t(item.preparation_status==="SENT"?"Start preparation":"Mark ready")}</button>}</article>)}</div>{!items.length&&<Empty/>}</Panel>
}

