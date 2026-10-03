import {auth} from "@/lib/auth";
import {redirect} from "next/navigation";
import {PasswordForm} from "@/components/operations/PasswordForm";
export default async function Security(){const session=await auth();if(!session?.accessToken)redirect("/login");return <main className="max-w-xl mx-auto p-6"><PasswordForm mode="change"/></main>;}
