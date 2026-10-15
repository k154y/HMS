import {SessionGuard} from "@/components/SessionGuard";
import {productAccess} from "@/lib/product-access";
import { auth } from "@/lib/auth";
import { SessionProvider } from "next-auth/react";
import { redirect } from "next/navigation";
import {headers} from "next/headers";
import {loginDestination} from "@/lib/navigation";
import WorkspaceAccessState from "@/components/WorkspaceAccessState";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();
  if (!session?.accessToken) redirect(loginDestination((await headers()).get("x-workspace-path")??"/dashboard"));
  const access=await productAccess(session.accessToken,"HMS");
  if(access.state==="expired")redirect(loginDestination((await headers()).get("x-workspace-path")??"/dashboard",true));
  if(access.state!=="allowed")return <SessionProvider session={session}><SessionGuard/><WorkspaceAccessState state={access.state==="unavailable"?"unavailable":"hotel"}/></SessionProvider>;
  if(access.profile?.mustChangePassword)redirect("/security?next=%2Fdashboard");
  const user={...session.user,...access.profile} as typeof session.user & {roles?:string[]};

  return (
    <SessionProvider session={session}><SessionGuard/>
      <div className="flex h-screen bg-slate-50 overflow-hidden">
        <Sidebar role={user.role??""} />
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <TopBar
            userName={user.name ?? ""}
            userRole={user.role??""} userRoles={user.roles}
            hotelName={user.hotelName ?? ""}
            subscriptionStatus={user.subscriptionStatus ?? ""}
            trialEndDate={user.trialEndDate ?? ""}
          />
          <main className="flex-1 overflow-y-auto p-5 lg:p-8"><div className="mx-auto w-full max-w-[1440px]">
            {children}
          </div></main>
        </div>
      </div>
    </SessionProvider>
  );
}



