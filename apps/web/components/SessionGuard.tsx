"use client";
import {useEffect} from "react";
import {usePathname} from "next/navigation";
export function SessionGuard(){const pathname=usePathname();useEffect(()=>{let active=true;async function check(){try{const response=await fetch("/api/auth/session",{cache:"no-store"});const session=await response.json();if(active&&(!response.ok||!session?.accessToken))window.location.replace("/login?reason=expired")}catch{}}check();const timer=setInterval(check,60000);return()=>{active=false;clearInterval(timer)}},[pathname]);return null}
