"use client";
import {FormEvent,useEffect,useState} from "react";
import {useLocale} from "@/components/LocaleProvider";
import {loginDestination} from "@/lib/navigation";
type Row=Record<string,unknown>;
export default function CommerceStockOperations({businessId,locationId,products,canWrite,onPosted,trackedProductId,onClose}:{businessId:string;locationId:string;trackedProductId:string|null;onClose:()=>void;products:{id:string;name:string}[];canWrite:boolean;onPosted:()=>Promise<void>}) {
 const {t}=useLocale();const [ledger,setLedger]=useState<Row[]>([]),[locations,setLocations]=useState<Row[]>([]);
 const [kind,setKind]=useState("ADJUSTMENT"),[busy,setBusy]=useState(false),[message,setMessage]=useState("");
 // Persist the exact command during uncertain responses. Editing requires an explicit new command.
 const [pending,setPending]=useState<Record<string,unknown>|null>(null);
 async function api(path:string,data?:unknown){const res=await fetch(`/api/commerce/businesses/${businessId}/${path}`,{method:data===undefined?"GET":"POST",cache:"no-store",headers:data===undefined?{}:{"Content-Type":"application/json"},body:data===undefined?undefined:JSON.stringify(data)});
  if(res.status===401)window.location.replace(loginDestination("/commerce",true));
  if(!res.ok)throw new Error(res.status===409?"Stock operation rejected. Check available quantity and the request details.":"Unable to complete the stock operation. Retry the same request.");return res.json();}
 useEffect(()=>{let cancelled=false;setLedger([]);setMessage("");
  Promise.all([trackedProductId?api(`locations/${locationId}/ledger?product=${trackedProductId}`):Promise.resolve([]),api("branches").then(async(bs:Row[])=>{const lists=await Promise.all(bs.filter(b=>b.active).map(async b=>(await api(`branches/${b.id}/locations`)).map((l:Row)=>({...l,branchName:b.name}))));return lists.flat();})]).then(([l,d])=>{if(!cancelled){setLedger(l);setLocations(d)}}).catch(e=>{if(!cancelled)setMessage(e.message)});return()=>{cancelled=true};
 },[businessId,locationId,trackedProductId]);
 useEffect(()=>{if(trackedProductId)document.getElementById("stock-tracking")?.scrollIntoView({block:"start"})},[trackedProductId]);
 async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();const fd=new FormData(e.currentTarget);
  const command=pending??{requestId:crypto.randomUUID(),sourceLocation:locationId,destinationLocation:kind==="TRANSFER"?String(fd.get("destination")):null,productId:String(fd.get("product")),quantity:String(fd.get("quantity")),reason:String(fd.get("reason"))};
  setPending(command);setBusy(true);setMessage("");
  try{await api("stock-commands",command);setPending(null);setLedger(trackedProductId?await api(`locations/${locationId}/ledger?product=${trackedProductId}`):[]);await onPosted();setMessage("Stock operation recorded.");}catch(e){setMessage(e instanceof Error?e.message:"Unable to load data")}finally{setBusy(false)}}
 const field="mt-1 block w-full rounded border bg-white p-2";
 return <section className="mt-5 rounded-xl border bg-white p-5"><h2 className="font-semibold">{t("Stock operations")}</h2>
  {message&&<p role="status" className="my-3 rounded bg-amber-50 p-3">{t(message)}</p>}
  {canWrite&&<details className="my-3 rounded-lg border p-3"><summary className="cursor-pointer font-medium">Adjust or transfer stock</summary><form onSubmit={submit} className="my-4 space-y-3"><fieldset disabled={busy||!!pending} className="grid gap-3 md:grid-cols-2">
   <label>{t("Operation")}<select value={kind} onChange={e=>setKind(e.target.value)} className={field}><option value="ADJUSTMENT">{t("Stock adjustment")}</option><option value="TRANSFER">{t("Stock transfer")}</option></select></label>
   <label>{t("Product")}<select required name="product" className={field}><option value="">{t("Select")}</option>{products.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
   {kind==="TRANSFER"&&<label>{t("Destination location")}<select required name="destination" className={field}><option value="">{t("Select")}</option>{locations.filter(l=>l.id!==locationId&&l.active).map(l=><option key={String(l.id)} value={String(l.id)}>{String(l.branchName)} — {String(l.name)}</option>)}</select></label>}
   <label>{t("Quantity")}<input required type="number" step="0.000001" min={kind==="TRANSFER"?"0.000001":undefined} name="quantity" className={field}/></label>
   <label>{t("Reason")}<input required name="reason" maxLength={500} className={field}/></label>
  </fieldset><button disabled={busy} className="rounded bg-emerald-700 px-4 py-2 text-white disabled:opacity-50">{t(pending?"Retry same request":"Save")}</button>
  {pending&&<p className="text-sm text-slate-600">{t("Retry preserves the original request. Check the ledger before starting another operation.")}</p>}
  </form></details>}
  {trackedProductId&&<section id="stock-tracking" className="mt-4 space-y-3"><div className="flex justify-between gap-3"><h3 className="font-semibold">{products.find(p=>p.id===trackedProductId)?.name} · Stock movements</h3><button type="button" className="rounded border px-3 py-1 text-sm" onClick={onClose}>Close</button></div><p className="text-xs text-slate-500">Latest 500 movements for this product at the selected stock location. Positive quantities add stock; negative quantities remove it.</p><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{["Date","Product","Operation","Quantity","Reason"].map(k=><th className="p-2" key={k}>{t(k)}</th>)}</tr></thead><tbody>{ledger.map(l=><tr key={String(l.id)} className="border-t"><td className="p-2">{String(l.created_at)}</td><td className="p-2">{String(l.name)}</td><td className="p-2">{String(l.movement_type)}</td><td className="p-2">{String(l.quantity_delta)}</td><td className="p-2">{String(l.reason??"")}</td></tr>)}</tbody></table>{!ledger.length&&<p>{t("No records yet")}</p>}</div></section>}
 </section>;
}
