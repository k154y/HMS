import {auth} from "@/lib/auth";
import {redirect} from "next/navigation";
import {applicationForPath,loginDestination,safeDestination} from "@/lib/navigation";
import {PasswordForm} from "@/components/operations/PasswordForm";
export default async function Security({searchParams}:{searchParams:Promise<{next?:string}>}){const params=await searchParams;const next=safeDestination(params.next);const session=await auth();if(!session?.accessToken)redirect(loginDestination(next));return <main className="max-w-xl mx-auto p-6"><PasswordForm mode="change" application={applicationForPath(next)}/></main>;}
