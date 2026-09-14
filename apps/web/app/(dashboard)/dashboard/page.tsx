"use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {api} from "@/lib/hms-api";
import {Panel} from "@/components/operations/ui";
import {useLocale} from "@/components/LocaleProvider";
export default function Dashboard(){const {t}=useLocale();const [data,setData]=useState<{date:string;cards:{label:string;value:number;href:string}[]}|null>(null);const [error,setError]=useState("");useEffect(()=>{api<typeof data>("dashboard").then(setData).catch(e=>setError(e.message))},[]);return <Panel title="Dashboard" error={error}><p className="text-slate-500">{data?.date??t("Loading")}</p><div className="grid gap-5 md:grid-cols-3">{data?.cards.map(c=><Link key={c.label} href={c.href} className="rounded-xl border bg-white p-6 shadow-sm hover:border-blue-400"><p className="text-sm text-slate-600">{t(c.label)}</p><p className="mt-2 text-3xl font-semibold text-slate-900">{c.value}</p></Link>)}</div></Panel>}
