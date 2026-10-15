import CommercePanel from "@/components/commerce/CommercePanel";
import {auth} from "@/lib/auth";
import {redirect} from "next/navigation";
import {loginDestination} from "@/lib/navigation";
import WorkspaceAccessState, {type AccessState} from "@/components/WorkspaceAccessState";

export default async function CommerceHome() {
 const session=await auth();
 if(!session?.accessToken)redirect(loginDestination("/commerce"));
 if((session.user as {mustChangePassword?:boolean}).mustChangePassword)redirect("/security?next=%2Fcommerce");
 let response:Response;
 try {
  response=await fetch(`${process.env.HMS_API_URL??"http://localhost:8081/api/v1"}/commerce/businesses`,{
   headers:{Authorization:`Bearer ${session.accessToken}`},cache:"no-store",signal:AbortSignal.timeout(15000),
  });
 }catch{return <WorkspaceAccessState state="unavailable"/>;}
 if(response.status===401)redirect(loginDestination("/commerce",true));
 const body=await response.json().catch(()=>null);
 if(body?.code==="PASSWORD_CHANGE_REQUIRED")redirect("/security?next=%2Fcommerce");
 if(!response.ok){
  const state:AccessState=response.status===404&&body?.code==="COMMERCE_DISABLED"?"disabled":response.status===403||response.status===404?"denied":"unavailable";
  return <WorkspaceAccessState state={state}/>;
 }
 if(!Array.isArray(body))return <WorkspaceAccessState state="unavailable"/>;
 if(!body.length)return <WorkspaceAccessState state="unassigned"/>;
 return <CommercePanel initialBusinesses={body} userId={session.user.id??""}/>;
}
