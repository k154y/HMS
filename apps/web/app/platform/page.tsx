import {productAccess} from "@/lib/product-access";
import WorkspaceAccessState from "@/components/WorkspaceAccessState";
import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import {loginDestination} from "@/lib/navigation";
export default async function Page(){
 const session=await auth();
 if(!session?.accessToken)redirect(loginDestination('/platform'));
 const access=await productAccess(session.accessToken,"PLATFORM_ADMIN");
 if(access.state==="expired")redirect(loginDestination("/platform",true));
 if(access.state!=="allowed")return <WorkspaceAccessState state={access.state==="unavailable"?"unavailable":"denied"}/>;
 if(access.profile?.mustChangePassword)redirect("/security?next=%2Fplatform%2Forganizations");
 redirect('/platform/organizations');
}

