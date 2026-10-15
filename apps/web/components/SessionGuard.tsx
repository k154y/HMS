"use client";
import {useEffect} from "react";
import {usePathname} from "next/navigation";
import {loginDestination,isLoginPath} from "@/lib/navigation";
export function SessionGuard(){const pathname=usePathname();useEffect(()=>{if(isLoginPath(pathname))return;let active=true;async function check(){try{const response=await fetch("/api/auth/session",{cache:"no-store"});const session=await response.json();if(active&&(!response.ok||!session?.accessToken))window.location.replace(loginDestination(pathname,true))}catch{}}check();const timer=setInterval(check,60000);return()=>{active=false;clearInterval(timer)}},[pathname]);return null}
