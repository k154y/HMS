import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import PlatformPanel from "./PlatformPanel";
export default async function Page(){
 const session=await auth();
 if(!session?.accessToken)redirect('/login');
 if(session.user.role!=="SUPER_ADMIN")redirect('/dashboard');
 return <PlatformPanel/>;
}

