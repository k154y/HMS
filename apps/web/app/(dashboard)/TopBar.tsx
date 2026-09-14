"use client";
import {signOut} from "next-auth/react";
import {useLocale} from "@/components/LocaleProvider";
type Props={userName:string;userRole:string;userRoles?:string[];hotelName:string;subscriptionStatus:string;trialEndDate:string};
export default function TopBar({userName,userRole,userRoles,hotelName}:Props){const {locale,setLocale,t}=useLocale();return <header className="flex items-center justify-between gap-4 border-b bg-white px-6 py-3"><div><h2 className="font-semibold text-slate-900">{hotelName||"HotelPro"}</h2><p className="text-sm text-slate-500">{userName} · {(userRoles?.length?userRoles:[userRole]).map(t).join(" · ")}</p></div><div className="flex gap-4 items-center"><select aria-label={t("Language")} value={locale} onChange={e=>setLocale(e.target.value as "en"|"fr"|"rw")} className="rounded border p-2"><option value="en">English</option><option value="fr">Français</option><option value="rw">Kinyarwanda</option></select><button className="text-red-700" onClick={()=>signOut({callbackUrl:"/login"})}>{t("Sign out")}</button></div></header>}

