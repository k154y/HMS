"use client";
import { useEffect,useState } from "react";
import { api,all } from "@/lib/hms-api";
import { useLocale } from "@/components/LocaleProvider";
import { Panel,Field,inputStyle,buttonStyle } from "@/components/operations/ui";
import {OrderHistory} from "@/components/operations/OrderHistory";
import Link from "next/link";
import {useSession} from "next-auth/react";
type Customer={id:string;name:string;active:boolean};
type Product={id:string;name:string;sellingPrice:number;taxRate:number;active:boolean;sellable:boolean;destination:string};
type Line={product:Product;quantity:number};
export default function POS(){
 const {t}=useLocale();const {data:session}=useSession();const canManage=(session?.user as {permissions?:string[]})?.permissions?.includes("PRODUCT_MANAGE");const [customers,setCustomers]=useState<Customer[]>([]);const [products,setProducts]=useState<Product[]>([]);
 const [customer,setCustomer]=useState("");const [product,setProduct]=useState("");const [quantity,setQuantity]=useState(1);const [cart,setCart]=useState<Line[]>([]);
 const [preview,setPreview]=useState(false);const [busy,setBusy]=useState(false);const [error,setError]=useState("");const [saved,setSaved]=useState<{id:string;folioId:string}|null>(null);
 useEffect(()=>{Promise.all([all<Customer>("customers"),all<Product>("products")]).then(([c,p])=>{setCustomers(c.filter(x=>x.active));setProducts(p.filter(x=>x.active&&x.sellable))}).catch(e=>setError(e.message))},[]);
 const selected=products.find(p=>p.id===product);const lineTotal=(p:Product,q:number)=>Math.round(Number(p.sellingPrice)*q*(1+Number(p.taxRate))*10000)/10000;
 const total=cart.reduce((sum,line)=>sum+lineTotal(line.product,line.quantity),0);
 function add(){if(!selected||quantity<=0)return;setCart(old=>[{product:selected,quantity:quantity+(old.find(x=>x.product.id===selected.id)?.quantity??0)},...old.filter(x=>x.product.id!==selected.id)]);setQuantity(1);setPreview(false);setSaved(null);}
 async function send(){setBusy(true);setError("");try{let order=saved;if(!order){order=await api<{id:string;folioId:string}>("orders","POST",{customerId:customer,destination:cart[0].product.destination,items:cart.map(l=>({productId:l.product.id,quantity:l.quantity}))});setSaved(order)}await api(`orders/${order.id}/send`,"POST");setCart([]);setPreview(false)}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 return <Panel title="POS / Orders" error={error}>{canManage&&<div className="flex justify-end"><Link href="/menu" className={buttonStyle}>{t("Manage menu items")}</Link></div>}<div className="grid gap-6 lg:grid-cols-2"><div className="space-y-4 rounded-xl border bg-white p-5">
 <Field label="Customer"><select className={inputStyle} value={customer} disabled={!!saved&&cart.length>0} onChange={e=>setCustomer(e.target.value)}><option value="">{t("Select customer")}</option>{customers.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>
 <div className="space-y-3"><h2 className="font-semibold">{t("Menu and drinks")}</h2><div className="grid grid-cols-2 gap-3 max-h-72 overflow-auto">{products.map(p=><button type="button" key={p.id} disabled={busy||(!!saved&&cart.length>0)} onClick={()=>setProduct(p.id)} className={`rounded-xl border p-4 text-left transition ${product===p.id?"border-blue-500 bg-blue-50 ring-1 ring-blue-500":"border-slate-200 hover:border-blue-300"}`}><span className="block text-xs uppercase tracking-wide text-slate-500">{t(p.destination)}</span><span className="block font-semibold my-1">{p.name}</span><span className="text-blue-700">{lineTotal(p,1).toFixed(2)}</span></button>)}</div></div><Field label="Product"><select className={inputStyle} value={product} onChange={e=>setProduct(e.target.value)}><option value="">{t("Select product")}</option>{products.map(p=><option key={p.id} value={p.id}>{p.name} — {p.sellingPrice}</option>)}</select></Field>
 <Field label="Quantity"><input type="number" min="0.0001" step="0.0001" className={inputStyle} value={quantity} onChange={e=>setQuantity(Number(e.target.value))}/></Field>
 <p className="text-xl font-semibold">{t("Item total")}: {selected?lineTotal(selected,quantity).toFixed(2):"0.00"}</p><button className={buttonStyle} disabled={!selected||quantity<=0||busy||(!!saved&&cart.length>0)} onClick={add}>{t("Confirm item")}</button></div>
 <div className="space-y-4 rounded-xl border bg-white p-5"><h2 className="font-semibold">{t("Cart")}</h2>{cart.map(l=><div className="flex justify-between border-b py-2" key={l.product.id}><span>{l.quantity} × {l.product.name} <small>({t(l.product.destination)})</small></span><span>{lineTotal(l.product,l.quantity).toFixed(2)} <button disabled={busy||!!saved} onClick={()=>{setCart(cart.filter(x=>x.product.id!==l.product.id));setPreview(false)}} className="ml-3 text-red-600">{t("Remove")}</button></span></div>)}<p className="text-xl font-semibold">{t("Grand total")}: {total.toFixed(2)}</p>
 {!preview?<button className={buttonStyle} disabled={!customer||!cart.length} onClick={()=>setPreview(true)}>{t("Preview order")}</button>:<div className="rounded-lg border border-blue-200 bg-blue-50 p-4 space-y-3"><p>{t("Confirm order for")}: {customers.find(c=>c.id===customer)?.name}</p><p>{t("Grand total")}: {total.toFixed(2)}</p><button className={buttonStyle} disabled={busy} onClick={send}>{t(busy?"Saving":"Confirm and send")}</button></div>}
 {saved&&!cart.length&&<p role="status">{t("Order sent")} · <Link className="text-blue-700 underline" href={`/folios/${saved.folioId}`}>{t("Open customer folio")}</Link></p>}</div></div><OrderHistory/></Panel>
}



