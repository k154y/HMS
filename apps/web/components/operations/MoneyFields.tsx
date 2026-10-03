"use client";
import {useLocale} from "@/components/LocaleProvider";
import {useEffect,useState} from "react";
import {all,api} from "@/lib/hms-api";
import {Field,inputStyle} from "./ui";
export type HotelCurrency={code:string;name:string;enabled:boolean};
export type PaymentAccount={id:string;name:string;method:string;currency:string;identifierType:string;maskedIdentifier:string|null;active:boolean};
export function CurrencyField({value,onChange,label="Currency",optional=false,bare=false}:{value:string;onChange:(value:string)=>void;label?:string;optional?:boolean;bare?:boolean}) {
 const {t}=useLocale();
 const [rows,setRows]=useState<HotelCurrency[]>([]);const [error,setError]=useState("");
 useEffect(()=>{all<HotelCurrency>("currencies").then(setRows).catch(()=>setError("Unable to load hotel currencies."));},[]);
 const content=<><select className={inputStyle} required={!optional} value={value} onChange={e=>onChange(e.target.value)}><option value="">{t(optional?"Hotel accounting currency":"Select currency")}</option>{value&&!rows.some(c=>c.code===value)&&<option value={value}>{value}</option>}{rows.filter(c=>c.enabled||c.code===value).map(c=><option key={c.code} value={c.code} disabled={!c.enabled}>{c.code} — {c.name}{!c.enabled?" (inactive)":""}</option>)}</select>{error&&<span role="alert" className="text-red-700">{error}</span>}</>;
 return bare?content:<Field label={label}>{content}</Field>;
}
export function PaymentAccountField({method,currency,value,onChange}:{method:string;currency:string;value:string;onChange:(id:string)=>void}) {
 const {t}=useLocale();
 const [rows,setRows]=useState<PaymentAccount[]>([]);const [error,setError]=useState("");
 useEffect(()=>{all<PaymentAccount>("payment-accounts").then(setRows).catch(()=>setError("Unable to load payment accounts."));},[]);
 const [base,setBase]=useState("");
 useEffect(()=>{api<{currency:string}>("currencies/base").then(r=>setBase(r.currency)).catch(()=>setError("Unable to load accounting currency."));},[]);
 const available=rows.filter(a=>a.active&&a.method===method&&a.currency===(currency||base));
 useEffect(()=>{if(value&&rows.length&&!rows.some(a=>a.id===value&&a.active&&a.method===method&&a.currency===(currency||base)))onChange("");},[rows,value,method,currency,base,onChange]);
 return <Field label="Payment account"><select className={inputStyle} value={value} required={available.length>0} onChange={e=>onChange(e.target.value)}><option value="">{t(available.length?"Select account":"No configured account")}</option>{available.map(a=><option key={a.id} value={a.id}>{a.name}{a.maskedIdentifier?` — ${a.maskedIdentifier}`:""}</option>)}</select>{error&&<span role="alert" className="text-red-700">{error}</span>}</Field>;
}
