import {auth} from "@/lib/auth";
import {SessionProvider} from "next-auth/react";
import {SessionGuard} from "@/components/SessionGuard";
// The public login and protected product entry share session context; the
// product page and APIs independently enforce membership and authorization.
export default async function CommerceLayout({children}:{children:React.ReactNode}){const session=await auth();return <SessionProvider session={session}><SessionGuard/>{children}</SessionProvider>;}
