"use client";
import Link from "next/link";
import {ArrowUpRight, Building2, Boxes, Check} from "lucide-react";
import {useLocale} from "@/components/LocaleProvider";
import {LanguageSelect} from "@/components/WorkspaceNavigation";

export default function PlatformLanding(){
 const {t}=useLocale();
 return <main className="min-h-screen bg-[#f5f6f2] text-slate-900">
  <header className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-6 py-6 lg:px-10">
   <Link href="/" aria-label="HotelPro" className="flex items-center gap-3 text-xl font-bold tracking-tight"><span className="rounded-xl bg-slate-900 p-2.5 text-white"><Building2 size={22}/></span>HotelPro</Link>
   <div className="flex items-center gap-4"><LanguageSelect/><Link href="/login" className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-700">{t("Sign In")}</Link></div>
  </header>
  <section className="mx-auto max-w-7xl px-6 pb-12 pt-12 lg:px-10 lg:pt-20">
   <p className="mb-5 text-xs font-bold uppercase tracking-[0.18em] text-emerald-800">{t("Built around your business")}</p>
   <h1 className="max-w-4xl text-4xl font-semibold leading-[1.12] tracking-tight sm:text-5xl lg:text-6xl">{t("One platform. Smarter business management.")}</h1>
   <p className="mt-6 max-w-2xl text-lg leading-relaxed text-slate-600">{t("Connected workspaces for hotels, retailers, wholesalers and distributors. Choose the tools that fit your business.")}</p>
   <div className="mt-12 grid gap-6 lg:grid-cols-2">
    <article className="flex flex-col rounded-2xl border border-slate-200 bg-white p-7 sm:p-9">
     <div className="mb-7 flex items-center justify-between"><Building2 className="h-9 w-9 text-blue-800"/><span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-800">{t("For hospitality")}</span></div>
     <h2 className="text-2xl font-semibold tracking-tight">{t("Hotel Management System")}</h2><p className="mt-3 text-slate-600">{t("Keep your front desk, guest services and daily operations connected.")}</p>
     <ul className="my-7 space-y-3 text-sm text-slate-700">{["Reservations and rooms","Guest management","Restaurant and bar","Purchasing and stock","Payments and reporting"].map(label=><li className="flex items-center gap-3" key={label}><Check size={16} className="shrink-0 text-blue-700"/>{t(label)}</li>)}</ul>
     <Link href="/dashboard" className="mt-auto flex items-center justify-between gap-3 rounded-lg bg-blue-800 px-5 py-3.5 font-semibold text-white hover:bg-blue-900">{t("Access Hotel Management")}<ArrowUpRight size={20}/></Link>
    </article>
    <article className="flex flex-col rounded-2xl border border-emerald-900/15 bg-[#edf3ee] p-7 sm:p-9">
     <div className="mb-7 flex items-center justify-between"><Boxes className="h-9 w-9 text-emerald-800"/><span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-emerald-800">{t("For trade and distribution")}</span></div>
     <h2 className="text-2xl font-semibold tracking-tight">{t("Inventory, Sales & Distribution")}</h2><p className="mt-3 text-slate-600">{t("Build your business catalogue and organize stock across branches and warehouses.")}</p>
     <ul className="my-7 space-y-3 text-sm text-slate-700">{["Businesses, branches and stock locations","Categories, products and units","Opening stock and stock balances"].map(label=><li className="flex items-center gap-3" key={label}><Check size={16} className="shrink-0 text-emerald-700"/>{t(label)}</li>)}</ul>
     <p className="mb-7 border-t border-emerald-900/10 pt-4 text-sm leading-relaxed text-slate-600"><strong>{t("Coming next")}: </strong>{t("Retail and wholesale sales, invoicing, distribution and accounting.")}</p>
     <Link href="/commerce" className="mt-auto flex items-center justify-between gap-3 rounded-lg bg-emerald-800 px-5 py-3.5 font-semibold text-white hover:bg-emerald-900">{t("Access Inventory & Sales")}<ArrowUpRight size={20}/></Link>
    </article>
   </div>
   <p className="mt-6 text-sm text-slate-500">{t("One account. Access is managed separately for each workspace.")}</p>
  </section>
  <footer className="mx-auto flex max-w-7xl flex-wrap justify-between gap-4 border-t border-slate-200 px-6 py-8 text-sm text-slate-600 lg:px-10"><span>© {new Date().getUTCFullYear()} HotelPro</span><nav aria-label={t("Product navigation")} className="flex flex-wrap gap-6"><Link href="/dashboard">{t("Hotel Management")}</Link><Link href="/commerce">{t("Inventory & Sales")}</Link></nav></footer>
 </main>;
}
