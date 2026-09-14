"use client";
import {createContext,useContext,useEffect,useState} from "react";
import {messages,type Locale} from "@/lib/i18n";
const LocaleContext=createContext<{locale:Locale;setLocale:(l:Locale)=>void;t:(k:string)=>string}>({locale:"en",setLocale:()=>{},t:k=>k});
export function LocaleProvider({children}:{children:React.ReactNode}){
 const [locale,setLocaleState]=useState<Locale>("en");
 useEffect(()=>{try{const saved=localStorage.getItem("hms-locale") as Locale|null;if(saved&&messages[saved])setLocaleState(saved)}catch{}},[]);
 useEffect(()=>{document.documentElement.lang=locale},[locale]);
 const setLocale=(language:Locale)=>{if(!messages[language])return;setLocaleState(language);try{localStorage.setItem("hms-locale",language)}catch{}};
 return <LocaleContext.Provider value={{locale,setLocale,t:key=>messages[locale][key]??messages.en[key]??key}}>{children}</LocaleContext.Provider>
}
export const useLocale=()=>useContext(LocaleContext);
