"use client";
import {usePathname} from "next/navigation";
import {applicationForPath,loginRoute} from "@/lib/navigation";
import Link from "next/link";
import { signOut } from "next-auth/react";
import { useLocale } from "@/components/LocaleProvider";

export function LanguageSelect() {
  const {locale,setLocale,t}=useLocale();
  return <select aria-label={t("Language")} value={locale} onChange={e=>setLocale(e.target.value as "en"|"fr"|"rw")} className="rounded-lg border border-slate-300 bg-white p-2 text-sm text-slate-800"><option value="en">English</option><option value="fr">Français</option><option value="rw">Kinyarwanda</option></select>;
}
export default function WorkspaceNavigation({controls=false}:{controls?:boolean}) {
  const {t}=useLocale();const pathname=usePathname();
  return <nav aria-label={t("Workspace navigation")} className="flex flex-wrap items-center gap-3 text-sm">
    
    {controls&&<><LanguageSelect/><button onClick={()=>signOut({callbackUrl:loginRoute(applicationForPath(pathname))})} className="text-red-700">{t("Sign out")}</button></>}
  </nav>;
}
