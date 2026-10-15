"use client";

type Row=Record<string,unknown>;
const text=(row:Row,key:string)=>String(row[key]??"");
const rows=(value:unknown):Row[]=>Array.isArray(value)?value as Row[]:[];

export default function CommerceInvoice({detail}:{detail:Row}){
 const purchase=detail.kind==="PURCHASE";
 const priced=detail.total!==undefined;
 return <article id="commerce-receipt" className="rounded-xl border border-slate-200 bg-white p-5 sm:p-7">
  <header className="invoice-grid grid gap-4 border-b border-slate-200 pb-5 sm:grid-cols-2">
   <div><h2 className="text-xl font-bold">{text(detail,"business_name")}</h2><p className="text-sm text-slate-600">{text(detail,"branch_name")} · {text(detail,"location_name")}</p></div>
   <div><p className="text-sm font-semibold uppercase text-emerald-800">{purchase?"Purchase invoice":"Sales invoice / receipt"}</p><p className="text-lg font-bold">#{text(detail,"document_number")}</p><p className="text-xs text-slate-500">{text(detail,"created_at")}</p></div>
  </header>
  <div className="invoice-grid my-5 grid gap-3 text-sm sm:grid-cols-2"><div><p className="text-xs uppercase text-slate-500">{purchase?"Supplier":"Customer"}</p><p className="font-semibold">{text(detail,"party_name")||(purchase?"Not specified":"Walk-in customer")}</p><p>Reference: {text(detail,"reference")||"—"}</p></div><div><p>Served by: {text(detail,"cashier_name")}</p>{purchase&&Boolean(detail.supply_date)&&<p>Supply date: {text(detail,"supply_date")}</p>}{Boolean(detail.due_date)&&<p>Payment due: {text(detail,"due_date")}</p>}<p>Currency: {text(detail,"currency")}</p></div></div>
  <table className="w-full text-left text-sm"><thead className="border-y bg-slate-50"><tr><th className="p-2">Product</th><th className="p-2">{purchase?"Quantity bought":"Quantity"}</th>{priced&&<><th className="p-2 text-right">Unit price</th><th className="p-2 text-right">Tax</th><th className="p-2 text-right">Total</th></>}</tr></thead><tbody>{rows(detail.lines).map(line=><tr className="border-b" key={text(line,"id")}><td className="p-2 font-medium">{text(line,"description")}<span className="block text-xs font-normal text-slate-500">{text(line,"sku")}</span></td><td className="p-2">{text(line,"quantity")} {text(line,"unit")}{purchase&&<span className="block text-xs text-slate-500">Received: {text(line,"received")||"0"} {text(line,"unit")}</span>}</td>{priced&&<><td className="p-2 text-right">{text(line,"unit_price")}</td><td className="p-2 text-right">{text(line,"tax")}</td><td className="p-2 text-right font-medium">{text(line,"line_total")}</td></>}</tr>)}</tbody></table>
  {priced&&<div className="ml-auto mt-5 max-w-sm space-y-2 text-sm"><p className="flex justify-between gap-3"><span>Subtotal</span><span>{text(detail,"subtotal")} {text(detail,"currency")}</span></p><p className="flex justify-between gap-3"><span>Tax</span><span>{text(detail,"tax")} {text(detail,"currency")}</span></p><p className="flex justify-between gap-3 border-t pt-3 text-lg font-bold"><span>Total</span><span>{text(detail,"total")} {text(detail,"currency")}</span></p><p className="flex justify-between gap-3"><span>Outstanding</span><span>{text(detail,"outstanding")} {text(detail,"currency")}</span></p><p className="text-xs text-slate-500">Exchange rate: {text(detail,"exchange_rate")}</p></div>}
  {rows(detail.payments).length>0&&<section className="mt-5 border-t pt-3 text-sm"><h3 className="font-semibold">Payments</h3>{rows(detail.payments).map(p=><p key={text(p,"id")}>{text(p,"account_name")}: {text(p,"amount")} {text(p,"currency")}{p.refund?" · Refund":""}</p>)}</section>}
 </article>;
}
