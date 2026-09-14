"use client";
import {useEffect,useState} from "react";
import {all} from "@/lib/hms-api";
import {Panel,Empty} from "./ui";
import {useLocale} from "@/components/LocaleProvider";
export function ReadRecords({title,resource,columns}:{title:string;resource:string;columns:Record<string,string>}){const {t}=useLocale();const [rows,setRows]=useState<Record<string,unknown>[]>([]);const [error,setError]=useState("");useEffect(()=>{all<Record<string,unknown>>(resource).then(setRows).catch(e=>setError(e.message))},[resource]);return <Panel title={title} error={error}><div className="rounded-xl border bg-white overflow-auto"><table className="w-full text-left text-sm"><thead><tr>{Object.values(columns).map(k=><th className="p-3" key={k}>{t(k)}</th>)}</tr></thead><tbody>{rows.map((row,i)=><tr key={String(row.id??i)} className="border-t">{Object.keys(columns).map(k=><td className="p-3" key={k}>{t(String(row[k]??""))}</td>)}</tr>)}</tbody></table>{!rows.length&&<Empty/>}</div></Panel>}
