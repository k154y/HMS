import {PasswordForm} from "@/components/operations/PasswordForm";
import {applicationFromQuery} from "@/lib/navigation";
export default async function Page({searchParams}:{searchParams:Promise<{app?:string}>}){const params=await searchParams;return <main className="min-h-screen bg-slate-100 p-6 flex items-center justify-center"><div className="w-full max-w-lg"><PasswordForm mode="forgot" application={params.app?applicationFromQuery(params.app):undefined}/></div></main>;}
