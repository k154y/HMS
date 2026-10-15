import {auth} from "@/lib/auth";
import {redirect} from "next/navigation";
import {Application,applicationDestination} from "@/lib/navigation";
import {productAccess} from "@/lib/product-access";
import SharedLogin from "./SharedLogin";
import WorkspaceAccessState from "./WorkspaceAccessState";

export default async function ProductLoginEntry({application,next}:{application:Application;next?:unknown}){
 const session=await auth();
 if(session?.accessToken){
  const access=await productAccess(session.accessToken,application);
  if(access.state==="allowed"){
   const destination=applicationDestination(application,next);
   redirect(access.profile?.mustChangePassword?`/security?next=${encodeURIComponent(destination)}`:destination);
  }
  if(access.state!=="expired")return <WorkspaceAccessState state={access.state==="unavailable"?"unavailable":"denied"}/>;
 }
 return <SharedLogin application={application}/>;
}
