"use client";
import {useState} from "react";
import {MasterData,Input,Row} from "@/components/operations/MasterData";
import {buttonStyle} from "@/components/operations/ui";
import {api} from "@/lib/hms-api";
import {useLocale} from "@/components/LocaleProvider";
const fields:Input[]=[{key:"roomId",label:"Room",lookup:"rooms",optional:true},{key:"issue",label:"Issue"},{key:"priority",label:"Priority",default:"MEDIUM",options:["LOW","MEDIUM","HIGH","URGENT"]}];
function Actions({ticket}:{ticket:Row}){const {t}=useLocale();const [status,setStatus]=useState(String(ticket.status));const [error,setError]=useState("");const [busy,setBusy]=useState(false);async function update(next:string){setBusy(true);setError("");try{await api(`maintenance/${ticket.id}/${next}`,"PUT");setStatus(next)}catch(e){setError((e as Error).message)}finally{setBusy(false)}}return <div className="rounded-xl border bg-white p-5 space-y-3"><p>{String(ticket.issue)} · {t(status)}</p>{error&&<p role="alert">{t(error)}</p>}{!["RESOLVED","CANCELLED"].includes(status)&&<div className="flex gap-4">{status==="OPEN"&&<button className={buttonStyle} disabled={busy} onClick={()=>update("IN_PROGRESS")}>{t("Start task")}</button>}<button className={buttonStyle} disabled={busy} onClick={()=>update("RESOLVED")}>{t("Mark complete")}</button><button className={buttonStyle} disabled={busy} onClick={()=>update("CANCELLED")}>{t("Cancel")}</button></div>}</div>}
export default function Maintenance(){return <MasterData title="Maintenance" resource="maintenance" fields={fields} columns={["issue","priority","status"]} extra={r=><Actions key={r.id} ticket={r}/>}/>}

